package com.DBSCAN.HotspotAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/db")
public class IncidentController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. Get Total Count (Should show 659,610)
    // URL: http://localhost:8080/db/count
    @GetMapping("/count")
    public String getCount() {
        String sql = "SELECT COUNT(*) FROM incident_data";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return "Total records in PostgreSQL: " + count;
    }

    // 2. Select first 10 rows
    // URL: http://localhost:8080/db/test-data
    @GetMapping("/test-data")
    public List<Map<String, Object>> getTestData() {
        String sql = "SELECT case_type, city_cn, province_cn FROM incident_data LIMIT 10";
        return jdbcTemplate.queryForList(sql);
    }

    // 3. Insert a new record (Manual Test)
    // URL: http://localhost:8080/db/insert?type=Theft&city=上海
    @GetMapping("/insert")
    public String insertData(@RequestParam String type, @RequestParam String city) {
        String sql = "INSERT INTO incident_data (case_type, city_cn) VALUES (?, ?)";
        jdbcTemplate.update(sql, type, city);
        return "Inserted " + type + " for city " + city + " successfully!";
    }
}