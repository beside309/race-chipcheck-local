package com.race.chipcheck.service;

import com.race.chipcheck.model.VoiceContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PreferenceService 单元测试
 */
class PreferenceServiceTest {
    private DatabaseService databaseService;
    private PreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService(":memory:");
        preferenceService = new PreferenceService(databaseService);
    }

    @Test
    void testDefaultPreferences() {
        assertTrue(preferenceService.isVoiceEnabled());
        assertEquals(VoiceContentType.BIB_NUMBER, preferenceService.getVoiceContent());
        assertTrue(preferenceService.isAlertSoundEnabled());
    }

    @Test
    void testSaveAndLoadVoiceEnabled() {
        preferenceService.setVoiceEnabled(false);
        assertFalse(preferenceService.isVoiceEnabled());

        preferenceService.setVoiceEnabled(true);
        assertTrue(preferenceService.isVoiceEnabled());
    }

    @Test
    void testSaveAndLoadVoiceContent() {
        preferenceService.setVoiceContent(VoiceContentType.NAME);
        assertEquals(VoiceContentType.NAME, preferenceService.getVoiceContent());

        preferenceService.setVoiceContent(VoiceContentType.BIB_NUMBER);
        assertEquals(VoiceContentType.BIB_NUMBER, preferenceService.getVoiceContent());
    }

    @Test
    void testSaveAndLoadAlertSoundEnabled() {
        preferenceService.setAlertSoundEnabled(false);
        assertFalse(preferenceService.isAlertSoundEnabled());

        preferenceService.setAlertSoundEnabled(true);
        assertTrue(preferenceService.isAlertSoundEnabled());
    }

    @Test
    void testPersistenceAcrossInstances() {
        // 设置配置
        preferenceService.setVoiceEnabled(false);
        preferenceService.setVoiceContent(VoiceContentType.NAME);
        preferenceService.setAlertSoundEnabled(false);

        // 创建新实例，验证配置持久化
        PreferenceService newPreferenceService = new PreferenceService(databaseService);
        assertFalse(newPreferenceService.isVoiceEnabled());
        assertEquals(VoiceContentType.NAME, newPreferenceService.getVoiceContent());
        assertFalse(newPreferenceService.isAlertSoundEnabled());
    }

    @AfterEach
    void tearDown() {
        databaseService.close();
    }
}
