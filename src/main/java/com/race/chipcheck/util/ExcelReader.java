package com.race.chipcheck.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel读取工具
 */
public class ExcelReader {
    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    /**
     * Excel行数据（用于导入）
     */
    public static class AthleteRow {
        private String bibNumber;
        private String name;
        private String chip1;
        private String chip2;
        private String chip3;
        private String chip4;

        public AthleteRow(String bibNumber, String name, String chip1, String chip2, String chip3, String chip4) {
            this.bibNumber = bibNumber;
            this.name = name;
            this.chip1 = chip1;
            this.chip2 = chip2;
            this.chip3 = chip3;
            this.chip4 = chip4;
        }

        public String getBibNumber() { return bibNumber; }
        public String getName() { return name; }
        public String getChip1() { return chip1; }
        public String getChip2() { return chip2; }
        public String getChip3() { return chip3; }
        public String getChip4() { return chip4; }
    }

    /**
     * 读取选手名单Excel文件
     * 预期格式：参赛号码 | 姓名 | 芯片1 | 芯片2 | 芯片3 | 芯片4
     * 第一行是表头，从第二行开始读取数据
     */
    public static List<AthleteRow> readAthletes(File excelFile) {
        List<AthleteRow> rows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;

            for (Row row : sheet) {
                rowCount++;

                // 跳过表头（第一行）
                if (rowCount == 1) {
                    continue;
                }

                // 跳过空行
                if (isRowEmpty(row)) {
                    continue;
                }

                String bibNumber = getCellValueAsString(row.getCell(0));
                String name = getCellValueAsString(row.getCell(1));
                String chip1 = getCellValueAsString(row.getCell(2));
                String chip2 = getCellValueAsString(row.getCell(3));
                String chip3 = getCellValueAsString(row.getCell(4));
                String chip4 = getCellValueAsString(row.getCell(5));

                rows.add(new AthleteRow(bibNumber, name, chip1, chip2, chip3, chip4));
            }

            logger.info("从Excel读取 {} 条选手记录", rows.size());

        } catch (IOException e) {
            logger.error("读取Excel文件失败：{}", excelFile.getName(), e);
            throw new RuntimeException("读取Excel文件失败", e);
        }

        return rows;
    }

    /**
     * 判断行是否为空
     */
    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 获取单元格值作为字符串
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    // 数字类型转字符串（去除小数点）
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        yield String.valueOf((long) numericValue);
                    } else {
                        yield String.valueOf(numericValue);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
