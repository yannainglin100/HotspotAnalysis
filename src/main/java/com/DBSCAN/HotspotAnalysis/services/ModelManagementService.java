package com.DBSCAN.HotspotAnalysis.services;

import com.DBSCAN.HotspotAnalysis.models.CityRiskDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ModelManagementService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getFilteredModels(String provinceId, String year) {
        String sql = "SELECT model_id, model_name, epsilon, minpts, dbi, \n" +
                "       TO_CHAR(trained_date_time, 'YYYY-MM-DD HH24:MI:SS') as trained_date_time, \n" +
                "       select_this \n" +
                "FROM model WHERE provinces = ? AND years = ? ORDER BY trained_date_time DESC";
        return jdbcTemplate.queryForList(sql, provinceId, year);
    }

    public List<String> getDistinctYears() {
        return jdbcTemplate.queryForList("SELECT DISTINCT years FROM model WHERE years != '0' ORDER BY years DESC", String.class);
    }

    @Transactional
    public void setModelAsActive(Integer modelId) {
        // Enforce only one "select_this" model at a time
        jdbcTemplate.update("UPDATE model SET select_this = false");
        jdbcTemplate.update("UPDATE model SET select_this = true WHERE model_id = ?", modelId);
    }

    public void updateModelName(Integer id, String newName) {
        jdbcTemplate.update("UPDATE model SET model_name = ? WHERE model_id = ?", newName, id);
    }

    public List<CityRiskDTO> getDetailedRisks(Integer modelId, String provinceFilter) {
        // Use LEFT JOIN to support cities without provinces (Beijing, Tianjin, etc.)
        StringBuilder sql = new StringBuilder(
                "SELECT p.province_en, c.city_en, cr.score, cr.risk_level, coords.lat, coords.lng " +
                        "FROM city c " +
                        "LEFT JOIN province p ON c.province_id = p.province_id " +
                        "LEFT JOIN city_risk cr ON c.city_id = cr.city_id AND cr.model_id = ? " +
                        "LEFT JOIN LATERAL (" +
                        "  SELECT AVG(latitude) as lat, AVG(longitude) as lng " +
                        "  FROM (SELECT latitude, longitude FROM incident_data i WHERE i.city_id = c.city_id LIMIT 3) sub" +
                        ") coords ON true "
        );

        List<Object> params = new ArrayList<>();
        params.add(modelId);
        if (!"0".equals(provinceFilter)) {
            sql.append(" WHERE c.province_id = ?");
            params.add(Integer.parseInt(provinceFilter));
        }

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Double scoreRaw = rs.getObject("score") != null ? rs.getDouble("score") : null;
            String pName = rs.getString("province_en");
            return new CityRiskDTO(
                    pName == null ? "-" : pName,
                    rs.getString("city_en"),
                    scoreRaw == null ? "-" : String.format("%.5f", scoreRaw),
                    scoreRaw == null ? -1.0 : scoreRaw,
                    scoreRaw == null ? "Safe" : rs.getString("risk_level"),
                    rs.getDouble("lat"), rs.getDouble("lng")
            );
        });
    }

    @Transactional
    public void deleteModel(Integer modelId) {
        jdbcTemplate.update("DELETE FROM city_risk WHERE model_id = ?", modelId);
        jdbcTemplate.update("DELETE FROM model WHERE model_id = ?", modelId);
    }
}