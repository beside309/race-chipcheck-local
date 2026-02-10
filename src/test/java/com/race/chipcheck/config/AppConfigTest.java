package com.race.chipcheck.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppConfig单元测试
 */
public class AppConfigTest {

    @Test
    public void testDefaultPaths() {
        String dbPath = AppConfig.getDatabasePath();
        String logsDir = AppConfig.getLogsDirectory();

        assertNotNull(dbPath);
        assertNotNull(logsDir);
        assertTrue(dbPath.contains(".RaceChipCheckOffline"),
            "数据库路径应包含.RaceChipCheckOffline，实际路径：" + dbPath);
        assertTrue(dbPath.endsWith("race-chipcheck.db"),
            "数据库路径应以race-chipcheck.db结尾，实际路径：" + dbPath);
        assertTrue(logsDir.contains("logs"),
            "日志目录应包含logs，实际路径：" + logsDir);
    }

    @Test
    public void testDirectoriesCreated() {
        Path dataDir = Paths.get(AppConfig.getUserDataDirectory(), "data");
        Path logsDir = Paths.get(AppConfig.getLogsDirectory());

        assertTrue(Files.exists(dataDir), "data目录应该存在：" + dataDir);
        assertTrue(Files.isDirectory(dataDir), "data应该是目录：" + dataDir);
        assertTrue(Files.exists(logsDir), "logs目录应该存在：" + logsDir);
        assertTrue(Files.isDirectory(logsDir), "logs应该是目录：" + logsDir);
    }

    @Test
    public void testUserDataDirectory() {
        String userDataDir = AppConfig.getUserDataDirectory();

        assertNotNull(userDataDir);
        assertTrue(userDataDir.contains(".RaceChipCheckOffline"),
            "用户数据目录应包含.RaceChipCheckOffline，实际路径：" + userDataDir);
    }

    @Test
    public void testPathsAreAbsolute() {
        String dbPath = AppConfig.getDatabasePath();
        String logsDir = AppConfig.getLogsDirectory();
        String userDataDir = AppConfig.getUserDataDirectory();

        // 检查路径是否为绝对路径
        assertTrue(Paths.get(dbPath).isAbsolute(), "数据库路径应为绝对路径");
        assertTrue(Paths.get(logsDir).isAbsolute(), "日志目录路径应为绝对路径");
        assertTrue(Paths.get(userDataDir).isAbsolute(), "用户数据目录路径应为绝对路径");
    }
}
