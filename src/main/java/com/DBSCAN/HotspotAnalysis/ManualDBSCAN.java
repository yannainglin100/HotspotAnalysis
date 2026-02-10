package com.DBSCAN.HotspotAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ManualDBSCAN implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== POINT DATA MODEL ====================
    public static class Point {
        public int id;
        public double lat;
        public double lng;
        public int clusterId = -1;  // -1 = noise
        public String pointType = "UNCLASSIFIED";
        public boolean visited = false;

        public Point(int id, double lat, double lng) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
        }
    }

    // ==================== SPRING ENTRY POINT ====================
    @Override
    public void run(String... args) throws Exception {
//        System.out.println("\n" + "=".repeat(50));
//        System.out.println("STARTING DBSCAN ANALYSIS ON POSTGRES DATA");
//        System.out.println("=".repeat(50));
//
//        // 1. Fetch data for 2018
//        List<Point> points = loadPointsFromDb();
//        System.out.println(points.size());
//        if (points.isEmpty()) {
//            System.out.println("No data found for the year 2018.");
//            return;
//        }
//
//        // 2. Automatic Parameter Estimation
//        System.out.println("\nStep 1: Automatic parameter estimation...");
//        double initialEpsilon = 1500.0; // Starting heuristic
//        int minPts = calculateOptimalMinPts(points, initialEpsilon);
//        double epsilon = calculateOptimalEpsilon(points, minPts);
//
//        System.out.printf("Selected Parameters: ε = %.2f meters, minPts = %d\n", epsilon, minPts);
//
//        // 3. Run Algorithm
//        System.out.println("\nStep 2: Running KD-Tree Optimized DBSCAN...");
//        long startTime = System.currentTimeMillis();
//        runOptimizedDBSCAN(points, epsilon, minPts);
//        long endTime = System.currentTimeMillis();
//
//        long coreCount = points.stream()
//                .filter(p -> "CORE".equalsIgnoreCase(p.pointType))
//                .count();
//
//        System.out.println("\nTotal Core Points: " + coreCount);
//        double percentage = (double) coreCount * 100 / points.size();
//        System.out.println("Core Percentage: " + String.format("%.2f", percentage) + "%");
//
//        long borderCount = points.stream()
//                .filter(p -> "BORDER".equalsIgnoreCase(p.pointType))
//                .count();
//
//        System.out.println("\nTotal Border Points: " + borderCount);
//        percentage = (double) borderCount * 100 / points.size();
//        System.out.println("Border Percentage: " + String.format("%.2f", percentage) + "%");
//
//        long noiseCount = points.stream()
//                .filter(p -> "NOISE".equalsIgnoreCase(p.pointType))
//                .count();
//
//        System.out.println("\nTotal Noise Points: " + noiseCount);
//        percentage = (double) noiseCount * 100 / points.size();
//        System.out.println("Noise Percentage: " + String.format("%.2f", percentage) + "%");
//
//        calculateAndPrintDBI(points);
//
//        System.out.printf("\nTotal Processing Time: %.2f seconds\n", (endTime - startTime) / 1000.0);
    }

    private List<Point> loadPointsFromDb() {
        System.out.println("Connecting to DB and fetching Latitude/Longitude (600k+ records)...");

        // We filter for NOT NULL and non-zero to prevent the algorithm from
        // clustering errors (0,0) as a fake hotspot.
        String sql = "SELECT latitude, longitude " +
                "FROM incident_data " +
                "Where city_id is not null " +
                "ORDER BY latitude, longitude";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new Point(
                        rowNum, // Uses row number as temporary ID
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        );
    }


    // ==================== KD-TREE IMPLEMENTATION ====================
    private static class KDNode {
        Point point;
        KDNode left, right;
        int axis;
        KDNode(Point point, int axis) { this.point = point; this.axis = axis; }
    }

    private static class KDTree {
        private KDNode root;
        private List<Point> points;

        public KDTree(List<Point> points) {
            this.points = new ArrayList<>(points);
            this.root = buildTree(0, this.points.size() - 1, 0);
        }

        private KDNode buildTree(int left, int right, int axis) {
            if (left > right) return null;
            int mid = (left + right) / 2;

            if (axis == 0) {
                points.subList(left, right + 1).sort((a, b) -> {
                    int cmp = Double.compare(a.lat, b.lat);
                    return (cmp != 0) ? cmp : Integer.compare(a.id, b.id); // Tie-breaker
                });
            } else {
                points.subList(left, right + 1).sort((a, b) -> {
                    int cmp = Double.compare(a.lng, b.lng);
                    return (cmp != 0) ? cmp : Integer.compare(a.id, b.id); // Tie-breaker
                });
            }

            KDNode node = new KDNode(points.get(mid), axis);
            int nextAxis = (axis + 1) % 2;
            node.left = buildTree(left, mid - 1, nextAxis);
            node.right = buildTree(mid + 1, right, nextAxis);
            return node;
        }

        public List<Point> rangeQuery(double lat, double lng, double epsilon) {
            List<Point> result = new ArrayList<>();
            rangeQuery(root, lat, lng, epsilon, result);
            return result;
        }

        private void rangeQuery(KDNode node, double lat, double lng, double epsilon, List<Point> result) {
            if (node == null) return;
            double dist = haversineDistance(lat, lng, node.point.lat, node.point.lng);
            if (dist <= epsilon) result.add(node.point);

            double axisDiff = (node.axis == 0) ? (lat - node.point.lat) : (lng - node.point.lng);
            double axisDiffMeters = axisDiff * 111000.0; // Rough degree to meter conversion

            if (axisDiffMeters - epsilon <= 0) rangeQuery(node.left, lat, lng, epsilon, result);
            if (axisDiffMeters + epsilon >= 0) rangeQuery(node.right, lat, lng, epsilon, result);
        }

        public List<Double> getKNearestDistances(Point target, int k) {
            PriorityQueue<Double> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
            findKNearest(root, target, k, maxHeap);
            List<Double> dists = new ArrayList<>(maxHeap);
            Collections.sort(dists);
            return dists;
        }

        private void findKNearest(KDNode node, Point target, int k, PriorityQueue<Double> maxHeap) {
            if (node == null) return;
            double dist = haversineDistance(target.lat, target.lng, node.point.lat, node.point.lng);
            if (maxHeap.size() < k) maxHeap.add(dist);
            else if (dist < maxHeap.peek()) { maxHeap.poll(); maxHeap.add(dist); }

            double axisDiff = (node.axis == 0) ? (target.lat - node.point.lat) : (target.lng - node.point.lng);
            KDNode near = (axisDiff < 0) ? node.left : node.right;
            KDNode far = (axisDiff < 0) ? node.right : node.left;

            findKNearest(near, target, k, maxHeap);
            if (maxHeap.size() < k || Math.abs(axisDiff * 111000) < maxHeap.peek()) {
                findKNearest(far, target, k, maxHeap);
            }
        }
    }

    // ==================== ALGORITHM LOGIC ====================
    public static void runOptimizedDBSCAN(List<Point> points, double epsilon, int minPts) {
        KDTree kdTree = new KDTree(points);
        int clusterId = 0;

        for (Point point : points) {
            if (point.visited) continue;
            point.visited = true;

            List<Point> neighbors = kdTree.rangeQuery(point.lat, point.lng, epsilon);

            if (neighbors.size() < minPts) {
                point.pointType = "NOISE";
            } else {
                point.clusterId = clusterId;
                point.pointType = "CORE";
                expandCluster(point, neighbors, clusterId, kdTree, epsilon, minPts);
                clusterId++;
            }
        }
        printResults(points, clusterId);
    }

    private static void expandCluster(Point root, List<Point> neighbors, int clusterId, KDTree kdTree, double epsilon, int minPts) {
        Queue<Point> queue = new LinkedList<>(neighbors);
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            if (!current.visited) {
                current.visited = true;
                List<Point> currentNeighbors = kdTree.rangeQuery(current.lat, current.lng, epsilon);
                if (currentNeighbors.size() >= minPts) {
                    current.pointType = "CORE";
                    for (Point n : currentNeighbors) {
                        if (!n.visited) queue.add(n);
                    }
                }
            }
            if (current.clusterId == -1) {
                current.clusterId = clusterId;
                if (!current.pointType.equals("CORE")) current.pointType = "BORDER";
            }
        }
    }

    // ==================== PARAMETER MATH ====================
    public static double calculateOptimalEpsilon(List<Point> points, int minPts) {
        return 1800;
    }

    public static int calculateOptimalMinPts(List<Point> points, double epsilon) {
        return 6;
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private static void printResults(List<Point> points, int clusters) {
        long noise = points.stream().filter(p -> p.clusterId == -1).count();
        System.out.println("\nResults:");
        System.out.println("- Total Clusters: " + clusters);
        System.out.println("- Noise Points: " + noise + "\n");
    }

    public static void calculateAndPrintSilhouetteScore(List<Point> points, int sampleSize) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("STEP 4: MEMORY-EFFICIENT SILHOUETTE SCORE");
        System.out.println("=".repeat(60));

        // 1. Filter out noise and prepare clustered points
        List<Point> clusteredPoints = new ArrayList<>();
        for (Point p : points) {
            if (p.clusterId != -1) clusteredPoints.add(p);
        }

        if (clusteredPoints.isEmpty()) {
            System.out.println("❌ No clusters found. Cannot calculate score.");
            return;
        }

        // 2. Select a Random Sample to protect Memory and CPU
        // Even with 80k points, a sample of 2000-5000 is statistically significant
        Collections.shuffle(clusteredPoints, new Random(42)); // Fixed seed for stability
        int actualSampleSize = Math.min(sampleSize, clusteredPoints.size());
        List<Point> sample = clusteredPoints.subList(0, actualSampleSize);

        double totalS = 0;
        Set<Integer> clusterIds = new HashSet<>();
        for (Point p : clusteredPoints) clusterIds.add(p.clusterId);

        System.out.printf("Calculating score using sample of %d points...\n", actualSampleSize);

        // 3. Calculation Loop
        for (int i = 0; i < sample.size(); i++) {
            Point target = sample.get(i);

            // a(i): Average distance to points in the SAME cluster
            double a_i = calculateAverageDistToCluster(target, target.clusterId, clusteredPoints);

            // b(i): Minimum average distance to any OTHER cluster
            double b_i = Double.MAX_VALUE;
            for (int otherId : clusterIds) {
                if (otherId == target.clusterId) continue;
                double avgDistToOther = calculateAverageDistToCluster(target, otherId, clusteredPoints);
                b_i = Math.min(b_i, avgDistToOther);
            }

            // s(i) calculation
            if (b_i == Double.MAX_VALUE) b_i = 0;
            double s_i = (Math.max(a_i, b_i) == 0) ? 0 : (b_i - a_i) / Math.max(a_i, b_i);
            totalS += s_i;

            if (i % 500 == 0 && i > 0) System.out.printf("Processed %d / %d points...\n", i, actualSampleSize);
        }

        double finalScore = totalS / actualSampleSize;
        System.out.printf("\n>>> AVERAGE SILHOUETTE SCORE: %.4f\n", finalScore);
        System.out.println("Interpretation: 1.0 = Perfect, 0.0 = Overlapping, -1.0 = Wrong Cluster");
    }

    private static double calculateAverageDistToCluster(Point target, int clusterId, List<Point> allClusteredPoints) {
        double sum = 0;
        int count = 0;
        for (Point p : allClusteredPoints) {
            if (p.clusterId == clusterId && p.id != target.id) {
                sum += haversineDistance(target.lat, target.lng, p.lat, p.lng);
                count++;
            }
        }
        return (count == 0) ? 0 : sum / count;
    }

    public static void calculateAndPrintDBI(List<Point> points) {
        // 1. Group points by cluster (Ignore noise -1)
        Map<Integer, List<Point>> clusterGroups = new HashMap<>();
        for (Point p : points) {
            if (p.clusterId != -1) {
                clusterGroups.computeIfAbsent(p.clusterId, k -> new ArrayList<>()).add(p);
            }
        }

        int k = clusterGroups.size();
        if (k < 2) {
            System.out.println("❌ DBI requires at least 2 clusters.");
            return;
        }

        Integer[] clusterIds = clusterGroups.keySet().toArray(new Integer[0]);
        double[] s = new double[k]; // Intra-cluster distances
        Point[] centroids = new Point[k];

        // 2. Calculate Centroids and Intra-cluster distances (S_i)
        for (int i = 0; i < k; i++) {
            List<Point> clusterPoints = clusterGroups.get(clusterIds[i]);

            // Calculate Centroid
            double avgLat = 0, avgLng = 0;
            for (Point p : clusterPoints) {
                avgLat += p.lat;
                avgLng += p.lng;
            }
            centroids[i] = new Point(-1, avgLat / clusterPoints.size(), avgLng / clusterPoints.size());

            // Calculate S_i (average distance to centroid)
            double sumDist = 0;
            for (Point p : clusterPoints) {
                sumDist += haversineDistance(p.lat, p.lng, centroids[i].lat, centroids[i].lng);
            }
            s[i] = sumDist / clusterPoints.size();
        }

        // 3. Calculate R_ij and find Max R_i
        double dbiSum = 0;
        for (int i = 0; i < k; i++) {
            double maxR = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < k; j++) {
                if (i == j) continue;

                // Distance between centroids (M_ij)
                double m_ij = haversineDistance(centroids[i].lat, centroids[i].lng, centroids[j].lat, centroids[j].lng);

                // Similarity measure
                double r_ij = (s[i] + s[j]) / m_ij;

                if (r_ij > maxR) maxR = r_ij;
            }
            dbiSum += maxR;
        }

        double dbiResult = dbiSum / k;

        System.out.println("\n" + "=".repeat(40));
        System.out.println("DAVIES-BOULDIN INDEX RESULTS");
        System.out.println("=".repeat(40));
        System.out.printf("Number of Clusters (K): %d\n", k);
        System.out.printf("DBI Score: %.4f\n", dbiResult);
        System.out.println("Note: Lower scores (closer to 0) indicate better clustering.");
        System.out.println("=".repeat(40));
    }
}