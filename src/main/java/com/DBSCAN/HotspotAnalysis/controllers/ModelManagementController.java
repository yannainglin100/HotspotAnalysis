package com.DBSCAN.HotspotAnalysis.controllers;

import com.DBSCAN.HotspotAnalysis.services.ModelManagementService;
import com.DBSCAN.HotspotAnalysis.repositories.ProvinceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin/models")
public class ModelManagementController {

    @Autowired
    private ModelManagementService modelService;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate; // Fix: Required for queryForMap in viewDetail

    @GetMapping
    public String listModels(@RequestParam(defaultValue = "0") String provinceId,
                             @RequestParam(defaultValue = "0") String year,
                             Model model) {
        model.addAttribute("models", modelService.getFilteredModels(provinceId, year));
        model.addAttribute("provinces", provinceRepository.findAllOrderedById());
        model.addAttribute("years", modelService.getDistinctYears());
        model.addAttribute("selP", provinceId);
        model.addAttribute("selY", year);
        return "admin/model_list";
    }

    @GetMapping("/detail/{id}")
    public String viewDetail(@PathVariable Integer id, @RequestParam(defaultValue = "0") String provinceId, Model model) {
        // FIX: Include all metadata columns (provinces, years, noise_percentage, etc.)
        Map<String, Object> modelData = jdbcTemplate.queryForMap(
                "SELECT model_id, model_name, epsilon, minpts, dbi, " +
                        "       noise_percentage, total_clusters, points_processed, " +
                        "       provinces, years, select_this, " + // These were missing!
                        "       TO_CHAR(trained_date_time, 'YYYY-MM-DD HH24:MI:SS') as trained_date_time " +
                        "FROM model WHERE model_id = ?", id);

        // Auto-lock filter if model is province-specific
        Object modelScopeObj = modelData.get("provinces");
        String modelScope = (modelScopeObj != null) ? modelScopeObj.toString() : "0";

        if (!"0".equals(modelScope)) {
            provinceId = modelScope;
            // Fetch province name for the "Provinces: [Name]" display in metadata
            String provName = jdbcTemplate.queryForObject(
                    "SELECT province_en FROM province WHERE province_id = ?",
                    String.class,
                    Integer.parseInt(modelScope)
            );
            model.addAttribute("provName", provName);
        }

        model.addAttribute("m", modelData);
        model.addAttribute("risks", modelService.getDetailedRisks(id, provinceId));
        model.addAttribute("provinces", provinceRepository.findAllOrderedById());
        model.addAttribute("selP", provinceId);

        return "admin/model_detail";
    }

    @PostMapping("/toggle")
    @ResponseBody
    public String toggleModel(@RequestParam Integer modelId) {
        modelService.setModelAsActive(modelId);
        return "success";
    }

    @PostMapping("/delete")
    public String deleteModel(@RequestParam Integer modelId) {
        modelService.deleteModel(modelId);
        return "redirect:/admin/models";
    }

    @PostMapping("/edit-name")
    public String editModelName(@RequestParam Integer modelId, @RequestParam String modelName) {
        modelService.updateModelName(modelId, modelName);
        return "redirect:/admin/models";
    }
}