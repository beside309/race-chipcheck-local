package com.race.chipcheck.service;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseService测试类
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        // 使用内存数据库进行测试（构造函数会自动初始化）
        databaseService = new DatabaseService(":memory:");
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseService != null) {
            databaseService.close();
        }
    }

    @Test
    public void testInitialize() throws SQLException {
        // 验证表已创建
        Connection conn = databaseService.getConnection();
        DatabaseMetaData metaData = conn.getMetaData();

        // 检查races表
        ResultSet rs1 = metaData.getTables(null, null, "races", null);
        assertTrue(rs1.next(), "races表应该存在");
        rs1.close();

        // 检查athletes表
        ResultSet rs2 = metaData.getTables(null, null, "athletes", null);
        assertTrue(rs2.next(), "athletes表应该存在");
        rs2.close();

        // 检查verification_records表
        ResultSet rs3 = metaData.getTables(null, null, "verification_records", null);
        assertTrue(rs3.next(), "verification_records表应该存在");
        rs3.close();
    }

    @Test
    public void testGetConnection() throws SQLException {
        Connection conn = databaseService.getConnection();

        assertNotNull(conn);
        assertFalse(conn.isClosed());
    }

    @Test
    public void testClose() throws SQLException {
        Connection conn = databaseService.getConnection();

        databaseService.close();
        assertTrue(conn.isClosed());
    }

    @Test
    public void testForeignKeyConstraint() throws SQLException {
        // 插入赛事
        String insertRaceSql = "INSERT INTO races (name, created_time) VALUES ('测试赛事', datetime('now'))";
        try (Statement stmt = databaseService.getConnection().createStatement()) {
            stmt.executeUpdate(insertRaceSql);
        }

        // 获取赛事ID
        String selectSql = "SELECT last_insert_rowid() as id";
        long raceId;
        try (Statement stmt = databaseService.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {
            assertTrue(rs.next());
            raceId = rs.getLong("id");
        }

        // 插入选手
        String insertAthleteSql = "INSERT INTO athletes (race_id, bib_number, name, chip1) VALUES (?, 'A001', '张三', 'CHIP001')";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(insertAthleteSql)) {
            stmt.setLong(1, raceId);
            stmt.executeUpdate();
        }

        // 删除赛事（级联删除选手）
        String deleteRaceSql = "DELETE FROM races WHERE id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(deleteRaceSql)) {
            stmt.setLong(1, raceId);
            stmt.executeUpdate();
        }

        // 验证选手也被删除
        String countSql = "SELECT COUNT(*) FROM athletes WHERE race_id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countSql)) {
            stmt.setLong(1, raceId);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    public void testDeleteVerificationRecordsByRaceId() throws SQLException {
        // 插入测试赛事
        String insertRaceSql = "INSERT INTO races (name, created_time) VALUES ('测试赛事1', datetime('now'))";
        try (Statement stmt = databaseService.getConnection().createStatement()) {
            stmt.executeUpdate(insertRaceSql);
        }

        // 获取赛事ID
        long raceId;
        try (Statement stmt = databaseService.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid() as id")) {
            assertTrue(rs.next());
            raceId = rs.getLong("id");
        }

        // 插入测试核验记录
        String insertRecordSql = "INSERT INTO verification_records (race_id, verification_time, chip_id, bib_number, name, status, remark) " +
                                 "VALUES (?, datetime('now'), ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(insertRecordSql)) {
            // 插入3条记录
            for (int i = 1; i <= 3; i++) {
                stmt.setLong(1, raceId);
                stmt.setString(2, "CHIP00" + i);
                stmt.setString(3, "A00" + i);
                stmt.setString(4, "选手" + i);
                stmt.setString(5, "成功");
                stmt.setString(6, "");
                stmt.executeUpdate();
            }
        }

        // 验证记录已插入
        String countBeforeSql = "SELECT COUNT(*) FROM verification_records WHERE race_id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countBeforeSql)) {
            stmt.setLong(1, raceId);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1), "应该有3条核验记录");
        }

        // 删除核验记录
        int deletedCount = databaseService.deleteVerificationRecordsByRaceId(raceId);
        assertEquals(3, deletedCount, "应该删除3条记录");

        // 验证记录已删除
        String countAfterSql = "SELECT COUNT(*) FROM verification_records WHERE race_id = ?";
        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(countAfterSql)) {
            stmt.setLong(1, raceId);
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "所有核验记录应该被删除");
        }
    }
}
