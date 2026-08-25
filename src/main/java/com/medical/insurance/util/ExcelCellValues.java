package com.medical.insurance.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

public final class ExcelCellValues {

    private ExcelCellValues() {
    }

    public static String text(Cell cell) {
         if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
         if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue())
                .stripTrailingZeros()
                .toPlainString();
        }
         if (cell.getCellType() == CellType.BOOLEAN) {
            return Boolean.toString(cell.getBooleanCellValue());
        }
        String value = cell.toString().trim();
        return value.isEmpty() ? null : value;
    }

    public static BigDecimal decimal(Cell cell, BigDecimal defaultValue) {
        String value = text(cell);
         if (value == null) {
            return defaultValue;
        }
        return new BigDecimal(value);
    }

    public static Timestamp timestamp(Cell cell) {
         if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
         if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            LocalDateTime value = DateUtil.getLocalDateTime(cell.getNumericCellValue());
            return Timestamp.valueOf(value);
        }
        throw new IllegalArgumentException("无法识别的Excel日期：" + cell);
    }
}
