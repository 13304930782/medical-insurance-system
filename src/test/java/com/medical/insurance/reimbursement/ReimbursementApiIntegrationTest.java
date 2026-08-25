package com.medical.insurance.reimbursement;

import com.medical.insurance.TestAdminSession;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
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
class ReimbursementApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    private MockHttpSession session;

    @BeforeEach
    void prepare() throws Exception {
        jdbcTemplate.update("INSERT INTO people (people_id,name,medical_personnel,social_security_id,medins_id) VALUES ('RB-PERSON','报销测试人员','在职','RB-CARD','RB-HOSP')");
        jdbcTemplate.update("INSERT INTO t_medical_insititution (dia_id,dia_name,dia_starttime,dia_endtime,dia_valid) VALUES ('RB-HOSP','报销测试医院','2026-01-01','2026-12-31','有效')");
        jdbcTemplate.update("INSERT INTO ext_medical_institution_profile (institution_id,institution_name,hospital_level) VALUES ('RB-HOSP','报销测试医院','一级医院')");
        jdbcTemplate.update("INSERT INTO t_disease_ (disease_id,disease_name,disease_reimbursement_standards) VALUES ('RB-DIS','可报销测试病种','可报销')");
        jdbcTemplate.update("INSERT INTO t_medicine (med_id,med_name,med_exp_type,med_exp_level,med_max_prize) VALUES ('RB-MED','报销测试药品','药品费','甲类',100)");
        session=TestAdminSession.create(mockMvc,jdbcTemplate);
    }

    @Test
    void visitAndPrescriptionUseOriginalTablesAndServerCalculatesAmount() throws Exception {
        String visit="{\"personId\":\"RB-PERSON\",\"hospitalizationNumber\":\"RB-VISIT\",\"designatedNumber\":\"RB-HOSP\",\"medicalCategory\":\"普通住院\",\"admissionDate\":\"2026-08-01T08:00:00\",\"dischargeDate\":\"2026-08-05T10:00:00\",\"diseaseCode\":\"RB-DIS\",\"hospitalGrade\":\"一级医院\"}";
        String wrongGradeVisit=visit.replace("RB-VISIT","RB-VISIT-WRONG").replace("一级医院","三级医院");
        mockMvc.perform(post("/api/reimbursements/visits").session(session).contentType(MediaType.APPLICATION_JSON).content(wrongGradeVisit))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message",is("医院等级与所选定点医疗机构不一致，应为一级医院")));
        mockMvc.perform(post("/api/reimbursements/visits").session(session).contentType(MediaType.APPLICATION_JSON).content(visit)).andExpect(status().isCreated());
        String catalog="{\"chargeableItemsCategory\":\"药品费\",\"projectCoding\":\"RB-MED\",\"projectName\":\"前端错误名称\",\"unitPrice\":12.34,\"quantity\":3,\"amount\":999,\"sourceType\":\"CATALOG\",\"catalogType\":\"MEDICINE\"}";
        String wrongCategory=catalog.replace("药品费","其他费");
        mockMvc.perform(post("/api/reimbursements/visits/RB-VISIT/prescriptions").session(session).contentType(MediaType.APPLICATION_JSON).content(wrongCategory))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message",is("收费项目类别与所选目录项目不一致，应为药品费")));
        mockMvc.perform(post("/api/reimbursements/visits/RB-VISIT/prescriptions").session(session).contentType(MediaType.APPLICATION_JSON).content(catalog))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.amount",is(37.02)));
        assertEquals("报销测试药品",jdbcTemplate.queryForObject("SELECT project_name FROM t_prescription_details WHERE hospitalization_number='RB-VISIT' AND project_coding='RB-MED'",String.class));
        assertEquals(new BigDecimal("37.02"),jdbcTemplate.queryForObject("SELECT amount FROM t_prescription_details WHERE hospitalization_number='RB-VISIT' AND project_coding='RB-MED'",BigDecimal.class));

        String manual="{\"chargeableItemsCategory\":\"其他费\",\"projectName\":\"目录外项目\",\"unitPrice\":20,\"quantity\":2,\"sourceType\":\"MANUAL\",\"catalogType\":\"MANUAL\"}";
        mockMvc.perform(post("/api/reimbursements/visits/RB-VISIT/prescriptions").session(session).contentType(MediaType.APPLICATION_JSON).content(manual))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.projectCoding").isNotEmpty());
        mockMvc.perform(get("/api/reimbursements/visits/RB-VISIT").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.prescriptions",hasSize(2))).andExpect(jsonPath("$.data.settlementFlag",is("未结算")));
        assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ext_prescription_item WHERE hospitalization_number='RB-VISIT' AND source_type='MANUAL'",Integer.class));
    }

    @Test
    void outpatientNumberIsGeneratedAndUnsettledVisitCanBeReused() throws Exception {
        String visit="{\"personId\":\"RB-PERSON\",\"designatedNumber\":\"RB-HOSP\",\"medicalCategory\":\"普通门诊\",\"admissionDate\":\"2026-08-01T08:00:00\",\"diseaseCode\":\"RB-DIS\",\"hospitalGrade\":\"一级医院\"}";
        mockMvc.perform(post("/api/reimbursements/visits").session(session).contentType(MediaType.APPLICATION_JSON).content(visit))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.hospitalizationNumber",matchesPattern("^MZ\\d{8}$")));

        mockMvc.perform(get("/api/reimbursements/visits/outpatient-candidate?personId=RB-PERSON").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hasCandidate",is(true)))
            .andExpect(jsonPath("$.data.candidate.hospitalizationNumber",matchesPattern("^MZ\\d{8}$")))
            .andExpect(jsonPath("$.data.candidate.settlementFlag",is("未结算")));
    }
}

