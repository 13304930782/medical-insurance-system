package com.medical.insurance.config;

import com.medical.insurance.util.ExcelCellValues;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExcelDataImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExcelDataImportRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataImportProperties properties;

    public ExcelDataImportRunner(JdbcTemplate jdbcTemplate, DataImportProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            log.info("Excel初始化导入未启用；需要导入时设置 DATA_IMPORT_ENABLED=true");
            return;
        }

        Path parameterFile = resolveRequiredFile(properties.getParameterFile());
        Path catalogFile = resolveRequiredFile(properties.getCatalogFile());

        int dictionaryCount = importDictionary(parameterFile);
        CatalogCounts catalogCounts = importCatalogs(catalogFile);

        log.info(
            "初始化导入完成：参数={}，药品={}，诊疗项目={}，服务设施={}",
            dictionaryCount,
            catalogCounts.medicines,
            catalogCounts.diagnosisProjects,
            catalogCounts.facilities
        );
    }

    private Path resolveRequiredFile(String configuredPath) {
        Path path = Paths.get(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("找不到初始化文件：" + path);
        }
        return path;
    }

    private int importDictionary(Path file) throws IOException {
        String sql = "INSERT INTO ext_data_dictionary "
            + "(source_row_no, parameter_category, parameter_value, parameter_label) "
            + "VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "parameter_category=VALUES(parameter_category), "
            + "parameter_value=VALUES(parameter_value), "
            + "parameter_label=VALUES(parameter_label)";

        try (InputStream input = Files.newInputStream(file);
             Workbook workbook = new HSSFWorkbook(input)) {
            Sheet sheet = requiredSheet(workbook, "数据字典");
            List<Object[]> rows = new ArrayList<>();

            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || ExcelCellValues.text(row.getCell(1)) == null) {
                    continue;
                }
                rows.add(new Object[] {
                    Integer.parseInt(ExcelCellValues.text(row.getCell(0))),
                    ExcelCellValues.text(row.getCell(1)),
                    ExcelCellValues.text(row.getCell(2)),
                    ExcelCellValues.text(row.getCell(3))
                });
            }

            jdbcTemplate.batchUpdate(sql, rows);
            requireCount("参数", rows.size(), 516);
            return rows.size();
        }
    }

    private CatalogCounts importCatalogs(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file);
             Workbook workbook = new HSSFWorkbook(input)) {
            int medicines = importMedicines(requiredSheet(workbook, "药品"));
            int diagnosisProjects = importDiagnosisProjects(requiredSheet(workbook, "诊疗项目"));
            int facilities = importFacilities(requiredSheet(workbook, "服务设施"));
            return new CatalogCounts(medicines, diagnosisProjects, facilities);
        }
    }

    private int importMedicines(Sheet sheet) {
        String sql = "INSERT INTO t_medicine "
            + "(med_id, med_name, med_exp_type, med_exp_level, med_measurement, "
            + "med_max_prize, med_approvalmark, med_hos_level, med_size, med_tradename, "
            + "med_starttime, med_endtime, med_valid, med_specialmark) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "med_name=VALUES(med_name), med_exp_type=VALUES(med_exp_type), "
            + "med_exp_level=VALUES(med_exp_level), med_measurement=VALUES(med_measurement), "
            + "med_max_prize=VALUES(med_max_prize), med_approvalmark=VALUES(med_approvalmark), "
            + "med_hos_level=VALUES(med_hos_level), med_size=VALUES(med_size), "
            + "med_tradename=VALUES(med_tradename), med_starttime=VALUES(med_starttime), "
            + "med_endtime=VALUES(med_endtime), med_valid=VALUES(med_valid), "
            + "med_specialmark=VALUES(med_specialmark)";

        List<Object[]> rows = new ArrayList<>();
        for (int index = 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (row == null || ExcelCellValues.text(row.getCell(1)) == null) {
                continue;
            }
            rows.add(new Object[] {
                ExcelCellValues.text(row.getCell(1)),
                ExcelCellValues.text(row.getCell(2)),
                ExcelCellValues.text(row.getCell(8)),
                ExcelCellValues.text(row.getCell(9)),
                ExcelCellValues.text(row.getCell(3)),
                ExcelCellValues.decimal(row.getCell(10), BigDecimal.ZERO),
                "不需要审批",
                "所有医院",
                ExcelCellValues.text(row.getCell(4)),
                ExcelCellValues.text(row.getCell(12)),
                ExcelCellValues.timestamp(row.getCell(5)),
                ExcelCellValues.timestamp(row.getCell(6)),
                ExcelCellValues.text(row.getCell(7)),
                ExcelCellValues.text(row.getCell(11))
            });
        }

        jdbcTemplate.batchUpdate(sql, rows);
        requireCount("药品", rows.size(), 1025);
        requireValue("夏星镇痛冲剂药品编码", "122000000000689", rows.get(621)[0]);
        return rows.size();
    }

    private int importDiagnosisProjects(Sheet sheet) {
        String sql = "INSERT INTO t_diagnosis_project "
            + "(dia_id, dia_name, dia_exp_type, dia_exp_level, dia_max_prize, "
            + "dia_starttime, dia_endtime, dia_valid, dia_hos_level, dia_approvalmark) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "dia_name=VALUES(dia_name), dia_exp_type=VALUES(dia_exp_type), "
            + "dia_exp_level=VALUES(dia_exp_level), dia_max_prize=VALUES(dia_max_prize), "
            + "dia_starttime=VALUES(dia_starttime), dia_endtime=VALUES(dia_endtime), "
            + "dia_valid=VALUES(dia_valid), dia_hos_level=VALUES(dia_hos_level), "
            + "dia_approvalmark=VALUES(dia_approvalmark)";

        List<Object[]> rows = new ArrayList<>();
        for (int index = 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (row == null || ExcelCellValues.text(row.getCell(1)) == null) {
                continue;
            }
            rows.add(new Object[] {
                ExcelCellValues.text(row.getCell(1)),
                ExcelCellValues.text(row.getCell(2)),
                ExcelCellValues.text(row.getCell(3)),
                ExcelCellValues.text(row.getCell(4)),
                BigDecimal.ZERO,
                ExcelCellValues.timestamp(row.getCell(5)),
                ExcelCellValues.timestamp(row.getCell(6)),
                ExcelCellValues.text(row.getCell(7)),
                "所有医院",
                "不需要审批"
            });
        }

        jdbcTemplate.batchUpdate(sql, rows);
        requireCount("诊疗项目", rows.size(), 899);
        return rows.size();
    }

    private int importFacilities(Sheet sheet) {
        String sql = "INSERT INTO t_service_facilities "
            + "(ser_id, ser_name, ser_exp_type, ser_starttime, ser_endtime, ser_valid) "
            + "VALUES (?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "ser_name=VALUES(ser_name), ser_exp_type=VALUES(ser_exp_type), "
            + "ser_starttime=VALUES(ser_starttime), ser_endtime=VALUES(ser_endtime), "
            + "ser_valid=VALUES(ser_valid)";

        List<Object[]> rows = new ArrayList<>();
        for (int index = 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (row == null || ExcelCellValues.text(row.getCell(1)) == null) {
                continue;
            }
            rows.add(new Object[] {
                ExcelCellValues.text(row.getCell(1)),
                ExcelCellValues.text(row.getCell(2)),
                ExcelCellValues.text(row.getCell(3)),
                ExcelCellValues.timestamp(row.getCell(4)),
                ExcelCellValues.timestamp(row.getCell(5)),
                ExcelCellValues.text(row.getCell(6))
            });
        }

        jdbcTemplate.batchUpdate(sql, rows);
        requireCount("服务设施", rows.size(), 2);
        return rows.size();
    }

    private Sheet requiredSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new IllegalStateException("Excel中不存在工作表：" + name);
        }
        return sheet;
    }

    private void requireCount(String label, int actual, int expected) {
        if (actual != expected) {
            throw new IllegalStateException(label + "数量不正确，期望=" + expected + "，实际=" + actual);
        }
    }

    private void requireValue(String label, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(label + "不正确，期望=" + expected + "，实际=" + actual);
        }
    }

    private static final class CatalogCounts {
        private final int medicines;
        private final int diagnosisProjects;
        private final int facilities;

        private CatalogCounts(int medicines, int diagnosisProjects, int facilities) {
            this.medicines = medicines;
            this.diagnosisProjects = diagnosisProjects;
            this.facilities = facilities;
        }
    }
}
