package com.race.chipcheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite数据库服务
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final String DB_URL = "jdbc:sqlite:race-chipcheck.db";

    private Connection connection;
    private static DatabaseService instance;

    private DatabaseService() {
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
            // 加载SQLite JDBC驱动
            Class.forName("org.sqlite.JDBC");

            // 建立连接
            connection = DriverManager.getConnection(DB_URL);
            logger.info("数据库连接成功：{}", DB_URL);

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

            // 创建索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_athletes_race_id ON athletes(race_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_athletes_chips ON athletes(chip1, chip2, chip3, chip4)");
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
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            logger.error("获取数据库连接失败", e);
            throw new RuntimeException("获取数据库连接失败", e);
        }
        return connection;
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
}
