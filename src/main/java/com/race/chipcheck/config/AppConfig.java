package com.race.chipcheck.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 应用配置类
 * 管理用户数据目录路径，确保数据存储在用户主目录下，卸载应用时不会丢失数据
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static final String APP_DIR_NAME = ".RaceChipCheckOffline";
    private static final String DB_FILENAME = "race-chipcheck.db";

    private static final String USER_DATA_DIR;
    private static final String DB_PATH;
    private static final String LOGS_DIR;

    static {
        // 1. 优先使用系统属性（测试用）
        String baseDir = System.getProperty("app.data.dir");

        // 2. 否则使用用户主目录
        if (baseDir == null) {
            String userHome = System.getProperty("user.home");
            baseDir = userHome + File.separator + APP_DIR_NAME;
        }

        USER_DATA_DIR = baseDir;
        DB_PATH = USER_DATA_DIR + File.separator + "data" + File.separator + DB_FILENAME;
        LOGS_DIR = USER_DATA_DIR + File.separator + "logs";

        // 3. 创建目录并设置系统属性供logback使用
        initDirectories();
        System.setProperty("app.logs.dir", LOGS_DIR);

        logger.info("应用数据目录初始化完成：{}", USER_DATA_DIR);
    }

    /**
     * 初始化应用数据目录结构
     */
    private static void initDirectories() {
        try {
            Files.createDirectories(Paths.get(USER_DATA_DIR, "data"));
            Files.createDirectories(Paths.get(LOGS_DIR));
        } catch (IOException e) {
            throw new RuntimeException("无法创建应用数据目录: " + USER_DATA_DIR, e);
        }
    }

    /**
     * 获取数据库文件路径
     * @return 数据库文件的绝对路径
     */
    public static String getDatabasePath() {
        return DB_PATH;
    }

    /**
     * 获取日志目录路径
     * @return 日志目录的绝对路径
     */
    public static String getLogsDirectory() {
        return LOGS_DIR;
    }

    /**
     * 获取用户数据目录路径
     * @return 用户数据目录的绝对路径
     */
    public static String getUserDataDirectory() {
        return USER_DATA_DIR;
    }
}
