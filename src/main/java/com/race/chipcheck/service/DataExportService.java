package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import com.race.chipcheck.model.VerificationRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据导出服务
 */
public class DataExportService {
    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);
    private final DatabaseService databaseService;

    public DataExportService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * 导出核验记录
     * 文件名格式：芯片核验记录+赛事名称+导出时间(YYYYMMDDHHMMSS).xlsx
     */
    public void exportVerificationRecords(Long raceId, String raceName, File outputFile) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("核验记录");

        // 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"核验时间", "芯片号", "参赛号", "姓名", "状态", "备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 从数据库查询核验记录
        List<VerificationRecord> records = getVerificationRecords(raceId);

        // 填充数据
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < records.size(); i++) {
            Row row = sheet.createRow(i + 1);
            VerificationRecord record = records.get(i);

            row.createCell(0).setCellValue(record.getVerificationTime().format(formatter));
            row.createCell(1).setCellValue(record.getChipId());
            row.createCell(2).setCellValue(record.getBibNumber());
            row.createCell(3).setCellValue(record.getName());
            row.createCell(4).setCellValue(record.getStatus());
            row.createCell(5).setCellValue(record.getRemark());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
        }
        workbook.close();

        logger.info("导出 {} 条核验记录到文件：{}", records.size(), outputFile.getName());
    }

    /**
     * 从数据库查询核验记录
     */
    private List<VerificationRecord> getVerificationRecords(Long raceId) {
        List<VerificationRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM verification_records WHERE race_id = ? ORDER BY verification_time DESC";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, raceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToVerificationRecord(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("查询核验记录失败：赛事ID={}", raceId, e);
        }

        return records;
    }

    /**
     * 将ResultSet映射为VerificationRecord对象
     */
    private VerificationRecord mapResultSetToVerificationRecord(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long raceId = rs.getLong("race_id");
        String verificationTimeStr = rs.getString("verification_time");
        LocalDateTime verificationTime = LocalDateTime.parse(verificationTimeStr);
        String chipId = rs.getString("chip_id");

        Long athleteId = rs.getLong("athlete_id");
        if (rs.wasNull()) {
            athleteId = null;
        }

        String bibNumber = rs.getString("bib_number");
        String name = rs.getString("name");
        String status = rs.getString("status");
        String remark = rs.getString("remark");

        return new VerificationRecord(id, raceId, athleteId, verificationTime,
            chipId, bibNumber, name, status, remark);
    }

    /**
     * 生成导出文件名
     * 格式：芯片核验记录+赛事名称+导出时间(YYYYMMDDHHMMSS).xlsx
     */
    public static String generateExportFileName(String raceName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "芯片核验记录" + raceName + timestamp + ".xlsx";
    }

    /**
     * 生成未核验选手导出文件名
     * 格式：未核验选手名单+赛事名称+导出时间(YYYYMMDDHHMMSS).xlsx
     */
    public static String generateUnverifiedExportFileName(String raceName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "未核验选手名单" + raceName + timestamp + ".xlsx";
    }

    /**
     * 导出未核验选手名单
     * 文件名格式：未核验选手名单+赛事名称+导出时间(YYYYMMDDHHMMSS).xlsx
     */
    public void exportUnverifiedAthletes(Long raceId, String raceName, File outputFile) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("未核验选手");

        // 创建表头样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"参赛号", "姓名", "芯片1", "芯片2", "芯片3", "芯片4"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 获取未核验选手列表
        List<Athlete> unverifiedAthletes = getUnverifiedAthletes(raceId);

        // 填充数据
        for (int i = 0; i < unverifiedAthletes.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Athlete athlete = unverifiedAthletes.get(i);

            row.createCell(0).setCellValue(athlete.getBibNumber());
            row.createCell(1).setCellValue(athlete.getName());
            row.createCell(2).setCellValue(athlete.getChip1() != null ? athlete.getChip1() : "");
            row.createCell(3).setCellValue(athlete.getChip2() != null ? athlete.getChip2() : "");
            row.createCell(4).setCellValue(athlete.getChip3() != null ? athlete.getChip3() : "");
            row.createCell(5).setCellValue(athlete.getChip4() != null ? athlete.getChip4() : "");
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
        }
        workbook.close();

        logger.info("导出 {} 个未核验选手到文件：{}", unverifiedAthletes.size(), outputFile.getName());
    }

    /**
     * 获取未核验选手列表
     * 逻辑：查询所有选手，排除已核验成功的选手
     */
    private List<Athlete> getUnverifiedAthletes(Long raceId) {
        List<Athlete> unverifiedAthletes = new ArrayList<>();

        // 1. 获取所有已核验成功的参赛号（去重）
        Set<String> verifiedBibNumbers = new HashSet<>();
        String verifiedSql = "SELECT DISTINCT bib_number FROM verification_records " +
                           "WHERE race_id = ? AND status = '成功'";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(verifiedSql)) {
            stmt.setLong(1, raceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    verifiedBibNumbers.add(rs.getString("bib_number"));
                }
            }
        } catch (SQLException e) {
            logger.error("查询已核验参赛号失败：赛事ID={}", raceId, e);
        }

        logger.info("赛事ID={} 已核验参赛号数量：{}", raceId, verifiedBibNumbers.size());

        // 2. 获取所有选手
        String athletesSql = "SELECT * FROM athletes WHERE race_id = ? ORDER BY bib_number";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(athletesSql)) {
            stmt.setLong(1, raceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Athlete athlete = mapResultSetToAthlete(rs);

                    // 如果该选手的参赛号不在已核验列表中，则为未核验选手
                    if (!verifiedBibNumbers.contains(athlete.getBibNumber())) {
                        unverifiedAthletes.add(athlete);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("查询选手列表失败：赛事ID={}", raceId, e);
        }

        logger.info("赛事ID={} 未核验选手数量：{}", raceId, unverifiedAthletes.size());

        return unverifiedAthletes;
    }

    /**
     * 将ResultSet映射为Athlete对象
     */
    private Athlete mapResultSetToAthlete(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long raceId = rs.getLong("race_id");
        String bibNumber = rs.getString("bib_number");
        String name = rs.getString("name");
        String chip1 = rs.getString("chip1");
        String chip2 = rs.getString("chip2");
        String chip3 = rs.getString("chip3");
        String chip4 = rs.getString("chip4");

        return new Athlete(id, raceId, bibNumber, name, chip1, chip2, chip3, chip4);
    }
}
