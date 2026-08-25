package com.medical.insurance.institution;

import com.medical.insurance.TestAdminSession;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class InstitutionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void institutionCrudUsesRequirementFieldsAndKeepsOriginalTableCompatible() throws Exception {
        MockHttpSession session = TestAdminSession.create(mockMvc,jdbcTemplate);

        String createJson = "{"
            + "\"institutionId\":\"TEST-HOSPITAL-001\","
            + "\"institutionName\":\"测试定点医院\","
            + "\"hospitalLevel\":\"三级\","
            + "\"institutionType\":\"综合医院\","
            + "\"postcode\":\"100000\","
            + "\"legalRepresentativeName\":\"测试法人\","
            + "\"legalRepresentativeMobile\":\"13800000000\","
            + "\"contactName\":\"测试联系人\","
            + "\"contactPhone\":\"010-12345678\","
            + "\"contactMobile\":\"13900000000\","
            + "\"address\":\"测试地址\","
            + "\"notes\":\"回归测试\"}";

        mockMvc.perform(post("/api/institutions").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(createJson))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/institutions/TEST-HOSPITAL-001").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.institutionName", is("测试定点医院")))
            .andExpect(jsonPath("$.data.contactName", is("测试联系人")))
            .andExpect(jsonPath("$.data.address", is("测试地址")));

        mockMvc.perform(put("/api/institutions/TEST-HOSPITAL-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson.replace("测试定点医院", "测试定点医院（更新）")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/institutions").session(session).param("keyword", "更新"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.items[0].institutionId", is("TEST-HOSPITAL-001")));

        mockMvc.perform(get("/api/dictionaries").session(session).param("category", "医院等级"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(5)));

        mockMvc.perform(delete("/api/institutions/TEST-HOSPITAL-001").session(session))
            .andExpect(status().isOk());
    }
}

