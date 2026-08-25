package com.medical.insurance.reimbursement;

import com.medical.insurance.TestAdminSession;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import tools.jackson.databind.ObjectMapper;

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
class SettlementApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;
    private MockHttpSession session;

    @BeforeEach
    void prepare() throws Exception {
        jdbcTemplate.update("INSERT INTO people (people_id,name,medical_personnel,social_security_id,medins_id) VALUES ('ST-PERSON','结算测试人员','结算测试在职','ST-CARD','ST-HOSP')");
        jdbcTemplate.update("INSERT INTO t_medical_insititution (dia_id,dia_name,dia_starttime,dia_endtime,dia_valid) VALUES ('ST-HOSP','结算测试医院','2026-01-01','2026-12-31','有效')");
        jdbcTemplate.update("INSERT INTO ext_medical_institution_profile (institution_id,institution_name,hospital_level) VALUES ('ST-HOSP','结算测试医院','一级医院')");
        jdbcTemplate.update("INSERT INTO t_disease_ (disease_id,disease_name,disease_reimbursement_standards) VALUES ('ST-DIS','结算测试病种','可报销')");
        jdbcTemplate.update("INSERT INTO t_medicine (med_id,med_name,med_exp_type,med_exp_level,med_max_prize,med_hos_level,med_valid,med_approvalmark) VALUES ('ST-A','甲类药','药品费','甲类',100,'所有医院','有效','不需要审批'),('ST-B','乙类药','药品费','乙类',100,'所有医院','有效','不需要审批'),('ST-C','丙类药','药品费','丙类',100,'所有医院','有效','不需要审批')");
        jdbcTemplate.update("INSERT INTO t_personnel_visits_info (person_ID,hospitalization_number,designated_number,medical_category,admission_date,discharge_date,disease_code,hospital_grade,settlement_flag) VALUES ('ST-PERSON','ST-VISIT','ST-HOSP','结算测试住院','2026-08-01','2026-08-05','ST-DIS','一级医院','未结算')");
        jdbcTemplate.update("INSERT INTO t_prescription_details (hospitalization_number,chargeable_items_Category,project_coding,project_name,unit_price,quantity,amount) VALUES ('ST-VISIT','药品费','ST-A','甲类药',120,1,120),('ST-VISIT','药品费','ST-B','乙类药',100,1,100),('ST-VISIT','药品费','ST-C','丙类药',100,1,100)");
        jdbcTemplate.update("INSERT INTO ext_prescription_item (hospitalization_number,chargeable_items_Category,project_coding,source_type,catalog_type) VALUES ('ST-VISIT','药品费','ST-A','CATALOG','MEDICINE'),('ST-VISIT','药品费','ST-B','CATALOG','MEDICINE'),('ST-VISIT','药品费','ST-C','CATALOG','MEDICINE')");
        jdbcTemplate.update("INSERT INTO t_capping_line (medical_personnel_category,capping_line_fee) VALUES ('结算测试在职',1000)");
        jdbcTemplate.update("INSERT INTO t_minimum_payment_standard (medical_category,medical_personnel_category,hospital_level,minimum_payment_standard) VALUES ('结算测试住院','结算测试在职','一级医院',10)");
        jdbcTemplate.update("INSERT INTO t_individual_segement_self_funded_ratio (medical_category,medical_personnel_category,hospital_level,maximum_amount,minimum_amount,reimbursement_proportion) VALUES ('结算测试住院','结算测试在职','一级医院',1000,10,0.8)");
        session=TestAdminSession.create(mockMvc,jdbcTemplate);
    }

    @Test
    void previewSettleAndCancelAreConsistentAndTransactional() throws Exception {
        jdbcTemplate.update("UPDATE t_prescription_details SET amount=999 WHERE hospitalization_number='ST-VISIT' AND project_coding='ST-A'");
        mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/preview").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items",hasSize(3)))
            .andExpect(jsonPath("$.data.totalFee",closeTo(320.0,0.001)))
            .andExpect(jsonPath("$.data.eligibleFee",closeTo(150.0,0.001)))
            .andExpect(jsonPath("$.data.overLimitSelfFee",closeTo(20.0,0.001)))
            .andExpect(jsonPath("$.data.deductibleSelfFee",closeTo(10.0,0.001)))
            .andExpect(jsonPath("$.data.segmentSelfFee",closeTo(28.0,0.001)))
            .andExpect(jsonPath("$.data.fundFee",closeTo(112.0,0.001)))
            .andExpect(jsonPath("$.data.personalFee",closeTo(208.0,0.001)));
        assertEquals(0,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM personal_annual_expenses WHERE people_id='ST-PERSON'",Integer.class));

        MvcResult settled=mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/settle").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.settlementStatus",is("SETTLED"))).andReturn();
        long settlementId=objectMapper.readTree(settled.getResponse().getContentAsByteArray()).path("data").path("settlementId").asLong();
        assertEquals("已结算",jdbcTemplate.queryForObject("SELECT settlement_flag FROM t_personnel_visits_info WHERE hospitalization_number='ST-VISIT'",String.class));
        assertEquals(new BigDecimal("112.00"),jdbcTemplate.queryForObject("SELECT medicare_expenses FROM personal_annual_expenses WHERE people_id='ST-PERSON' AND year=2026",BigDecimal.class));
        mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/settle").session(session)).andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/reimbursements/settlements/"+settlementId+"/cancel").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"测试取消\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.transactionType",is(0)));
        assertEquals("未结算",jdbcTemplate.queryForObject("SELECT settlement_flag FROM t_personnel_visits_info WHERE hospitalization_number='ST-VISIT'",String.class));
        assertEquals(new BigDecimal("0.00"),jdbcTemplate.queryForObject("SELECT medicare_expenses FROM personal_annual_expenses WHERE people_id='ST-PERSON' AND year=2026",BigDecimal.class));
        assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ext_reimbursement_settlement WHERE original_settlement_id=? AND transaction_type=0",Integer.class,settlementId));
        mockMvc.perform(post("/api/reimbursements/settlements/"+settlementId+"/cancel").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"重复取消\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void missingAndExpiredApprovalAreNotAdopted() throws Exception {
        jdbcTemplate.update("UPDATE t_medicine SET med_approvalmark='需要审批' WHERE med_id='ST-A'");
        jdbcTemplate.update("INSERT INTO t_special_approval (approval_number,person_ID,approval_category,start_date,termination_date,drug_Code,approval_flag) VALUES ('ST-APP','ST-PERSON','特检特治','2025-01-01','2025-12-31','ST-A','审批通过')");
        jdbcTemplate.update("INSERT INTO ext_special_approval_item (approval_number,item_type,project_code) VALUES ('ST-APP','MEDICINE','ST-A')");
        mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/preview").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.fundFee",closeTo(32.0,0.001)));
    }

    @Test
    void activeApprovalAndHospitalLevelAreEnforced() throws Exception {
        jdbcTemplate.update("UPDATE t_medicine SET med_approvalmark='需要审批' WHERE med_id='ST-A'");
        jdbcTemplate.update("INSERT INTO t_special_approval (approval_number,person_ID,approval_category,start_date,termination_date,drug_Code,approval_flag) VALUES ('ST-ACTIVE','ST-PERSON','特检特治','2026-01-01','2026-12-31','ST-A','审批通过')");
        jdbcTemplate.update("INSERT INTO ext_special_approval_item (approval_number,item_type,project_code) VALUES ('ST-ACTIVE','MEDICINE','ST-A')");
        jdbcTemplate.update("UPDATE t_medicine SET med_hos_level='一级医院' WHERE med_id='ST-B'");
        jdbcTemplate.update("UPDATE t_personnel_visits_info SET hospital_grade='三级医院' WHERE hospitalization_number='ST-VISIT'");
        jdbcTemplate.update("INSERT INTO t_minimum_payment_standard (medical_category,medical_personnel_category,hospital_level,minimum_payment_standard) VALUES ('结算测试住院','结算测试在职','三级医院',10)");
        jdbcTemplate.update("INSERT INTO t_individual_segement_self_funded_ratio (medical_category,medical_personnel_category,hospital_level,maximum_amount,minimum_amount,reimbursement_proportion) VALUES ('结算测试住院','结算测试在职','三级医院',1000,10,0.8)");
        mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/preview").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.breakdowns[?(@.feeType == 'HOSPITAL_SELF')].amount",hasItem(100.0)))
            .andExpect(jsonPath("$.data.breakdowns[?(@.feeType == 'SPECIAL_SELF')].amount",hasItem(0.0)));
    }

    @Test
    void annualCapLimitsFundPayment() throws Exception {
        jdbcTemplate.update("UPDATE t_capping_line SET capping_line_fee=100 WHERE medical_personnel_category='结算测试在职'");
        jdbcTemplate.update("INSERT INTO personal_annual_expenses (people_id,year,reimbursement_times,medical_expenses,medicare_expenses,personal_expenses) VALUES ('ST-PERSON',2026,1,200,90,110)");
        mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/preview").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.fundFee",closeTo(10.0,0.001))).andExpect(jsonPath("$.data.capSelfFee",closeTo(102.0,0.001)));
    }

    @Test
    void missingCatalogAndExpiredInstitutionAreRejectedSafely() throws Exception {
        jdbcTemplate.update("DELETE FROM t_medicine WHERE med_id='ST-A'");
        mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/preview").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.breakdowns[?(@.feeType == 'INVALID_ITEM_SELF')].amount",hasItem(120.0)))
            .andExpect(jsonPath("$.data.fundFee",closeTo(32.0,0.001)));

        jdbcTemplate.update("UPDATE t_medical_insititution SET dia_endtime='2025-12-31' WHERE dia_id='ST-HOSP'");
        mockMvc.perform(post("/api/reimbursements/visits/ST-VISIT/preview").session(session))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message",is("就诊医疗机构无效或不在有效期内")));
    }
}

