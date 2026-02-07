package com.DBSCAN.HotspotAnalysis.controllers;

import com.DBSCAN.HotspotAnalysis.models.CaseType;
import com.DBSCAN.HotspotAnalysis.repositories.CaseTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/case-types")
public class CaseTypeController {

    @Autowired
    private CaseTypeRepository caseTypeRepository;

    @GetMapping
    public String listCaseTypes(Model model) {
        List<CaseType> caseTypes = caseTypeRepository.findAllOrderedById();
        model.addAttribute("caseTypes", caseTypes);
        model.addAttribute("newCaseType", new CaseType());
        return "admin/case_types";
    }

    @PostMapping("/save")
    public String saveCaseType(@ModelAttribute("caseType") CaseType caseType) {
        caseTypeRepository.save(caseType);
        return "redirect:/admin/case-types";
    }

    @GetMapping("/delete/{id}")
    public String deleteCaseType(@PathVariable Integer id) {
        caseTypeRepository.deleteById(id);
        return "redirect:/admin/case-types";
    }
}