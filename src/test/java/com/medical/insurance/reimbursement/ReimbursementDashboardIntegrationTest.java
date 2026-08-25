package com.medical.insurance.reimbursement;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ReimbursementDashboardIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void dashboardSummarySupportsSelectedYearAndReimbursementRole() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reimbursement\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/reimbursements/dashboard/summary?year=2025").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.year").value(2025))
            .andExpect(jsonPath("$.data.peopleCount", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.data.companyCount", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.data.pendingVisitCount", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.data.settledVisitCount", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.data.fundPaid").isNumber())
            .andExpect(jsonPath("$.data.companyTypeDistribution").isArray());
    }
}

