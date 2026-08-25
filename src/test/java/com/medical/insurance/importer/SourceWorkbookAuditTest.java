package com.medical.insurance.importer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

class SourceWorkbookAuditTest {

    private final Path workspace = Paths.get("..", "..")
        .toAbsolutePath()
        .normalize();

    @Test
    void readEveryCellFromAllSourceWorkbooks() throws Exception {
        inspect(workspace.resolve("01.项目需求/项目功能结构-医疗保险中心.xls"), true);
        inspect(workspace.resolve("02.数据库设计/参数.xls"), false);
        inspect(workspace.resolve("02.数据库设计/数据.xls"), false);
    }

    private void inspect(Path path, boolean printAllRows) throws Exception {
        assertTrue(Files.isRegularFile(path), "缺少文件：" + path);
        DataFormatter formatter = new DataFormatter();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long nonBlankCells = 0;

        System.out.println("WORKBOOK=" + path.getFileName());
        try (InputStream input = Files.newInputStream(path);
             Workbook workbook = new HSSFWorkbook(input)) {
            System.out.println("SHEET_COUNT=" + workbook.getNumberOfSheets());
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                int maxColumns = 0;
                int nonBlankRows = 0;
                List<String> firstRows = new ArrayList<>();
                List<String> lastRows = new ArrayList<>();
                System.out.println("SHEET=" + sheet.getSheetName());
                for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;
                    maxColumns = Math.max(maxColumns, row.getLastCellNum());
                    List<String> values = new ArrayList<>();
                    boolean rowHasValue = false;
                    for (int columnIndex = 0; columnIndex < Math.max(0, row.getLastCellNum()); columnIndex++) {
                        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                        values.add(value);
                        if (!value.isEmpty()) {
                            rowHasValue = true;
                            nonBlankCells++;
                        }
                        String canonical = sheet.getSheetName() + "\u001f" + rowIndex + "\u001f"
                            + columnIndex + "\u001f" + value + "\n";
                        digest.update(canonical.getBytes(StandardCharsets.UTF_8));
                    }
                    if (!rowHasValue) continue;
                    nonBlankRows++;
                    String line = (rowIndex + 1) + ":" + String.join(" | ", values);
                    if (printAllRows || firstRows.size() < 3) firstRows.add(line);
                    if (!printAllRows) {
                        if (lastRows.size() == 3) lastRows.remove(0);
                        lastRows.add(line);
                    }
                }
                System.out.println("ROWS=" + nonBlankRows + ",MAX_COLUMNS=" + maxColumns);
                firstRows.forEach(value -> System.out.println("ROW=" + value));
                if (!printAllRows) lastRows.forEach(value -> System.out.println("TAIL=" + value));
            }
        }
        System.out.println("NON_BLANK_CELLS=" + nonBlankCells);
        System.out.println("CONTENT_SHA256=" + HexFormat.of().formatHex(digest.digest()));
    }
}
