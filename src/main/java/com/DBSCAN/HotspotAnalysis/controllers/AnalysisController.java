package com.DBSCAN.HotspotAnalysis.controllers;

import com.DBSCAN.HotspotAnalysis.repositories.*;
import com.DBSCAN.HotspotAnalysis.services.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/analysis")
@SessionAttributes("analysisResults")
public class AnalysisController {

    @Autowired private ProvinceRepository provinceRepo;
    @Autowired private IncidentDataRepository incidentRepo;
    @Autowired private AnalysisService analysisService;

    @GetMapping
    public String showForm(Model model) {
        populateModel(model);
        return "admin/analysis";
    }

    @PostMapping("/run")
    public String run(@RequestParam double epsilon, @RequestParam int minPts,
                      @RequestParam Integer provinceId, @RequestParam Integer year, Model model) {

        Map<String, Object> results = analysisService.executeClustering(epsilon, minPts, provinceId, year);

        // CONVERT TO JSON STRING MANUALLY TO PREVENT THYMELEAF ERRORS
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Convert the points list to a clean JSON string
            String json = mapper.writeValueAsString(results.get("tempPoints"));
            model.addAttribute("pointsJson", json);
        } catch (Exception e) {
            model.addAttribute("pointsJson", "[]");
        }

        model.addAttribute("results", results);
        model.addAttribute("analysisResults", results);
        model.addAttribute("eps", epsilon);
        model.addAttribute("min", minPts);
        model.addAttribute("selP", provinceId);
        model.addAttribute("selY", year);
        populateModel(model);
        return "admin/analysis";
    }

    @PostMapping("/save")
    public String saveAnalysis(
            @RequestParam String modelName,
            @RequestParam double epsilon,
            @RequestParam int minPts,
            @RequestParam int provinceId,
            @RequestParam int year,
            @SessionAttribute("analysisResults") Map<String, Object> results,
            RedirectAttributes redirectAttributes) {

        analysisService.saveAnalysis(modelName, epsilon, minPts, provinceId, year, results);

        // This will NOT show in the URL and disappears after one refresh
        redirectAttributes.addFlashAttribute("saveSuccess", "Model '" + modelName + "' saved successfully!");

        return "redirect:/admin/analysis";
    }

    private void populateModel(Model model) {
        model.addAttribute("provinces", provinceRepo.findAllOrderedById());
        List<Object> yearObjs = incidentRepo.findAllYears();
        List<Integer> cleanYears = yearObjs.stream()
                .filter(Objects::nonNull)
                .map(o -> ((Number) o).intValue())
                .collect(Collectors.toList());
        model.addAttribute("years", cleanYears);
    }
}