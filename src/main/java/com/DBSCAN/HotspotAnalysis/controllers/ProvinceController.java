package com.DBSCAN.HotspotAnalysis.controllers;

import com.DBSCAN.HotspotAnalysis.models.Province;
import com.DBSCAN.HotspotAnalysis.repositories.ProvinceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

import java.util.List;

@Controller
@RequestMapping("/admin/provinces")
public class ProvinceController {

    @Autowired
    private ProvinceRepository provinceRepository;

    // Display all provinces
    @GetMapping
    public String listProvinces(Model model) {
        // Call the SQL-based method we just created
        List<Province> provinces = provinceRepository.findAllOrderedById();

        model.addAttribute("provinces", provinces);
        model.addAttribute("newProvince", new Province());
        return "admin/provinces";
    }

    // Save or Update province
    @PostMapping("/save")
    public String saveProvince(@ModelAttribute("province") Province province) {
        provinceRepository.save(province);
        return "redirect:/admin/provinces";
    }

    // Delete province
    @GetMapping("/delete/{id}")
    public String deleteProvince(@PathVariable Integer id) {
        provinceRepository.deleteById(id);
        return "redirect:/admin/provinces";
    }
}