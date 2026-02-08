package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AthleteService测试类
 */
public class AthleteServiceTest {

    private DatabaseService databaseService;
    private AthleteService athleteService;
    private Long testRaceId;

    @BeforeEach
    public void setUp() {
        // 使用内存数据库进行测试（构造函数会自动初始化）
        databaseService = new DatabaseService(":memory:");
        athleteService = new AthleteService(databaseService);

        // 创建测试赛事
        RaceService raceService = new RaceService(databaseService);
        testRaceId = raceService.createRace("测试赛事").getId();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseService != null) {
            databaseService.close();
        }
    }

    @Test
    public void testCreateAthlete() {
        Athlete athlete = new Athlete();
        athlete.setRaceId(testRaceId);
        athlete.setBibNumber("A001");
        athlete.setName("张三");
        athlete.setChip1("CHIP001");
        athlete.setChip2("CHIP002");
        athlete.setChip3("CHIP003");
        athlete.setChip4("CHIP004");

        Athlete created = athleteService.createAthlete(athlete);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("A001", created.getBibNumber());
        assertEquals("张三", created.getName());
        assertEquals("CHIP001", created.getChip1());
    }

    @Test
    public void testFindAthleteByChip_WithChip1() {
        // 创建测试选手
        Athlete athlete = new Athlete();
        athlete.setRaceId(testRaceId);
        athlete.setBibNumber("A001");
        athlete.setName("张三");
        athlete.setChip1("CHIP001");
        athleteService.createAthlete(athlete);

        // 通过chip1查找
        Athlete found = athleteService.findAthleteByChip(testRaceId, "CHIP001");
        assertNotNull(found);
        assertEquals("A001", found.getBibNumber());
        assertEquals("张三", found.getName());
    }

    @Test
    public void testFindAthleteByChip_WithChip2() {
        Athlete athlete = new Athlete();
        athlete.setRaceId(testRaceId);
        athlete.setBibNumber("A002");
        athlete.setName("李四");
        athlete.setChip1("CHIP001");
        athlete.setChip2("CHIP002");
        athleteService.createAthlete(athlete);

        // 通过chip2查找
        Athlete found = athleteService.findAthleteByChip(testRaceId, "CHIP002");
        assertNotNull(found);
        assertEquals("A002", found.getBibNumber());
    }

    @Test
    public void testFindAthleteByChip_WithChip3() {
        Athlete athlete = new Athlete();
        athlete.setRaceId(testRaceId);
        athlete.setBibNumber("A003");
        athlete.setName("王五");
        athlete.setChip1("CHIP001");
        athlete.setChip2("CHIP002");
        athlete.setChip3("CHIP003");
        athleteService.createAthlete(athlete);

        // 通过chip3查找
        Athlete found = athleteService.findAthleteByChip(testRaceId, "CHIP003");
        assertNotNull(found);
        assertEquals("A003", found.getBibNumber());
    }

    @Test
    public void testFindAthleteByChip_WithChip4() {
        Athlete athlete = new Athlete();
        athlete.setRaceId(testRaceId);
        athlete.setBibNumber("A004");
        athlete.setName("赵六");
        athlete.setChip1("CHIP001");
        athlete.setChip2("CHIP002");
        athlete.setChip3("CHIP003");
        athlete.setChip4("CHIP004");
        athleteService.createAthlete(athlete);

        // 通过chip4查找
        Athlete found = athleteService.findAthleteByChip(testRaceId, "CHIP004");
        assertNotNull(found);
        assertEquals("A004", found.getBibNumber());
    }

    @Test
    public void testFindAthleteByChip_NotFound() {
        Athlete found = athleteService.findAthleteByChip(testRaceId, "NONEXISTENT");
        assertNull(found);
    }

    @Test
    public void testUpdateAthlete() {
        // 创建选手
        Athlete athlete = new Athlete();
        athlete.setRaceId(testRaceId);
        athlete.setBibNumber("A001");
        athlete.setName("张三");
        athlete.setChip1("CHIP001");
        Athlete created = athleteService.createAthlete(athlete);

        // 更新选手
        created.setName("张三三");
        created.setBibNumber("A002");
        created.setChip2("CHIP002");
        athleteService.updateAthlete(created);

        // 验证更新
        Athlete found = athleteService.findAthleteByChip(testRaceId, "CHIP001");
        assertEquals("张三三", found.getName());
        assertEquals("A002", found.getBibNumber());
        assertEquals("CHIP002", found.getChip2());
    }

    @Test
    public void testDeleteAthlete() {
        // 创建选手
        Athlete athlete = new Athlete();
        athlete.setRaceId(testRaceId);
        athlete.setBibNumber("A001");
        athlete.setName("张三");
        athlete.setChip1("CHIP001");
        Athlete created = athleteService.createAthlete(athlete);

        // 删除选手
        athleteService.deleteAthlete(created.getId());

        // 验证已删除
        Athlete found = athleteService.findAthleteByChip(testRaceId, "CHIP001");
        assertNull(found);
    }

    @Test
    public void testGetAthletesByRaceId() {
        // 创建多个选手
        for (int i = 1; i <= 3; i++) {
            Athlete athlete = new Athlete();
            athlete.setRaceId(testRaceId);
            athlete.setBibNumber("A00" + i);
            athlete.setName("选手" + i);
            athlete.setChip1("CHIP00" + i);
            athleteService.createAthlete(athlete);
        }

        List<Athlete> athletes = athleteService.getAthletesByRaceId(testRaceId);
        assertEquals(3, athletes.size());
    }

    @Test
    public void testGetAthleteCountByRaceId() {
        // 创建多个选手
        for (int i = 1; i <= 5; i++) {
            Athlete athlete = new Athlete();
            athlete.setRaceId(testRaceId);
            athlete.setBibNumber("A00" + i);
            athlete.setName("选手" + i);
            athlete.setChip1("CHIP00" + i);
            athleteService.createAthlete(athlete);
        }

        int count = athleteService.getAthleteCountByRaceId(testRaceId);
        assertEquals(5, count);
    }

    @Test
    public void testGetAthleteCountByRaceId_EmptyRace() {
        int count = athleteService.getAthleteCountByRaceId(testRaceId);
        assertEquals(0, count);
    }
}
