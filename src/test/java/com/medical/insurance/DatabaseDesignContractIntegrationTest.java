package com.medical.insurance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseDesignContractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allOriginalTablesAndColumnsMatchDatabaseOverviewDocument() {
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("t_medicine", columns("med_id,med_name,med_exp_type,med_exp_level,med_measurement,med_max_prize,med_approvalmark,med_hos_level,med_size,med_tradename,med_starttime,med_endtime,med_valid,med_specialmark"));
        expected.put("t_diagnosis_project", columns("dia_id,dia_name,dia_exp_type,dia_exp_level,dia_max_prize,dia_starttime,dia_endtime,dia_valid,dia_hos_level,dia_approvalmark"));
        expected.put("t_service_facilities", columns("ser_id,ser_name,ser_exp_type,ser_starttime,ser_endtime,ser_valid"));
        expected.put("t_medical_insititution", columns("dia_id,dia_name,dia_exp_type,dia_exp_level,dia_max_prize,dia_starttime,dia_endtime,dia_valid,dia_hos_level,dia_approvalmark"));
        expected.put("t_disease_", columns("disease_id,disease_name,disease_type,disease_reimbursement_standards,notes"));
        expected.put("t_capping_line", columns("medical_personnel_category,capping_line_fee"));
        expected.put("t_minimum_payment_standard", columns("medical_category,medical_personnel_category,hospital_level,minimum_payment_standard"));
        expected.put("t_individual_segement_self_funded_ratio", columns("medical_category,medical_personnel_category,hospital_level,maximum_amount,minimum_amount,reimbursement_proportion"));
        expected.put("t_application_info", columns("approval_number,person_ID,approval_category,start_date,termination_date,medical_institution_code,approval_opinions,approver,approval_date,approval_flag"));
        expected.put("t_special_approval", columns("approval_number,person_ID,approval_category,start_date,termination_date,drug_Code,approval_opinions,approver,approval_date,approval_flag"));
        expected.put("t_personnel_visits_info", columns("person_ID,hospitalization_number,designated_number,medical_category,admission_date,discharge_date,disease_code,hospital_grade,admission_code,diagnosed_name,discharge_reason,settlement_flag"));
        expected.put("t_prescription_details", columns("hospitalization_number,chargeable_items_Category,project_coding,project_name,unit_price,quantity,amount"));
        expected.put("personal_annual_expenses", columns("people_id,year,reimbursement_times,medical_expenses,medicare_expenses,personal_expenses"));
        expected.put("company", columns("company_id,company_name,company_type,address,postcode,phone_number"));
        expected.put("people", columns("people_id,ID_type,ID,name,sex,nationality,brithday,work_date,retirement_date,retirement,residence_type,residence_adress,education,political_status,identity,employment,technical_position,worker_level,marriage,administrative_position,note,company_id,medical_personnel,health,model_worker,cadre,civil_servant,authorized_strength,resident_type,flexible_employment,migrant_worker,employer,military_personnel,social_security_id,medins_id"));

        expected.forEach((table, columns) -> assertEquals(columns, actualColumns(table), table));
    }

    private List<String> actualColumns(String table) {
        return jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? ORDER BY ORDINAL_POSITION",
            String.class,
            table
        );
    }

    private List<String> columns(String names) {
        return Arrays.asList(names.split(","));
    }
}
