package com.medical.insurance.approval;

import com.medical.insurance.TestAdminSession;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
class ApprovalApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    private MockHttpSession session;

    @BeforeEach
    void prepare() throws Exception {
        jdbcTemplate.update("INSERT INTO people (people_id,name,social_security_id) VALUES ('AP-PERSON','审批测试人员','AP-CARD')");
        jdbcTemplate.update("INSERT INTO t_medical_insititution (dia_id,dia_name) VALUES ('AP-HOSP','审批测试医院')");
        jdbcTemplate.update("INSERT INTO ext_medical_institution_profile (institution_id,institution_name,hospital_level) VALUES ('AP-HOSP','审批测试医院','一级医院')");
        jdbcTemplate.update("INSERT INTO t_medicine (med_id,med_name,med_exp_type,med_exp_level,med_approvalmark,med_specialmark) VALUES ('AP-MED','审批测试药品','西药','乙类','需要审批','特检特治'),('AP-MED-NORMAL','普通药品','西药','甲类','不需要审批','否')");
        jdbcTemplate.update("INSERT INTO t_diagnosis_project (dia_id,dia_name,dia_exp_type,dia_exp_level,dia_approvalmark) VALUES ('AP-DIA','审批测试诊疗','特殊检查费','丙类','需要审批'),('AP-DIA-NORMAL','普通诊疗','检查费','甲类','不需要审批')");
        session=TestAdminSession.create(mockMvc,jdbcTemplate);
    }

    @Test
    void institutionAndSpecialApprovalsUseOriginalTablesAndDiagnosisExtension() throws Exception {
        String institution="{\"approvalNumber\":\"AP-INST-1\",\"personId\":\"AP-PERSON\",\"approvalCategory\":\"人员就诊机构审批\",\"startDate\":\"2026-01-01\",\"terminationDate\":\"2026-12-31\",\"medicalInstitutionCode\":\"AP-HOSP\",\"approvalFlag\":\"审批通过\"}";
        mockMvc.perform(post("/api/approvals/institutions").session(session).contentType(MediaType.APPLICATION_JSON).content(institution))
            .andExpect(status().isCreated());
        assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_application_info WHERE person_ID='AP-PERSON' AND medical_institution_code='AP-HOSP' AND '2026-08-19' BETWEEN start_date AND termination_date",Integer.class));

        mockMvc.perform(get("/api/approvals/special/projects").session(session).param("itemType","MEDICINE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.projectCode == 'AP-MED')]",hasSize(1)))
            .andExpect(jsonPath("$.data[?(@.projectCode == 'AP-MED')].expenseLevel",is(java.util.List.of("乙类"))))
            .andExpect(jsonPath("$.data[?(@.projectCode == 'AP-MED-NORMAL')]",hasSize(0)));
        mockMvc.perform(get("/api/approvals/special/projects").session(session).param("itemType","DIAGNOSIS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.projectCode == 'AP-DIA')]",hasSize(1)))
            .andExpect(jsonPath("$.data[?(@.projectCode == 'AP-DIA')].expenseLevel",is(java.util.List.of("丙类"))))
            .andExpect(jsonPath("$.data[?(@.projectCode == 'AP-DIA-NORMAL')]",hasSize(0)));

        String medicine="{\"approvalNumber\":\"AP-SPECIAL-M\",\"personId\":\"AP-PERSON\",\"approvalCategory\":\"特检特治审批\",\"itemType\":\"MEDICINE\",\"projectCode\":\"AP-MED\",\"startDate\":\"2026-01-01\",\"terminationDate\":\"2026-12-31\",\"approvalFlag\":\"审批通过\"}";
        String diagnosis=medicine.replace("AP-SPECIAL-M","AP-SPECIAL-D").replace("MEDICINE","DIAGNOSIS").replace("AP-MED","AP-DIA");
        String normalMedicine=medicine.replace("AP-SPECIAL-M","AP-SPECIAL-N").replace("AP-MED","AP-MED-NORMAL");
        mockMvc.perform(post("/api/approvals/special").session(session).contentType(MediaType.APPLICATION_JSON).content(normalMedicine))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message",is("所选药品不需要特检特治审批")));
        mockMvc.perform(post("/api/approvals/special").session(session).contentType(MediaType.APPLICATION_JSON).content(medicine)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/approvals/special").session(session).contentType(MediaType.APPLICATION_JSON).content(diagnosis)).andExpect(status().isCreated());
        assertEquals("AP-MED",jdbcTemplate.queryForObject("SELECT drug_Code FROM t_special_approval WHERE approval_number='AP-SPECIAL-M'",String.class));
        assertNull(jdbcTemplate.queryForObject("SELECT drug_Code FROM t_special_approval WHERE approval_number='AP-SPECIAL-D'",String.class));
        assertEquals("DIAGNOSIS",jdbcTemplate.queryForObject("SELECT item_type FROM ext_special_approval_item WHERE approval_number='AP-SPECIAL-D'",String.class));
        assertEquals(1,jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_special_approval s JOIN ext_special_approval_item e ON e.approval_number=s.approval_number WHERE s.person_ID='AP-PERSON' AND e.item_type='DIAGNOSIS' AND e.project_code='AP-DIA'",Integer.class));

        mockMvc.perform(get("/api/approvals/special").session(session).param("keyword","AP-PERSON"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data",hasSize(2))).andExpect(jsonPath("$.data[0].approvalFlag",is("审批通过")));
        mockMvc.perform(delete("/api/approvals/special/AP-SPECIAL-D").session(session)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/approvals/special/AP-SPECIAL-M").session(session)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/approvals/institutions/AP-INST-1").session(session)).andExpect(status().isOk());
    }
}

