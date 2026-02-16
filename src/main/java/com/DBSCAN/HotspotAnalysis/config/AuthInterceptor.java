package com.DBSCAN.HotspotAnalysis.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Check if the session attribute "user" exists
        if (request.getSession().getAttribute("user") == null) {
            // If not logged in, redirect to login page
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }
}