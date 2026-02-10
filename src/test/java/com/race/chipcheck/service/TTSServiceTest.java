package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import com.race.chipcheck.model.VoiceContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TTSService 单元测试
 */
class TTSServiceTest {
    private DatabaseService databaseService;
    private PreferenceService preferenceService;
    private TTSService ttsService;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService(":memory:");
        preferenceService = new PreferenceService(databaseService);
        ttsService = new TTSService(preferenceService);
    }

    @Test
    void testSpeakWhenDisabled() {
        preferenceService.setVoiceEnabled(false);
        // 应该不报错，直接返回
        assertDoesNotThrow(() -> ttsService.speak("test"));
    }

    @Test
    void testSpeakWhenEnabled() {
        preferenceService.setVoiceEnabled(true);
        // 应该不报错
        assertDoesNotThrow(() -> ttsService.speak("test"));
    }

    @Test
    void testSpeakAthleteInfoWithBibNumber() {
        preferenceService.setVoiceEnabled(true);
        preferenceService.setVoiceContent(VoiceContentType.BIB_NUMBER);

        Athlete athlete = new Athlete(1L, 1L, "A123", "测试选手", "chip1", null, null, null);
        assertDoesNotThrow(() -> ttsService.speakAthleteInfo(athlete));
    }

    @Test
    void testSpeakAthleteInfoWithName() {
        preferenceService.setVoiceEnabled(true);
        preferenceService.setVoiceContent(VoiceContentType.NAME);

        Athlete athlete = new Athlete(1L, 1L, "A123", "测试选手", "chip1", null, null, null);
        assertDoesNotThrow(() -> ttsService.speakAthleteInfo(athlete));
    }

    @Test
    void testSpeakAthleteInfoWhenDisabled() {
        preferenceService.setVoiceEnabled(false);

        Athlete athlete = new Athlete(1L, 1L, "A123", "测试选手", "chip1", null, null, null);
        // 应该不报错，直接返回
        assertDoesNotThrow(() -> ttsService.speakAthleteInfo(athlete));
    }

    @AfterEach
    void tearDown() {
        ttsService.shutdown();
        databaseService.close();
    }
}
