package com.DBSCAN.HotspotAnalysis.controllers;

import com.DBSCAN.HotspotAnalysis.models.City;
import com.DBSCAN.HotspotAnalysis.repositories.CityRepository;
import com.DBSCAN.HotspotAnalysis.repositories.ProvinceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/cities")
public class CityController {

    @Autowired private CityRepository cityRepository;
    @Autowired private ProvinceRepository provinceRepository;

    @GetMapping
    public String listCities(@RequestParam(value = "provinceId", required = false) Integer provinceId, Model model) {
        List<City> cities;

        if (provinceId == null || provinceId == 0) {
            cities = cityRepository.findByProvinceIsNull();
            provinceId = 0; // Represents "No Province"
        } else {
            cities = cityRepository.findByProvinceId(provinceId);
        }

        model.addAttribute("cities", cities);
        model.addAttribute("provinces", provinceRepository.findAllOrderedById());
        model.addAttribute("selectedProvinceId", provinceId);
        model.addAttribute("newCity", new City());
        return "admin/cities";
    }

    @PostMapping("/save")
    public String saveCity(@ModelAttribute City city, @RequestParam("selectedProvinceId") Integer selectedProvinceId) {

        if (city.getProvince() != null) {
            Integer pId = city.getProvince().getProvince_id();
            if (pId == null || pId == 0) {
                city.setProvince(null);
            }
        }

        cityRepository.save(city);
        return "redirect:/admin/cities?provinceId=" + selectedProvinceId;
    }

    @GetMapping("/delete/{id}")
    public String deleteCity(@PathVariable Integer id, @RequestParam("provinceId") Integer provinceId) {
        cityRepository.deleteById(id);
        return "redirect:/admin/cities?provinceId=" + provinceId;
    }
}