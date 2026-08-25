package com.medical.insurance.bulk;

import com.medical.insurance.TestAdminSession;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class BulkDataIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    private MockHttpSession session;

    @BeforeEach
    void login() throws Exception {
        session=TestAdminSession.create(mockMvc,jdbc);
    }

    @Test
    void xlsAndXlsxFrameworkImportsExportsAndSafelyDeletes() throws Exception {
        mockMvc.perform(get("/api/bulk/modules").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].code",hasItem("diseases")));
        mockMvc.perform(get("/api/bulk/diseases/template.xlsx").session(session))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        MockMultipartFile legacy=new MockMultipartFile("file","数据.xls","application/vnd.ms-excel",legacyMedicineWorkbook());
        mockMvc.perform(multipart("/api/bulk/medicines/import").file(legacy).param("mode","VALIDATE_ONLY").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successRows",is(1)))
            .andExpect(jsonPath("$.data.failureRows",is(0)));

        MockMultipartFile file=new MockMultipartFile("file","diseases.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",diseaseWorkbook());
        mockMvc.perform(multipart("/api/bulk/diseases/import").file(file).param("mode","UPSERT").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalRows",is(1)))
            .andExpect(jsonPath("$.data.successRows",is(1)))
            .andExpect(jsonPath("$.data.failureRows",is(0)))
            .andExpect(jsonPath("$.data.errors",hasSize(0)));

        mockMvc.perform(get("/api/bulk/diseases/export.xlsx").session(session))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"));

        mockMvc.perform(delete("/api/bulk/diseases").session(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"keys\":[{\"disease_id\":\"BULK-DIS\"}],\"confirmation\":\"DELETE 1\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.deletedRows",is(1)));
    }

    private byte[] diseaseWorkbook() throws Exception {
        try(Workbook workbook=new XSSFWorkbook();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            var sheet=workbook.createSheet("t_disease_");
            var header=sheet.createRow(0);
            String[] columns={"disease_id","disease_name","disease_type","disease_reimbursement_standards","notes"};
            for(int i=0;i<columns.length;i++)header.createCell(i).setCellValue(columns[i]);
            var row=sheet.createRow(1);
            row.createCell(0).setCellValue("BULK-DIS");row.createCell(1).setCellValue("批量测试病种");row.createCell(2).setCellValue("测试");row.createCell(3).setCellValue("可报销");row.createCell(4).setCellValue("xlsx导入");
            workbook.write(output);return output.toByteArray();
        }
    }

    private byte[] legacyMedicineWorkbook() throws Exception {
        try(Workbook workbook=new HSSFWorkbook();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            var sheet=workbook.createSheet("药品");sheet.createRow(0).createCell(0).setCellValue("序号");
            var row=sheet.createRow(1);
            row.createCell(0).setCellValue(1);row.createCell(1).setCellValue("BULK-MED");row.createCell(2).setCellValue("批量测试药");row.createCell(3).setCellValue("片");row.createCell(4).setCellValue("10mg");
            row.createCell(5).setCellValue(DateUtil.getExcelDate(LocalDateTime.of(2026,1,1,0,0)));row.createCell(6).setCellValue(DateUtil.getExcelDate(LocalDateTime.of(2026,12,31,0,0)));
            row.createCell(7).setCellValue("有效");row.createCell(8).setCellValue("西药");row.createCell(9).setCellValue("乙类");row.createCell(10).setCellValue(100);row.createCell(11).setCellValue("否");row.createCell(12).setCellValue("测试商品名");
            workbook.write(output);return output.toByteArray();
        }
    }
}
