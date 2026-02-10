package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import com.race.chipcheck.model.VerificationRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VerificationService测试类
 */
public class VerificationServiceTest {

    private DatabaseService databaseService;
    private AthleteService athleteService;
    private PreferenceService preferenceService;
    private AlertSoundService alertSoundService;
    private TTSService ttsService;
    private VerificationService verificationService;
    private ObservableList<VerificationRecord> records;
    private Long testRaceId;

    @BeforeEach
    public void setUp() throws SQLException {
        // 使用内存数据库进行测试
        databaseService = new DatabaseService(":memory:");
        athleteService = new AthleteService(databaseService);
        preferenceService = new PreferenceService(databaseService);
        alertSoundService = new AlertSoundService(preferenceService);
        ttsService = new TTSService(preferenceService);
        records = FXCollections.observableArrayList();

        // 创建 VerificationService
        verificationService = new VerificationService(
            athleteService,
            databaseService,
            alertSoundService,
            ttsService,
            preferenceService,
            records
        );

        // 创建测试赛事
        String insertRaceSql = "INSERT INTO races (name, created_time) VALUES ('测试赛事', datetime('now'))";
        try (Statement stmt = databaseService.getConnection().createStatement()) {
            stmt.executeUpdate(insertRaceSql);
        }

        // 获取赛事ID
        try (Statement stmt = databaseService.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid() as id")) {
            assertTrue(rs.next());
            testRaceId = rs.getLong("id");
        }

        // 设置当前赛事
        verificationService.setCurrentRaceId(testRaceId);
    }

    @AfterEach
    public void tearDown() {
        if (databaseService != null) {
            databaseService.close();
        }
    }

    @Test
    public void testClearVerificationStatistics() throws SQLException {
        // 插入测试选手
        insertTestAthletes();

        // 插入测试核验记录
        insertTestVerificationRecords();

        // 验证初始状态
        assertEquals(3, verificationService.getVerifiedAthleteCount(), "应该有3个已核验选手");

        String countSql = "SELECT COUNT(*) FROM verification_records WHERE race_id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countSql)) {
            stmt.setLong(1, testRaceId);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1), "数据库应该有3条核验记录");
        }

        // 清空核验统计
        verificationService.clearVerificationStatistics();

        // 验证核验人数归0
        assertEquals(0, verificationService.getVerifiedAthleteCount(), "核验人数应该归0");

        // 验证数据库记录已删除
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countSql)) {
            stmt.setLong(1, testRaceId);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "数据库记录应该被删除");
        }

        // 验证UI显示记录已清空
        assertEquals(0, records.size(), "UI记录列表应该为空");
    }

    @Test
    public void testClearStatisticsDoesNotAffectOtherRaces() throws SQLException {
        // 创建第二个测试赛事
        String insertRace2Sql = "INSERT INTO races (name, created_time) VALUES ('测试赛事2', datetime('now'))";
        try (Statement stmt = databaseService.getConnection().createStatement()) {
            stmt.executeUpdate(insertRace2Sql);
        }

        long testRaceId2;
        try (Statement stmt = databaseService.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid() as id")) {
            assertTrue(rs.next());
            testRaceId2 = rs.getLong("id");
        }

        // 为两个赛事插入核验记录
        insertTestVerificationRecords();  // 赛事1

        String insertRecord2Sql = "INSERT INTO verification_records (race_id, verification_time, chip_id, bib_number, name, status, remark) " +
                                  "VALUES (?, datetime('now'), 'CHIP201', 'B001', '选手B1', '成功', '')";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(insertRecord2Sql)) {
            stmt.setLong(1, testRaceId2);
            stmt.executeUpdate();
        }

        // 清空赛事1的核验统计
        verificationService.clearVerificationStatistics();

        // 验证赛事1的记录已删除
        String countSql = "SELECT COUNT(*) FROM verification_records WHERE race_id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countSql)) {
            stmt.setLong(1, testRaceId);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "赛事1的记录应该被删除");
        }

        // 验证赛事2的记录未受影响
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countSql)) {
            stmt.setLong(1, testRaceId2);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "赛事2的记录应该保留");
        }
    }

    @Test
    public void testClearStatisticsCannotRestoreFromDatabase() throws SQLException {
        // 插入测试选手和核验记录
        insertTestAthletes();
        insertTestVerificationRecords();

        // 验证初始状态
        assertEquals(3, verificationService.getVerifiedAthleteCount(), "应该有3个已核验选手");

        // 清空核验统计
        verificationService.clearVerificationStatistics();

        // 验证核验人数归0
        assertEquals(0, verificationService.getVerifiedAthleteCount(), "核验人数应该归0");

        // 重新加载赛事（模拟应用重启）
        verificationService.setCurrentRaceId(testRaceId);

        // 验证核验人数仍为0（因为数据库记录已删除，无法恢复）
        assertEquals(0, verificationService.getVerifiedAthleteCount(), "重新加载后核验人数应该仍为0");
    }

    @Test
    public void testClearRecordsDoesNotClearStatistics() throws SQLException {
        // 插入测试选手和核验记录
        insertTestAthletes();
        insertTestVerificationRecords();

        // 验证初始状态
        int initialCount = verificationService.getVerifiedAthleteCount();
        assertEquals(3, initialCount, "应该有3个已核验选手");

        // 清空记录（现有功能，只清空UI显示）
        verificationService.clearRecords();

        // 验证核验人数不变
        assertEquals(initialCount, verificationService.getVerifiedAthleteCount(), "核验人数应该保持不变");

        // 验证UI显示记录已清空
        assertEquals(0, records.size(), "UI记录列表应该为空");

        // 验证数据库记录未删除
        String countSql = "SELECT COUNT(*) FROM verification_records WHERE race_id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countSql)) {
            stmt.setLong(1, testRaceId);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1), "数据库记录应该保留");
        }
    }

    /**
     * 插入测试选手
     */
    private void insertTestAthletes() throws SQLException {
        String insertAthleteSql = "INSERT INTO athletes (race_id, bib_number, name, chip1) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(insertAthleteSql)) {
            for (int i = 1; i <= 3; i++) {
                stmt.setLong(1, testRaceId);
                stmt.setString(2, "A00" + i);
                stmt.setString(3, "选手" + i);
                stmt.setString(4, "CHIP00" + i);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * 插入测试核验记录
     */
    private void insertTestVerificationRecords() throws SQLException {
        String insertRecordSql = "INSERT INTO verification_records (race_id, verification_time, chip_id, bib_number, name, status, remark) " +
                                 "VALUES (?, datetime('now'), ?, ?, ?, '成功', '')";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(insertRecordSql)) {
            for (int i = 1; i <= 3; i++) {
                stmt.setLong(1, testRaceId);
                stmt.setString(2, "CHIP00" + i);
                stmt.setString(3, "A00" + i);
                stmt.setString(4, "选手" + i);
                stmt.executeUpdate();
            }
        }

        // 手动加载已核验人数（模拟应用启动时的行为）
        verificationService.setCurrentRaceId(testRaceId);
    }
}
