package com.DBSCAN.HotspotAnalysis;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String showHomePage() {
        // This tells Spring to look for the static file directly
        return "redirect:/index.html";
    }

    @GetMapping("/map")
    public String showMapPage() {
        // This tells Spring to look for the static file directly
        return "redirect:/map.html";
    }
}