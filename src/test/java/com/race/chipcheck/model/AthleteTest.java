package com.race.chipcheck.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Athlete model测试类
 */
public class AthleteTest {

    @Test
    public void testHasChip_WithChip1() {
        Athlete athlete = new Athlete();
        athlete.setChip1("CHIP001");

        assertTrue(athlete.hasChip("CHIP001"));
        assertFalse(athlete.hasChip("CHIP002"));
    }

    @Test
    public void testHasChip_WithChip2() {
        Athlete athlete = new Athlete();
        athlete.setChip2("CHIP002");

        assertTrue(athlete.hasChip("CHIP002"));
        assertFalse(athlete.hasChip("CHIP001"));
    }

    @Test
    public void testHasChip_WithMultipleChips() {
        Athlete athlete = new Athlete();
        athlete.setChip1("CHIP001");
        athlete.setChip2("CHIP002");
        athlete.setChip3("CHIP003");
        athlete.setChip4("CHIP004");

        assertTrue(athlete.hasChip("CHIP001"));
        assertTrue(athlete.hasChip("CHIP002"));
        assertTrue(athlete.hasChip("CHIP003"));
        assertTrue(athlete.hasChip("CHIP004"));
        assertFalse(athlete.hasChip("CHIP005"));
    }

    @Test
    public void testHasChip_WithNullChip() {
        Athlete athlete = new Athlete();
        athlete.setChip1("CHIP001");

        assertFalse(athlete.hasChip(null));
    }

    @Test
    public void testHasChip_WithEmptyChip() {
        Athlete athlete = new Athlete();
        athlete.setChip1("CHIP001");

        assertFalse(athlete.hasChip(""));
        assertFalse(athlete.hasChip("   "));
    }

    @Test
    public void testAthleteProperties() {
        Athlete athlete = new Athlete();
        athlete.setId(1L);
        athlete.setRaceId(100L);
        athlete.setBibNumber("A001");
        athlete.setName("测试选手");
        athlete.setChip1("CHIP001");
        athlete.setChip2("CHIP002");
        athlete.setChip3("CHIP003");
        athlete.setChip4("CHIP004");

        assertEquals(1L, athlete.getId());
        assertEquals(100L, athlete.getRaceId());
        assertEquals("A001", athlete.getBibNumber());
        assertEquals("测试选手", athlete.getName());
        assertEquals("CHIP001", athlete.getChip1());
        assertEquals("CHIP002", athlete.getChip2());
        assertEquals("CHIP003", athlete.getChip3());
        assertEquals("CHIP004", athlete.getChip4());
    }
}
