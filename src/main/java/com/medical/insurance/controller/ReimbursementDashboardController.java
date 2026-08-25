package com.medical.insurance.controller;

import com.medical.insurance.service.impl.ReimbursementDashboardService;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reimbursements/dashboard")
public class ReimbursementDashboardController {
    private final ReimbursementDashboardService service;

    ReimbursementDashboardController(ReimbursementDashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    Map<String, Object> summary(@RequestParam(required = false) Integer year) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", service.summary(year));
        return response;
    }
}
