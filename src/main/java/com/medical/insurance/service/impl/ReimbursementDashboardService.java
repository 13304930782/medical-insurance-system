package com.medical.insurance.service.impl;

import com.medical.insurance.dao.ReimbursementDashboardMapper;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ReimbursementDashboardService {
    private final ReimbursementDashboardMapper mapper;

    public ReimbursementDashboardService(ReimbursementDashboardMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> summary(Integer requestedYear) {
        int year = requestedYear == null || requestedYear < 2000 || requestedYear > 2100
            ? Year.now().getValue() : requestedYear;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("peopleCount", mapper.peopleCount());
        result.put("companyCount", mapper.companyCount());
        result.put("pendingVisitCount", mapper.pendingVisitCount());
        result.put("settledVisitCount", mapper.settledVisitCount(year));
        result.put("fundPaid", mapper.fundPaid(year));
        result.put("companyTypeDistribution", mapper.companyTypeDistribution());
        return result;
    }
}
