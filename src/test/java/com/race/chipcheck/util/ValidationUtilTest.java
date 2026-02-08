package com.race.chipcheck.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationUtil测试类
 */
public class ValidationUtilTest {

    @Test
    public void testIsEmpty_WithNull() {
        assertTrue(ValidationUtil.isEmpty(null));
    }

    @Test
    public void testIsEmpty_WithEmptyString() {
        assertTrue(ValidationUtil.isEmpty(""));
        assertTrue(ValidationUtil.isEmpty("   "));
    }

    @Test
    public void testIsEmpty_WithNonEmptyString() {
        assertFalse(ValidationUtil.isEmpty("test"));
        assertFalse(ValidationUtil.isEmpty("  test  "));
    }

    @Test
    public void testIsNotEmpty_WithNull() {
        assertFalse(ValidationUtil.isNotEmpty(null));
    }

    @Test
    public void testIsNotEmpty_WithEmptyString() {
        assertFalse(ValidationUtil.isNotEmpty(""));
        assertFalse(ValidationUtil.isNotEmpty("   "));
    }

    @Test
    public void testIsNotEmpty_WithNonEmptyString() {
        assertTrue(ValidationUtil.isNotEmpty("test"));
        assertTrue(ValidationUtil.isNotEmpty("  test  "));
    }
}
