package com.medical.insurance.treatment;

import com.medical.insurance.TestAdminSession;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
class TreatmentParameterApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void threeOriginalParameterTablesSupportCrudAndRejectOverlappingSegments() throws Exception {
        jdbcTemplate.update("INSERT INTO ext_data_dictionary(source_row_no,parameter_category,parameter_value,parameter_label) VALUES (-1001,'医疗人员类别','TEST-PERSONNEL','测试人员类别'),(-1002,'医疗类别','TEST-MEDICAL','测试住院'),(-1003,'医院等级','TEST-HOSPITAL','测试一级')");
        MockHttpSession session = TestAdminSession.create(mockMvc,jdbcTemplate);

        mockMvc.perform(post("/api/treatment-parameters/capping-lines").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"medicalPersonnelCategory\":\"测试人员类别\",\"cappingLineFee\":50000}"))
            .andExpect(status().isCreated());
        mockMvc.perform(put("/api/treatment-parameters/capping-lines/{category}", "测试人员类别").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"medicalPersonnelCategory\":\"测试人员类别\",\"cappingLineFee\":60000}"))
            .andExpect(status().isOk());
        assertEquals(new BigDecimal("60000.00"), jdbcTemplate.queryForObject(
            "SELECT capping_line_fee FROM t_capping_line WHERE medical_personnel_category='测试人员类别'", BigDecimal.class));

        String minimum = "{\"medicalCategory\":\"测试住院\",\"medicalPersonnelCategory\":\"测试人员类别\",\"hospitalLevel\":\"测试一级\",\"minimumPaymentStandard\":100}";
        mockMvc.perform(post("/api/treatment-parameters/minimum-payment-standards").session(session).contentType(MediaType.APPLICATION_JSON).content(minimum))
            .andExpect(status().isCreated());

        String segment = "{\"medicalCategory\":\"测试住院\",\"medicalPersonnelCategory\":\"测试人员类别\",\"hospitalLevel\":\"测试一级\",\"minimumAmount\":100,\"maximumAmount\":10000,\"reimbursementProportion\":0.8}";
        mockMvc.perform(post("/api/treatment-parameters/segment-ratios").session(session).contentType(MediaType.APPLICATION_JSON).content(segment))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/treatment-parameters/segment-ratios").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(segment.replace("10000", "20000").replace("100,", "9000,")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("该金额区间与已有分段重叠")));

        mockMvc.perform(get("/api/treatment-parameters/segment-ratios").session(session).param("keyword", "测试住院"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(delete("/api/treatment-parameters/segment-ratios").session(session)
                .param("medicalCategory", "测试住院").param("medicalPersonnelCategory", "测试人员类别")
                .param("hospitalLevel", "测试一级").param("minimumAmount", "100").param("maximumAmount", "10000"))
            .andExpect(status().isOk());
        mockMvc.perform(delete("/api/treatment-parameters/minimum-payment-standards").session(session)
                .param("medicalCategory", "测试住院").param("medicalPersonnelCategory", "测试人员类别").param("hospitalLevel", "测试一级"))
            .andExpect(status().isOk());
        mockMvc.perform(delete("/api/treatment-parameters/capping-lines/{category}", "测试人员类别").session(session))
            .andExpect(status().isOk());
        assertEquals(7, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ext_operation_log WHERE operation_module='医疗待遇计算参数维护'", Integer.class));
    }
}

