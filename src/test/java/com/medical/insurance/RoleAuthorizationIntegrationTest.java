package com.medical.insurance;

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
class RoleAuthorizationIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void approverAndReimbursementRolesAreSeparated() throws Exception {
        MockHttpSession approver=login("approver");
        mockMvc.perform(get("/api/approvals/institutions").session(approver)).andExpect(status().isOk());
        mockMvc.perform(post("/api/reimbursements/visits/ANY/preview").session(approver)).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("当前角色无权执行该操作"));
        mockMvc.perform(post("/api/medicines").session(approver).contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isForbidden());

        MockHttpSession reimbursement=login("reimbursement");
        mockMvc.perform(get("/api/reimbursements/settlements").session(reimbursement)).andExpect(status().isOk());
        mockMvc.perform(get("/api/reimbursements/dashboard/summary").session(reimbursement)).andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/knowledge/sync-status").session(reimbursement)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/ai/help").session(reimbursement)).andExpect(status().isOk());
        mockMvc.perform(post("/api/approvals/institutions").session(reimbursement).contentType(MediaType.APPLICATION_JSON).content("{}")) .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/medicines?page=1&size=1").session(reimbursement)).andExpect(status().isOk());
    }

    private MockHttpSession login(String username)throws Exception{MvcResult result=mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\""+username+"\",\"password\":\"123456\"}")).andExpect(status().isOk()).andReturn();return (MockHttpSession)result.getRequest().getSession(false);}
}

