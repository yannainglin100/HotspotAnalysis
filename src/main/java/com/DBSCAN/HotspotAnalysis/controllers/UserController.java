package com.DBSCAN.HotspotAnalysis.controllers;

import com.DBSCAN.HotspotAnalysis.models.City;
import com.DBSCAN.HotspotAnalysis.repositories.CityRepository;
import com.DBSCAN.HotspotAnalysis.repositories.ProvinceRepository;
import com.DBSCAN.HotspotAnalysis.services.ModelManagementService; // Corrected Name
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserController {

    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ModelManagementService modelManagementService; // Corrected Name

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("provinces", provinceRepository.findAllOrderedById());
        return "user_index";
    }

    @GetMapping("/api/cities")
    @ResponseBody
    public List<City> getCities(@RequestParam(required = false) Integer provinceId) {
        if (provinceId == null || provinceId == 0) {
            return cityRepository.findByProvinceIsNull();
        }
        return cityRepository.findByProvinceId(provinceId);
    }

    @PostMapping("/api/check-risk")
    @ResponseBody
    public Map<String, Object> checkRisk(@RequestParam Integer cityId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 1. Get the exact City Name from DB using ID to avoid duplicates
            String cityName = jdbcTemplate.queryForObject(
                    "SELECT city_en FROM city WHERE city_id = ?", String.class, cityId);

            // 2. Find the active model
            Integer activeModelId = jdbcTemplate.queryForObject(
                    "SELECT model_id FROM model WHERE select_this = true LIMIT 1", Integer.class);

            // 3. Get risks and filter by the verified city name
            return modelManagementService.getDetailedRisks(activeModelId, "0").stream()
                    .filter(r -> r.getCityName().equalsIgnoreCase(cityName))
                    .findFirst()
                    .map(dto -> {
                        response.put("status", "success");
                        response.put("risk", dto.getRiskLevel());
                        response.put("score", dto.getScore());
                        response.put("cityName", dto.getCityName()); // Return the name for the UI
                        return response;
                    })
                    .orElseGet(() -> {
                        response.put("status", "error");
                        response.put("message", "No analysis data found for this location.");
                        return response;
                    });

        } catch (EmptyResultDataAccessException e) {
            response.put("status", "error");
            response.put("message", "System busy: No active model or city not found.");
            return response;
        }
    }
}