package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import com.race.chipcheck.model.ImportValidationResult;
import com.race.chipcheck.util.ExcelReader;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelImportService测试类
 */
public class ExcelImportServiceTest {

    private DatabaseService databaseService;
    private AthleteService athleteService;
    private ExcelImportService excelImportService;
    private RaceService raceService;
    private Long testRaceId;
    private File tempExcelFile;

    @BeforeEach
    public void setUp() {
        // 使用内存数据库进行测试
        databaseService = new DatabaseService(":memory:");
        athleteService = new AthleteService(databaseService);
        excelImportService = new ExcelImportService(athleteService);
        raceService = new RaceService(databaseService);

        // 创建测试赛事
        testRaceId = raceService.createRace("测试赛事").getId();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseService != null) {
            databaseService.close();
        }
        if (tempExcelFile != null && tempExcelFile.exists()) {
            tempExcelFile.delete();
        }
    }

    @Test
    public void testImportAthletes_ValidData() throws Exception {
        // 创建测试Excel文件
        tempExcelFile = createTestExcelFile(new String[][]{
            {"参赛号", "姓名", "芯片1", "芯片2", "芯片3", "芯片4"},
            {"A001", "张三", "CHIP001", "CHIP002", "", ""},
            {"A002", "李四", "CHIP003", "", "", ""},
            {"A003", "王五", "CHIP004", "CHIP005", "CHIP006", ""}
        });

        // 导入
        ImportValidationResult result = excelImportService.importAthletes(tempExcelFile, testRaceId);

        assertTrue(result.isValid());
        assertEquals(3, result.getImportedCount());

        // 验证数据
        assertEquals(3, athleteService.getAthleteCountByRaceId(testRaceId));
        assertNotNull(athleteService.findAthleteByChip(testRaceId, "CHIP001"));
        assertNotNull(athleteService.findAthleteByChip(testRaceId, "CHIP003"));
    }

    @Test
    public void testImportAthletes_DuplicateBibNumber() throws Exception {
        // 创建包含重复参赛号的Excel
        tempExcelFile = createTestExcelFile(new String[][]{
            {"参赛号", "姓名", "芯片1", "芯片2", "芯片3", "芯片4"},
            {"A001", "张三", "CHIP001", "", "", ""},
            {"A001", "李四", "CHIP002", "", "", ""}
        });

        ImportValidationResult result = excelImportService.importAthletes(tempExcelFile, testRaceId);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("参赛号码重复")));
    }

    @Test
    public void testImportAthletes_DuplicateChip() throws Exception {
        // 创建包含重复芯片的Excel
        tempExcelFile = createTestExcelFile(new String[][]{
            {"参赛号", "姓名", "芯片1", "芯片2", "芯片3", "芯片4"},
            {"A001", "张三", "CHIP001", "", "", ""},
            {"A002", "李四", "CHIP001", "", "", ""}
        });

        ImportValidationResult result = excelImportService.importAthletes(tempExcelFile, testRaceId);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("芯片号重复")));
    }

    @Test
    public void testImportAthletes_MissingBibNumber() throws Exception {
        // 创建缺少参赛号的Excel
        tempExcelFile = createTestExcelFile(new String[][]{
            {"参赛号", "姓名", "芯片1", "芯片2", "芯片3", "芯片4"},
            {"", "张三", "CHIP001", "", "", ""}
        });

        ImportValidationResult result = excelImportService.importAthletes(tempExcelFile, testRaceId);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("参赛号码不能为空")));
    }

    @Test
    public void testImportAthletes_MissingChip1() throws Exception {
        // 创建缺少芯片1的Excel
        tempExcelFile = createTestExcelFile(new String[][]{
            {"参赛号", "姓名", "芯片1", "芯片2", "芯片3", "芯片4"},
            {"A001", "张三", "", "", "", ""}
        });

        ImportValidationResult result = excelImportService.importAthletes(tempExcelFile, testRaceId);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("芯片1不能为空")));
    }

    @Test
    public void testImportAthletes_DuplicateWithExistingData() throws Exception {
        // 先导入一个选手
        Athlete existing = new Athlete();
        existing.setRaceId(testRaceId);
        existing.setBibNumber("A001");
        existing.setName("已存在的选手");
        existing.setChip1("CHIP001");
        athleteService.createAthlete(existing);

        // 尝试导入包含重复参赛号的Excel
        tempExcelFile = createTestExcelFile(new String[][]{
            {"参赛号", "姓名", "芯片1", "芯片2", "芯片3", "芯片4"},
            {"A001", "新选手", "CHIP002", "", "", ""}
        });

        ImportValidationResult result = excelImportService.importAthletes(tempExcelFile, testRaceId);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessages().stream().anyMatch(msg -> msg.contains("参赛号码重复")));
    }

    private File createTestExcelFile(String[][] data) throws Exception {
        File file = File.createTempFile("test-athletes-", ".xlsx");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("选手名单");

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(data[i][j]);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();

        return file;
    }
}
