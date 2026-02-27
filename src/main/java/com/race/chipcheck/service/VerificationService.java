package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import com.race.chipcheck.model.UnverifiedChip;
import com.race.chipcheck.model.VerificationRecord;
import com.race.chipcheck.util.AlertHelper;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 核验服务（处理RFID读卡，多芯片匹配，去重统计）
 */
public class VerificationService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private final AthleteService athleteService;
    private final DatabaseService databaseService;
    private final AlertSoundService alertSoundService;
    private final TTSService ttsService;
    private final PreferenceService preferenceService;
    private final ObservableList<VerificationRecord> records;
    private final Set<String> verifiedBibNumbers = new HashSet<>();  // 已核验的参赛号（去重）
    private final Set<String> verifiedChipIds = new HashSet<>();     // 已核验的芯片号（去重）

    private Long currentRaceId;  // 当前赛事ID

    /** 核验结果回调（用于右侧面板显示参赛号、姓名） */
    private VerificationResultCallback resultCallback;

    public interface VerificationResultCallback {
        void onResult(String chipId, String bibNumber, String name, boolean success);
    }

    public void setResultCallback(VerificationResultCallback callback) {
        this.resultCallback = callback;
    }

    public VerificationService(AthleteService athleteService,
                              DatabaseService databaseService,
                              AlertSoundService alertSoundService,
                              TTSService ttsService,
                              PreferenceService preferenceService,
                              ObservableList<VerificationRecord> records) {
        this.athleteService = athleteService;
        this.databaseService = databaseService;
        this.alertSoundService = alertSoundService;
        this.ttsService = ttsService;
        this.preferenceService = preferenceService;
        this.records = records;
    }

    /**
     * 设置当前赛事ID
     */
    public void setCurrentRaceId(Long raceId) {
        this.currentRaceId = raceId;
        // 从数据库加载历史核验记录，恢复已核验人数统计
        loadVerifiedBibNumbersFromDatabase();
    }

    /**
     * 从数据库加载已核验的参赛号和芯片号（用于恢复统计）
     */
    private void loadVerifiedBibNumbersFromDatabase() {
        verifiedBibNumbers.clear();
        verifiedChipIds.clear();

        if (currentRaceId == null) {
            return;
        }

        String sqlBib = "SELECT DISTINCT bib_number FROM verification_records WHERE race_id = ? AND status = '成功'";
        String sqlChip = "SELECT DISTINCT chip_id FROM verification_records WHERE race_id = ? AND status = '成功' AND chip_id IS NOT NULL AND chip_id <> ''";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sqlBib)) {
            stmt.setLong(1, currentRaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String bibNumber = rs.getString("bib_number");
                    if (bibNumber != null && !bibNumber.isEmpty()) {
                        verifiedBibNumbers.add(bibNumber);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("加载历史核验记录失败（参赛号）", e);
        }

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sqlChip)) {
            stmt.setLong(1, currentRaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String chipId = rs.getString("chip_id");
                    if (chipId != null && !chipId.isEmpty()) {
                        verifiedChipIds.add(chipId);
                    }
                }
            }

            logger.info("从历史记录中恢复核验统计：已核验人数 {}，已核验芯片数 {}", verifiedBibNumbers.size(), verifiedChipIds.size());

        } catch (SQLException e) {
            logger.error("加载历史核验记录失败（芯片号）", e);
        }
    }

    /**
     * 处理读取到的芯片
     */
    public void processChip(String chipId) {
        if (currentRaceId == null) {
            logger.warn("未选择赛事，无法进行核验");
            Platform.runLater(() -> {
                AlertHelper.showWarning("核验失败", "请先选择赛事");
            });
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 查找选手（芯片1-4任意一个匹配即可）
        Athlete athlete = athleteService.findAthleteByChip(currentRaceId, chipId);

        VerificationRecord record;
        if (athlete != null) {
            // 找到选手 - 核验成功
            record = new VerificationRecord(null, currentRaceId, athlete.getId(), now,
                chipId, athlete.getBibNumber(), athlete.getName(), "成功", "");

            // 记录已核验的参赛号和芯片号（用于统计）
            verifiedBibNumbers.add(athlete.getBibNumber());
            if (chipId != null && !chipId.isEmpty()) {
                verifiedChipIds.add(chipId);
            }

            logger.info("核验成功：参赛号={}, 姓名={}, 芯片={}", athlete.getBibNumber(), athlete.getName(), chipId);

            // 播报选手信息
            ttsService.speakAthleteInfo(athlete);

        } else {
            // 未找到选手 - 核验失败
            record = new VerificationRecord(null, currentRaceId, null, now,
                chipId, "未知", "未知选手", "失败", "芯片未绑定");

            logger.warn("核验失败：芯片未绑定 (芯片={})", chipId);

            // 根据配置决定是否播放报警声音
            alertSoundService.playAlert();

            // 显示错误提示
            Platform.runLater(() -> {
                AlertHelper.showError("核验失败", "未找到芯片：" + chipId);
            });
        }

        // 保存到数据库
        saveVerificationRecord(record);

        // 通知右侧面板显示结果
        final String chip = record.getChipId();
        final String bib = record.getBibNumber();
        final String name = record.getName();
        final boolean success = (athlete != null);
        if (resultCallback != null) {
            Platform.runLater(() -> resultCallback.onResult(
                chip != null ? chip : "",
                bib != null ? bib : "",
                name != null ? name : "",
                success
            ));
        }

        // 添加到记录列表（TableView会自动更新）
        Platform.runLater(() -> {
            records.add(0, record);  // 新记录插入顶部
        });
    }

    /**
     * 保存核验记录到数据库
     */
    private void saveVerificationRecord(VerificationRecord record) {
        String insertSql = "INSERT INTO verification_records (race_id, verification_time, chip_id, athlete_id, bib_number, name, status, remark) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String selectIdSql = "SELECT last_insert_rowid() as id";

        try (PreparedStatement insertStmt = databaseService.getConnection().prepareStatement(insertSql)) {
            insertStmt.setLong(1, record.getRaceId());
            insertStmt.setString(2, record.getVerificationTime().toString());
            insertStmt.setString(3, record.getChipId());

            if (record.getAthleteId() != null && record.getAthleteId() > 0) {
                insertStmt.setLong(4, record.getAthleteId());
            } else {
                insertStmt.setNull(4, Types.INTEGER);
            }

            insertStmt.setString(5, record.getBibNumber());
            insertStmt.setString(6, record.getName());
            insertStmt.setString(7, record.getStatus());
            insertStmt.setString(8, record.getRemark());

            insertStmt.executeUpdate();

            // 使用SQLite的last_insert_rowid()获取刚插入的ID
            try (Statement selectStmt = databaseService.getConnection().createStatement();
                 ResultSet rs = selectStmt.executeQuery(selectIdSql)) {

                if (rs.next()) {
                    Long id = rs.getLong("id");
                    record.setId(id);
                }
            }

        } catch (SQLException e) {
            logger.error("保存核验记录失败", e);
        }
    }

    /**
     * 获取核验记录数
     */
    public int getTotalRecordCount() {
        return records.size();
    }

    /**
     * 获取总人数（当前赛事的选手总数）
     */
    public int getTotalAthleteCount() {
        if (currentRaceId == null) {
            return 0;
        }
        return athleteService.getAthleteCountByRaceId(currentRaceId);
    }

    /**
     * 获取核验人数（去重后的参赛号数量）
     */
    public int getVerifiedAthleteCount() {
        return verifiedBibNumbers.size();
    }

    /**
     * 获取芯片总数（当前赛事所有选手绑定的去重芯片数）
     */
    public int getTotalChipCount() {
        if (currentRaceId == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(DISTINCT chip) FROM (
                SELECT chip1 AS chip FROM athletes WHERE race_id = ? AND chip1 IS NOT NULL AND chip1 <> ''
                UNION ALL
                SELECT chip2 AS chip FROM athletes WHERE race_id = ? AND chip2 IS NOT NULL AND chip2 <> ''
                UNION ALL
                SELECT chip3 AS chip FROM athletes WHERE race_id = ? AND chip3 IS NOT NULL AND chip3 <> ''
                UNION ALL
                SELECT chip4 AS chip FROM athletes WHERE race_id = ? AND chip4 IS NOT NULL AND chip4 <> ''
            )
            """;

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, currentRaceId);
            stmt.setLong(2, currentRaceId);
            stmt.setLong(3, currentRaceId);
            stmt.setLong(4, currentRaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("查询芯片总数失败：赛事ID={}", currentRaceId, e);
        }
        return 0;
    }

    /**
     * 获取已核验芯片数（去重）
     */
    public int getVerifiedChipCount() {
        return verifiedChipIds.size();
    }

    /**
     * 获取当前赛事未核验的芯片列表（按选手展开）
     */
    public List<UnverifiedChip> getUnverifiedChips() {
        List<UnverifiedChip> result = new ArrayList<>();
        if (currentRaceId == null) {
            return result;
        }

        List<Athlete> athletes = athleteService.getAthletesByRaceId(currentRaceId);
        for (Athlete athlete : athletes) {
            addIfUnverified(result, athlete.getBibNumber(), athlete.getName(), athlete.getChip1());
            addIfUnverified(result, athlete.getBibNumber(), athlete.getName(), athlete.getChip2());
            addIfUnverified(result, athlete.getBibNumber(), athlete.getName(), athlete.getChip3());
            addIfUnverified(result, athlete.getBibNumber(), athlete.getName(), athlete.getChip4());
        }
        return result;
    }

    private void addIfUnverified(List<UnverifiedChip> list, String bibNumber, String name, String chipId) {
        if (chipId == null || chipId.trim().isEmpty()) {
            return;
        }
        String chip = chipId.trim();
        if (!verifiedChipIds.contains(chip)) {
            list.add(new UnverifiedChip(bibNumber, name, chip, "未核验"));
        }
    }

    /**
     * 清空记录（仅清空记录列表，不清空核验人数统计）
     */
    public void clearRecords() {
        records.clear();
        // 不清空verifiedBibNumbers，让核验人数继续累计
    }

    /**
     * 清空核验统计（包括内存统计、UI显示、数据库记录）
     * 注意：这是一个破坏性操作，应配合二次确认使用
     */
    public void clearVerificationStatistics() {
        if (currentRaceId == null) {
            logger.warn("未选择赛事，无法清空核验统计");
            return;
        }

        try {
            // 1. 删除数据库记录（先执行，失败则不继续）
            int deletedCount = databaseService.deleteVerificationRecordsByRaceId(currentRaceId);

            // 2. 清空内存统计
            verifiedBibNumbers.clear();
            verifiedChipIds.clear();

            // 3. 清空UI显示
            records.clear();

            logger.info("清空赛事 {} 的核验统计成功：删除 {} 条记录，核验人数归0",
                        currentRaceId, deletedCount);

        } catch (SQLException e) {
            logger.error("清空核验统计失败", e);
            throw new RuntimeException("清空核验统计失败：" + e.getMessage(), e);
        }
    }
}
