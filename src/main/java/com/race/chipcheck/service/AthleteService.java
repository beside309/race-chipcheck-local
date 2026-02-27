package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;


/**
 * 选手管理服务（支持多芯片查询）
 */
public class AthleteService {
    private static final Logger logger = LoggerFactory.getLogger(AthleteService.class);
    private final DatabaseService databaseService;

    public AthleteService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * 根据芯片号查找选手（芯片1-4任意匹配）
     */
    public Athlete findAthleteByChip(Long raceId, String chipId) {
        String sql = "SELECT * FROM athletes WHERE race_id = ? AND (chip1 = ? OR chip2 = ? OR chip3 = ? OR chip4 = ?)";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, raceId);
            stmt.setString(2, chipId);
            stmt.setString(3, chipId);
            stmt.setString(4, chipId);
            stmt.setString(5, chipId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Athlete athlete = mapResultSetToAthlete(rs);
                    logger.debug("找到选手：芯片={}, 参赛号={}", chipId, athlete.getBibNumber());
                    return athlete;
                }
            }
        } catch (SQLException e) {
            logger.error("查询选手失败：芯片={}", chipId, e);
        }

        logger.debug("未找到选手：芯片={}", chipId);
        return null;
    }

    /**
     * 创建选手
     */
    public Athlete createAthlete(Athlete athlete) {
        String insertSql = "INSERT INTO athletes (race_id, bib_number, name, chip1, chip2, chip3, chip4) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String selectIdSql = "SELECT last_insert_rowid() as id";

        try (PreparedStatement insertStmt = databaseService.getConnection().prepareStatement(insertSql)) {
            insertStmt.setLong(1, athlete.getRaceId());
            insertStmt.setString(2, athlete.getBibNumber());
            insertStmt.setString(3, athlete.getName());
            insertStmt.setString(4, athlete.getChip1());
            insertStmt.setString(5, athlete.getChip2());
            insertStmt.setString(6, athlete.getChip3());
            insertStmt.setString(7, athlete.getChip4());

            insertStmt.executeUpdate();

            // 使用SQLite的last_insert_rowid()获取刚插入的ID
            try (Statement selectStmt = databaseService.getConnection().createStatement();
                 ResultSet rs = selectStmt.executeQuery(selectIdSql)) {

                if (rs.next()) {
                    Long id = rs.getLong("id");
                    athlete.setId(id);
                    logger.info("创建选手成功：参赛号={}, ID={}", athlete.getBibNumber(), id);
                    return athlete;
                }
            }
        } catch (SQLException e) {
            logger.error("创建选手失败：参赛号={}", athlete.getBibNumber(), e);
            throw new RuntimeException("创建选手失败：" + e.getMessage(), e);
        }

        throw new RuntimeException("创建选手失败：未能获取生成的ID");
    }

    /**
     * 更新选手
     */
    public void updateAthlete(Athlete athlete) {
        String sql = "UPDATE athletes SET bib_number = ?, name = ?, chip1 = ?, chip2 = ?, chip3 = ?, chip4 = ? WHERE id = ?";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setString(1, athlete.getBibNumber());
            stmt.setString(2, athlete.getName());
            stmt.setString(3, athlete.getChip1());
            stmt.setString(4, athlete.getChip2());
            stmt.setString(5, athlete.getChip3());
            stmt.setString(6, athlete.getChip4());
            stmt.setLong(7, athlete.getId());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("更新选手成功：参赛号={}, ID={}", athlete.getBibNumber(), athlete.getId());
            } else {
                logger.warn("更新选手失败：选手不存在 (ID: {})", athlete.getId());
            }
        } catch (SQLException e) {
            logger.error("更新选手失败：ID={}", athlete.getId(), e);
            throw new RuntimeException("更新选手失败", e);
        }
    }

    /**
     * 删除选手
     */
    public void deleteAthlete(Long athleteId) {
        String sql = "DELETE FROM athletes WHERE id = ?";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, athleteId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("删除选手成功：ID={}", athleteId);
            } else {
                logger.warn("删除选手失败：选手不存在 (ID: {})", athleteId);
            }
        } catch (SQLException e) {
            logger.error("删除选手失败：ID={}", athleteId, e);
            throw new RuntimeException("删除选手失败", e);
        }
    }

    /**
     * 获取指定赛事的所有选手
     */
    public List<Athlete> getAthletesByRaceId(Long raceId) {
        List<Athlete> athletes = new ArrayList<>();
        String sql = "SELECT * FROM athletes WHERE race_id = ? ORDER BY bib_number";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, raceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    athletes.add(mapResultSetToAthlete(rs));
                }
            }

            logger.info("查询到 {} 个选手 (赛事ID: {})", athletes.size(), raceId);
        } catch (SQLException e) {
            logger.error("查询选手列表失败：赛事ID={}", raceId, e);
        }

        return athletes;
    }

    /**
     * 获取指定赛事的选手数量
     */
    public int getAthleteCountByRaceId(Long raceId) {
        String sql = "SELECT COUNT(*) FROM athletes WHERE race_id = ?";

        try (PreparedStatement stmt = databaseService.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, raceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("查询选手数量失败：赛事ID={}", raceId, e);
        }

        return 0;
    }

    /**
     * 校验选手芯片号（用于编辑时检查重复）
     * 检查：芯片1不能为空、同选手内芯片不重复、芯片未被其他选手使用
     *
     * @param raceId  赛事ID
     * @param athlete 当前编辑的选手
     * @return 有错误时返回错误信息，否则返回 null
     */
    public String validateChipsForUpdate(Long raceId, Athlete athlete) {
        // 芯片1不能为空
        if (athlete.getChip1() == null || athlete.getChip1().trim().isEmpty()) {
            return "芯片1不能为空";
        }

        List<String> chips = Arrays.asList(
            athlete.getChip1(), athlete.getChip2(),
            athlete.getChip3(), athlete.getChip4()
        );
        Set<String> seen = new HashSet<>();

        for (String chip : chips) {
            if (chip == null || chip.trim().isEmpty()) {
                continue;
            }
            chip = chip.trim();

            // 同选手内芯片不能重复
            if (!seen.add(chip)) {
                return "芯片号重复：" + chip;
            }

            // 芯片不能被其他选手使用
            Athlete other = findAthleteByChip(raceId, chip);
            if (other != null && !other.getId().equals(athlete.getId())) {
                return "芯片号重复：" + chip + " 已被选手 " + other.getBibNumber() + " 使用";
            }
        }
        return null;
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
