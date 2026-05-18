package com.race.chipcheck.service;

import com.race.chipcheck.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite数据库服务
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final String DB_URL = "jdbc:sqlite:" + AppConfig.getDatabasePath();

    private Connection connection;
    private static DatabaseService instance;
    private final String dbUrl;

    private DatabaseService() {
        this.dbUrl = DB_URL;
        initializeDatabase();
    }

    /**
     * 测试专用构造函数（包可见性）
     * @param dbUrl 数据库URL（如":memory:"用于内存数据库测试）
     */
    DatabaseService(String dbUrl) {
        this.dbUrl = "jdbc:sqlite:" + dbUrl;
        initializeDatabase();
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * 初始化数据库连接和表结构
     */
    private void initializeDatabase() {
        try {
            // 首次启动迁移检查
            performMigrationIfNeeded();

            // 加载SQLite JDBC驱动
            Class.forName("org.sqlite.JDBC");

            // 建立连接
            connection = DriverManager.getConnection(dbUrl);
            logger.info("数据库连接成功：{}", dbUrl);

            // 性能优化：WAL模式允许并发读写，NORMAL同步在保证基本安全的同时提升写入速度
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
            }

            // 创建表结构
            createTables();

        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC驱动未找到", e);
            throw new RuntimeException("SQLite JDBC驱动未找到", e);
        } catch (SQLException e) {
            logger.error("数据库连接失败", e);
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 创建赛事表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS races (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 创建选手表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS athletes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    race_id INTEGER NOT NULL,
                    bib_number TEXT NOT NULL,
                    name TEXT,
                    chip1 TEXT NOT NULL,
                    chip2 TEXT,
                    chip3 TEXT,
                    chip4 TEXT,
                    FOREIGN KEY (race_id) REFERENCES races(id) ON DELETE CASCADE,
                    UNIQUE(race_id, bib_number)
                )
            """);

            // 创建核验记录表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS verification_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    race_id INTEGER NOT NULL,
                    verification_time DATETIME NOT NULL,
                    chip_id TEXT NOT NULL,
                    athlete_id INTEGER,
                    bib_number TEXT,
                    name TEXT,
                    status TEXT NOT NULL,
                    remark TEXT,
                    FOREIGN KEY (race_id) REFERENCES races(id) ON DELETE CASCADE,
                    FOREIGN KEY (athlete_id) REFERENCES athletes(id) ON DELETE SET NULL
                )
            """);

            // 创建配置表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL,
                    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 插入默认配置
            stmt.execute("""
                INSERT OR IGNORE INTO app_settings (key, value) VALUES
                    ('voice_enabled', 'true'),
                    ('voice_content', 'BIB_NUMBER'),
                    ('alert_sound_enabled', 'true')
            """);

            // 创建索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_athletes_race_id ON athletes(race_id)");
            // 拆分芯片索引：复合索引对 OR 查询无效，改为单列索引让 SQLite 为每个 OR 分支选择对应索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_athletes_chip1 ON athletes(race_id, chip1)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_athletes_chip2 ON athletes(race_id, chip2)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_athletes_chip3 ON athletes(race_id, chip3)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_athletes_chip4 ON athletes(race_id, chip4)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_verification_race_id ON verification_records(race_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_verification_time ON verification_records(verification_time)");

            logger.info("数据库表创建成功");
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(dbUrl);
            }
        } catch (SQLException e) {
            logger.error("获取数据库连接失败", e);
            throw new RuntimeException("获取数据库连接失败", e);
        }
        return connection;
    }

    /**
     * 删除指定赛事的所有核验记录
     * @param raceId 赛事ID
     * @return 删除的记录数
     */
    public int deleteVerificationRecordsByRaceId(Long raceId) throws SQLException {
        String sql = "DELETE FROM verification_records WHERE race_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setLong(1, raceId);
            int deletedCount = stmt.executeUpdate();
            logger.info("删除赛事 {} 的核验记录：{} 条", raceId, deletedCount);
            return deletedCount;
        }
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("数据库连接已关闭");
            }
        } catch (SQLException e) {
            logger.error("关闭数据库连接失败", e);
        }
    }

    /**
     * 迁移旧数据库到新位置（如果需要）
     * 仅在首次启动时执行，检测旧位置的数据库并复制到用户目录
     */
    private void performMigrationIfNeeded() {
        Path newDbPath = Paths.get(AppConfig.getDatabasePath());

        // 如果新位置已有数据库，跳过迁移
        if (Files.exists(newDbPath)) {
            logger.info("数据库已存在于用户目录：{}", newDbPath);
            return;
        }

        // 检查旧位置是否有数据库
        Path oldDbPath = Paths.get("race-chipcheck.db");
        if (!Files.exists(oldDbPath)) {
            logger.info("首次启动，将在用户目录创建新数据库");
            return;
        }

        // 执行迁移
        try {
            logger.info("检测到旧数据库，开始迁移...");
            logger.info("源文件：{}", oldDbPath.toAbsolutePath());
            logger.info("目标文件：{}", newDbPath.toAbsolutePath());

            // 确保目标目录存在
            Files.createDirectories(newDbPath.getParent());

            // 复制文件（而非移动，保留备份）
            Files.copy(oldDbPath, newDbPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("数据库迁移成功！");
            logger.info("提示：原数据库文件保留在安装目录，可手动删除");

        } catch (IOException e) {
            logger.error("数据库迁移失败", e);
            throw new RuntimeException("数据库迁移失败：" + e.getMessage(), e);
        }
    }
}
