package com.race.chipcheck.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Race model测试类
 */
public class RaceTest {

    @Test
    public void testRaceProperties() {
        LocalDateTime now = LocalDateTime.now();
        Race race = new Race();
        race.setId(1L);
        race.setName("测试赛事");
        race.setCreatedTime(now);

        assertEquals(1L, race.getId());
        assertEquals("测试赛事", race.getName());
        assertEquals(now, race.getCreatedTime());
    }

    @Test
    public void testRaceConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Race race = new Race(1L, "测试赛事", now);

        assertEquals(1L, race.getId());
        assertEquals("测试赛事", race.getName());
        assertEquals(now, race.getCreatedTime());
    }
}
