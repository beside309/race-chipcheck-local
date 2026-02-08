package com.race.chipcheck.service;

import com.race.chipcheck.model.Race;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RaceService测试类
 */
public class RaceServiceTest {

    private DatabaseService databaseService;
    private RaceService raceService;

    @BeforeEach
    public void setUp() {
        // 使用内存数据库进行测试（构造函数会自动初始化）
        databaseService = new DatabaseService(":memory:");
        raceService = new RaceService(databaseService);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (databaseService != null) {
            databaseService.close();
        }
    }

    @Test
    public void testCreateRace() {
        Race race = raceService.createRace("测试赛事");

        assertNotNull(race);
        assertNotNull(race.getId());
        assertEquals("测试赛事", race.getName());
        assertNotNull(race.getCreatedTime());
    }

    @Test
    public void testGetAllRaces() {
        // 创建多个赛事
        raceService.createRace("赛事1");
        raceService.createRace("赛事2");
        raceService.createRace("赛事3");

        List<Race> races = raceService.getAllRaces();
        assertEquals(3, races.size());
    }

    @Test
    public void testGetAllRaces_Empty() {
        List<Race> races = raceService.getAllRaces();
        assertTrue(races.isEmpty());
    }

    @Test
    public void testUpdateRace() {
        // 创建赛事
        Race race = raceService.createRace("测试赛事");

        // 更新赛事
        race.setName("更新后的赛事");
        raceService.updateRace(race);

        // 验证更新
        List<Race> races = raceService.getAllRaces();
        assertEquals(1, races.size());
        assertEquals("更新后的赛事", races.get(0).getName());
    }

    @Test
    public void testDeleteRace() {
        // 创建赛事
        Race race = raceService.createRace("测试赛事");

        // 删除赛事
        raceService.deleteRace(race.getId());

        // 验证已删除
        List<Race> races = raceService.getAllRaces();
        assertTrue(races.isEmpty());
    }

    @Test
    public void testGetRaceById() {
        // 创建赛事
        Race created = raceService.createRace("测试赛事");

        // 查询赛事
        Race found = raceService.getRaceById(created.getId());
        assertNotNull(found);
        assertEquals("测试赛事", found.getName());
    }

    @Test
    public void testGetRaceById_NotFound() {
        Race found = raceService.getRaceById(999L);
        assertNull(found);
    }
}
