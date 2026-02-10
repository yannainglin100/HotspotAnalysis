package com.DBSCAN.HotspotAnalysis.controllers;

import com.DBSCAN.HotspotAnalysis.models.*;
import com.DBSCAN.HotspotAnalysis.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin/incidents")
public class IncidentController {

    @Autowired private IncidentDataRepository incidentRepo;
    @Autowired private ProvinceRepository provinceRepo;
    @Autowired private CityRepository cityRepo;
    @Autowired private CaseTypeRepository caseTypeRepo;

    @GetMapping
    public String list(@RequestParam(required = false) Integer provinceId,
                       @RequestParam(required = false) Integer cityId,
                       @RequestParam(required = false) Integer caseTypeId,
                       @RequestParam(required = false) Integer year,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        List<Province> provinces = provinceRepo.findAllOrderedById();
        List<CaseType> caseTypes = caseTypeRepo.findAllOrderedById();
        List<Object> yearObjects = incidentRepo.findAllYears();

        List<Integer> years = yearObjects.stream()
                .filter(java.util.Objects::nonNull)
                .map(obj -> ((Number) obj).intValue())
                .collect(Collectors.toList());

        if (year == null) year = years.isEmpty() ? 2026 : years.get(0);
        if (provinceId == null) provinceId = 0;

        List<City> citiesForFilter = (provinceId == 0) ? cityRepo.findByProvinceIsNull() : cityRepo.findByProvinceId(provinceId);
        if (cityId == null && !citiesForFilter.isEmpty()) cityId = citiesForFilter.get(0).getCity_id();
        if (caseTypeId == null && !caseTypes.isEmpty()) caseTypeId = caseTypes.get(0).getCase_type_id();

        Page<IncidentData> incidentPage = incidentRepo.findFilteredPage(provinceId, cityId, caseTypeId, year, PageRequest.of(page, 100));

        model.addAttribute("incidents", incidentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", incidentPage.getTotalPages());

        // Logic for page numbers 1, 2, 3...
        if (incidentPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(0, incidentPage.getTotalPages() - 1)
                    .boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        model.addAttribute("provinces", provinces);
        model.addAttribute("cities", citiesForFilter);
        model.addAttribute("caseTypes", caseTypes);
        model.addAttribute("years", years);
        model.addAttribute("selP", provinceId);
        model.addAttribute("selC", cityId);
        model.addAttribute("selCT", caseTypeId);
        model.addAttribute("selY", year);

        return "admin/incidents";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute IncidentData incident, @RequestParam Integer selP, @RequestParam Integer selC, @RequestParam Integer selCT, @RequestParam Integer selY) {
        incidentRepo.save(incident);
        return String.format("redirect:/admin/incidents?provinceId=%d&cityId=%d&caseTypeId=%d&year=%d", selP, selC, selCT, selY);
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long id, @RequestParam Integer p, @RequestParam Integer c, @RequestParam Integer ct, @RequestParam Integer y) {
        incidentRepo.deleteById(id);
        return String.format("redirect:/admin/incidents?provinceId=%d&cityId=%d&caseTypeId=%d&year=%d", p, c, ct, y);
    }

    @GetMapping("/cities-by-province")
    @ResponseBody
    public List<City> getCities(@RequestParam Integer provinceId) {
        return (provinceId == 0) ? cityRepo.findByProvinceIsNull() : cityRepo.findByProvinceId(provinceId);
    }
}