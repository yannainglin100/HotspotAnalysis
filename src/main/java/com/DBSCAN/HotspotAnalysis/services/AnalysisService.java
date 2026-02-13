package com.DBSCAN.HotspotAnalysis.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AnalysisService {

    @Autowired private JdbcTemplate jdbcTemplate;

    public static class Point {
        // Spatial & Clustering Data
        public double lat, lng;
        public int cityId;
        public long cityPopulation;
        public int caseWeight;
        public int clusterId = -1; // -1 = Noise
        public String type = "NOISE"; // CORE, BORDER, NOISE
        public boolean visited = false;

        // Metadata for UI Hover/Popups
        public String caseType;
        public String cityName;
        public String provinceName;
        public String datetime;

        public Point(double lat, double lng, int cityId, long cityPop, int caseWeight,
                     String caseType, String cityName, String provinceName, String datetime) {
            this.lat = lat; this.lng = lng; this.cityId = cityId;
            this.cityPopulation = cityPop; this.caseWeight = caseWeight;
            this.caseType = caseType; this.cityName = cityName;
            this.provinceName = provinceName; this.datetime = datetime;
        }
    }

    /**
     * Executes DBSCAN clustering and calculates performance/quality metrics.
     */
    public Map<String, Object> executeClustering(double epsilon, int minPts, Integer pId, Integer year) {
        long startTime = System.currentTimeMillis();

        // 1. Dynamic Query with necessary joins for population, weights, and metadata
        StringBuilder sql = new StringBuilder(
                "SELECT i.latitude, i.longitude, i.city_id, c.population, ct.weight, " +
                        "ct.case_type_en, c.city_en, " +
                        "COALESCE(p.province_en, 'Municipality') AS province_en, " + // Handles null provinces
                        "i.formatted_datetime " +
                        "FROM incident_data i " +
                        "JOIN city c ON i.city_id = c.city_id " +
                        "JOIN case_type ct ON i.case_type_id = ct.case_type_id " +
                        "LEFT JOIN province p ON c.province_id = p.province_id " + // Changed to LEFT JOIN
                        "WHERE i.latitude IS NOT NULL AND i.longitude IS NOT NULL " +
                        "AND i.formatted_datetime IS NOT NULL "
        );

        List<Object> params = new ArrayList<>();
        if (year != null && year != 0) {
            sql.append("AND EXTRACT(YEAR FROM i.formatted_datetime) = ? ");
            params.add(year);
        }
        if (pId != null && pId != 0) {
            sql.append("AND c.province_id = ? ");
            params.add(pId);
        }

        List<Point> points = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
                new Point(
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getInt("city_id"),
                        rs.getLong("population"),
                        rs.getInt("weight"),
                        rs.getString("case_type_en"),
                        rs.getString("city_en"),
                        rs.getString("province_en"),
                        rs.getString("formatted_datetime")
                ));

        if (points.isEmpty()) return Collections.singletonMap("error", "No records found for this criteria.");

        // 2. Build KD-Tree & Run DBSCAN
        KDTree tree = new KDTree(points);
        int clusterCount = 0;
        for (Point p : points) {
            if (p.visited) continue;
            p.visited = true;
            List<Point> neighbors = tree.rangeQuery(p.lat, p.lng, epsilon);
            if (neighbors.size() >= minPts) {
                p.type = "CORE";
                p.clusterId = clusterCount;
                expandCluster(p, neighbors, tree, clusterCount, epsilon, minPts);
                clusterCount++;
            }
        }

        // Check if any clusters were created
        if (clusterCount == 0) {
            Map<String, Object> errorResults = new HashMap<>();
            errorResults.put("error", "There are only " + points.size() + " point(s). No clusters formed! Try increasing Epsilon or decreasing MinPts.");
            return errorResults;
        }

        // 3. Metrics Calculation
        double dbi = calculateDBI(points, clusterCount);
        long noiseCount = points.stream().filter(p -> p.clusterId == -1).count();
        long endTime = System.currentTimeMillis();

        Map<String, Object> res = new HashMap<>();
        res.put("totalClusters", clusterCount);
        res.put("noisePercentage", (double) noiseCount * 100 / points.size());
        res.put("dbi", dbi);
        res.put("totalPoints", points.size());
        res.put("executionTime", (endTime - startTime) / 1000.0 + "s");
        res.put("tempPoints", points); // Passed to controller
        return res;
    }

    private void expandCluster(Point p, List<Point> neighbors, KDTree tree, int cid, double eps, int min) {
        Queue<Point> queue = new LinkedList<>(neighbors);

        while (!queue.isEmpty()) {
            Point curr = queue.poll();

            // Handle Noise-to-Border transition
            if (curr.clusterId == -1) {
                curr.clusterId = cid;
                curr.type = "BORDER";
            }

            if (!curr.visited) {
                curr.visited = true;
                List<Point> cn = tree.rangeQuery(curr.lat, curr.lng, eps);

                if (cn.size() >= min) {
                    curr.type = "CORE";
                    curr.clusterId = cid;
                    queue.addAll(cn);
                } else {
                    curr.type = "BORDER";
                }
            }
        }
    }

    @Transactional
    public void saveAnalysis(String modelName, double eps, int min, Integer pId, Integer year, Map<String, Object> results) {
        List<Point> points = (List<Point>) results.get("tempPoints");
        int clusterCount = (int) results.get("totalClusters");

        // Step A: Calculate Cluster Risk Scores (Severity x Spatial Multiplier)
        Map<Integer, Double> clusterRiskMap = new HashMap<>();
        for (Point p : points) {
            if (p.clusterId == -1) continue;
            double multiplier = p.type.equals("CORE") ? 1.5 : 1.0;
            clusterRiskMap.merge(p.clusterId, (double) p.caseWeight * multiplier, Double::sum);
        }

        // Step B: Aggregate by City
        Map<Integer, List<Integer>> cityToClusters = new HashMap<>();
        for (Point p : points) {
            if (p.clusterId != -1) {
                cityToClusters.computeIfAbsent(p.cityId, k -> new ArrayList<>()).add(p.clusterId);
            }
        }

        // Step C: Normalize by Population (Unique Cluster Sum)
        Map<Integer, Double> cityScores = new HashMap<>();
        for (Integer cityId : cityToClusters.keySet()) {
            Set<Integer> uniqueClusterIds = new HashSet<>(cityToClusters.get(cityId));
            double totalCityRisk = uniqueClusterIds.stream()
                    .mapToDouble(cId -> clusterRiskMap.getOrDefault(cId, 0.0))
                    .sum();

            long pop = points.stream()
                    .filter(p -> p.cityId == cityId)
                    .findFirst()
                    .map(p -> p.cityPopulation)
                    .orElse(1L);

            cityScores.put(cityId, totalCityRisk / (double) pop);
        }

        // Step D: Classify into categories
        List<Double> allScores = new ArrayList<>(cityScores.values());
        Collections.sort(allScores);

        // Persistent Model Record
        String sqlModel = "INSERT INTO model (model_name, provinces, years, epsilon, minpts, total_clusters, noise_percentage, dbi) VALUES (?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sqlModel, modelName, String.valueOf(pId), String.valueOf(year), eps, min, clusterCount, results.get("noisePercentage"), results.get("dbi"));
        int modelId = jdbcTemplate.queryForObject("SELECT lastval()", Integer.class);

        // Persistent City Risk Records
        for (Map.Entry<Integer, Double> entry : cityScores.entrySet()) {
            String level = getLevel(entry.getValue(), allScores);
            jdbcTemplate.update("INSERT INTO city_risk (model_id, city_id, risk_level, score) VALUES (?,?,?,?)", modelId, entry.getKey(), level, entry.getValue());
        }
    }

    private String getLevel(Double score, List<Double> scores) {
        if (scores.isEmpty()) return "Minimal Activity";
        double q1 = scores.get((int) (scores.size() * 0.25));
        double q2 = scores.get((int) (scores.size() * 0.50));
        double q3 = scores.get((int) (scores.size() * 0.75));

        if (score <= q1) return "Safe";
        if (score <= q2) return "Low Risk";
        if (score <= q3) return "Medium Risk";
        return "High Risk";
    }

    private double calculateDBI(List<Point> points, int k) {
        if (k < 2) return 0.0;
        Map<Integer, List<Point>> clusters = new HashMap<>();
        for (Point p : points) if (p.clusterId != -1) clusters.computeIfAbsent(p.clusterId, x -> new ArrayList<>()).add(p);

        double[] s = new double[k];
        Point[] centroids = new Point[k];
        for (int i = 0; i < k; i++) {
            List<Point> cPoints = clusters.get(i);
            double sumLat = 0, sumLng = 0;
            for (Point p : cPoints) { sumLat += p.lat; sumLng += p.lng; }
            centroids[i] = new Point(sumLat / cPoints.size(), sumLng / cPoints.size(), 0, 0, 0, "", "", "", "");
            double distSum = 0;
            for (Point p : cPoints) distSum += haversine(p.lat, p.lng, centroids[i].lat, centroids[i].lng);
            s[i] = distSum / cPoints.size();
        }

        double dbiSum = 0;
        for (int i = 0; i < k; i++) {
            double maxR = 0;
            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                double m_ij = haversine(centroids[i].lat, centroids[i].lng, centroids[j].lat, centroids[j].lng);
                double r_ij = (s[i] + s[j]) / (m_ij == 0 ? 1 : m_ij);
                if (r_ij > maxR) maxR = r_ij;
            }
            dbiSum += maxR;
        }
        return dbiSum / k;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private class KDTree {
        private class Node { Point p; Node left, right; int axis; Node(Point p, int a) { this.p = p; this.axis = a; } }
        private Node root;
        KDTree(List<Point> pts) { root = build(new ArrayList<>(pts), 0); }
        private Node build(List<Point> pts, int axis) {
            if (pts.isEmpty()) return null;
            pts.sort((a, b) -> axis == 0 ? Double.compare(a.lat, b.lat) : Double.compare(a.lng, b.lng));
            int mid = pts.size() / 2;
            Node node = new Node(pts.get(mid), axis);
            node.left = build(new ArrayList<>(pts.subList(0, mid)), (axis + 1) % 2);
            node.right = build(new ArrayList<>(pts.subList(mid + 1, pts.size())), (axis + 1) % 2);
            return node;
        }
        void rangeQuery(Node n, double lat, double lng, double eps, List<Point> res) {
            if (n == null) return;
            if (haversine(lat, lng, n.p.lat, n.p.lng) <= eps) res.add(n.p);
            double diff = (n.axis == 0) ? lat - n.p.lat : lng - n.p.lng;
            // 111,000 meters is roughly 1 degree of latitude
            if (diff * 111000 <= eps) rangeQuery(n.left, lat, lng, eps, res);
            if (diff * 111000 >= -eps) rangeQuery(n.right, lat, lng, eps, res);
        }
        List<Point> rangeQuery(double lat, double lng, double eps) {
            List<Point> res = new ArrayList<>();
            rangeQuery(root, lat, lng, eps, res);
            return res;
        }
    }
}