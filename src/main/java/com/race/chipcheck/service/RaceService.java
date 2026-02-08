package com.race.chipcheck.service;

import com.race.chipcheck.model.Race;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 赛事管理服务
 */
public class RaceService {
    private static final Logger logger = LoggerFactory.getLogger(RaceService.class);
    private final DatabaseService databaseService;

    public RaceService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * 创建赛事
     */
    public Race createRace(String name) {
        String insertSql = "INSERT INTO races (name) VALUES (?)";
        String selectIdSql = "SELECT last_insert_rowid() as id";

        try (PreparedStatement insertStmt = databaseService.getConnection().prepareStatement(insertSql)) {
            insertStmt.setString(1, name);
            insertStmt.executeUpdate();

            // 使用SQLite的last_insert_rowid()获取刚插入的ID
            try (Statement selectStmt = databaseService.getConnection().createStatement();
                 ResultSet rs = selectStmt.executeQuery(selectIdSql)) {

                if (rs.next()) {
                    Long id = rs.getLong("id");
                    logger.info("创建赛事成功：{} (ID: {})", name, id);

                    // 查询完整的记录（包括created_time）
                    Race race = getRaceById(id);
                    if (race != null) {
                        return race;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("创建赛事失败：{}", name, e);
            throw new RuntimeException("创建赛事失败：" + e.getMessage(), e);
        }

        throw new RuntimeException("创建赛事失败：未能获取生成的ID");
    }

    /**
     * 获取所有赛事
     */
    public List<Race> getAllRaces() {
        List<Race> races = new ArrayList<>();
        String sql = "SELECT id, name, created_time FROM races ORDER BY created_time DESC";

        try (Statement stmt = databaseService.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                races.add(mapResultSetToRace(rs));
            }

            logger.info("查询到 {} 个赛事", races.size());
        } catch (SQLException e) {
            logger.error("查询赛事列表失败", e);
        }

        return races;
    }

    /**
     * 根据ID获取赛事
     */
    public Race getRaceById(Long id) {
        String sql = "SELECT id, name, created_time FROM races WHERE id = ?";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRace(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("查询赛事失败：ID={}", id, e);
        }

        return null;
    }

    /**
     * 更新赛事
     */
    public void updateRace(Race race) {
        String sql = "UPDATE races SET name = ? WHERE id = ?";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setString(1, race.getName());
            stmt.setLong(2, race.getId());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("更新赛事成功：{} (ID: {})", race.getName(), race.getId());
            } else {
                logger.warn("更新赛事失败：赛事不存在 (ID: {})", race.getId());
            }
        } catch (SQLException e) {
            logger.error("更新赛事失败：ID={}", race.getId(), e);
            throw new RuntimeException("更新赛事失败", e);
        }
    }

    /**
     * 删除赛事（级联删除相关的选手和核验记录）
     */
    public void deleteRace(Long raceId) {
        String sql = "DELETE FROM races WHERE id = ?";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, raceId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("删除赛事成功：ID={}", raceId);
            } else {
                logger.warn("删除赛事失败：赛事不存在 (ID: {})", raceId);
            }
        } catch (SQLException e) {
            logger.error("删除赛事失败：ID={}", raceId, e);
            throw new RuntimeException("删除赛事失败", e);
        }
    }

    /**
     * 将ResultSet映射为Race对象
     */
    private Race mapResultSetToRace(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        String createdTimeStr = rs.getString("created_time");

        // SQLite的CURRENT_TIMESTAMP返回格式: "YYYY-MM-DD HH:MM:SS"
        LocalDateTime createdTime;
        try {
            if (createdTimeStr.contains(" ")) {
                // SQLite CURRENT_TIMESTAMP格式
                createdTime = LocalDateTime.parse(createdTimeStr.replace(" ", "T"));
            } else {
                // ISO格式
                createdTime = LocalDateTime.parse(createdTimeStr);
            }
        } catch (Exception e) {
            logger.error("解析时间失败：{}", createdTimeStr, e);
            createdTime = LocalDateTime.now();
        }

        return new Race(id, name, createdTime);
    }
}
