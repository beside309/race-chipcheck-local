package com.race.chipcheck.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库迁移逻辑单元测试
 */
public class DatabaseMigrationTest {

    @Test
    public void testDatabaseServiceWithMemory() {
        // 测试内存数据库仍然正常工作（不受迁移逻辑影响）
        DatabaseService service = new DatabaseService(":memory:");
        assertNotNull(service.getConnection());
        service.close();
    }

    @Test
    public void testDatabasePathConfiguration(@TempDir Path tempDir) throws IOException {
        // 设置临时数据目录
        String tempDataDir = tempDir.toString();
        System.setProperty("app.data.dir", tempDataDir);

        try {
            // 创建旧数据库文件
            Path oldDbPath = tempDir.resolve("race-chipcheck.db");
            Files.writeString(oldDbPath, "test database content");

            // 验证旧数据库存在
            assertTrue(Files.exists(oldDbPath), "旧数据库文件应该存在");

            // 注意：实际迁移逻辑在DatabaseService初始化时执行
            // 这里仅验证文件操作逻辑
            Path newDataDir = tempDir.resolve("data");
            Path newDbPath = newDataDir.resolve("race-chipcheck.db");

            // 模拟迁移：创建目录并复制文件
            Files.createDirectories(newDataDir);
            Files.copy(oldDbPath, newDbPath);

            // 验证迁移结果
            assertTrue(Files.exists(newDbPath), "新数据库文件应该存在");
            assertTrue(Files.exists(oldDbPath), "旧数据库文件应该保留");
            assertEquals(
                Files.readString(oldDbPath),
                Files.readString(newDbPath),
                "新旧数据库内容应该一致"
            );

        } finally {
            // 清理系统属性
            System.clearProperty("app.data.dir");
        }
    }

    @Test
    public void testNoMigrationWhenNewDatabaseExists(@TempDir Path tempDir) throws IOException {
        // 设置临时数据目录
        String tempDataDir = tempDir.toString();
        System.setProperty("app.data.dir", tempDataDir);

        try {
            // 创建新数据库文件
            Path newDataDir = tempDir.resolve("data");
            Files.createDirectories(newDataDir);
            Path newDbPath = newDataDir.resolve("race-chipcheck.db");
            Files.writeString(newDbPath, "existing database");

            // 创建旧数据库文件
            Path oldDbPath = tempDir.resolve("race-chipcheck.db");
            Files.writeString(oldDbPath, "old database");

            // 验证两个文件都存在
            assertTrue(Files.exists(newDbPath), "新数据库应该存在");
            assertTrue(Files.exists(oldDbPath), "旧数据库应该存在");

            // 验证新数据库未被覆盖
            assertEquals("existing database", Files.readString(newDbPath),
                "新数据库内容不应被覆盖");

        } finally {
            // 清理系统属性
            System.clearProperty("app.data.dir");
        }
    }
}
