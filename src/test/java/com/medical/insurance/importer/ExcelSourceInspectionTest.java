package com.medical.insurance.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

class ExcelSourceInspectionTest {

    private final Path sourceDirectory = Paths.get("..", "..", "02.数据库设计")
        .toAbsolutePath()
        .normalize();

    @Test
    void sourceWorkbookCountsAndLongCodeRemainStable() throws Exception {
        try (InputStream input = Files.newInputStream(sourceDirectory.resolve("参数.xls"));
             Workbook workbook = new HSSFWorkbook(input)) {
            assertEquals(516, workbook.getSheet("数据字典").getLastRowNum());
        }

        try (InputStream input = Files.newInputStream(sourceDirectory.resolve("数据.xls"));
             Workbook workbook = new HSSFWorkbook(input)) {
            assertEquals(1025, workbook.getSheet("药品").getLastRowNum());
            assertEquals(899, workbook.getSheet("诊疗项目").getLastRowNum());
            assertEquals(2, workbook.getSheet("服务设施").getLastRowNum());
            assertEquals(
                "122000000000689",
                BigDecimal.valueOf(workbook.getSheet("药品").getRow(622).getCell(1).getNumericCellValue())
                    .stripTrailingZeros()
                    .toPlainString()
            );
        }
    }
}
