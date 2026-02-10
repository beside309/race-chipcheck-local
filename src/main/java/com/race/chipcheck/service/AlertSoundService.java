package com.race.chipcheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

/**
 * 报警声音服务
 */
public class AlertSoundService {
    private static final Logger logger = LoggerFactory.getLogger(AlertSoundService.class);
    private Clip alertClip;
    private final PreferenceService preferenceService;

    public AlertSoundService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
        try {
            // 加载声音文件
            URL soundUrl = getClass().getResource("/sounds/alert.wav");
            if (soundUrl != null) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundUrl);
                alertClip = AudioSystem.getClip();
                alertClip.open(audioIn);
                logger.info("报警声音文件加载成功");
            } else {
                logger.warn("报警声音文件未找到：/sounds/alert.wav");
            }
        } catch (UnsupportedAudioFileException e) {
            logger.error("不支持的音频文件格式", e);
        } catch (IOException e) {
            logger.error("读取声音文件失败", e);
        } catch (LineUnavailableException e) {
            logger.error("音频线路不可用", e);
        }
    }

    /**
     * 播放报警声音
     */
    public void playAlert() {
        // 检查开关
        if (!preferenceService.isAlertSoundEnabled()) {
            logger.debug("报警声音已禁用，跳过播放");
            return;
        }

        if (alertClip != null) {
            // 重置到开始位置
            alertClip.setFramePosition(0);
            alertClip.start();
            logger.debug("播放报警声音");
        } else {
            logger.warn("报警声音未加载，无法播放");
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (alertClip != null && alertClip.isRunning()) {
            alertClip.stop();
        }
    }

    /**
     * 释放资源
     */
    public void close() {
        if (alertClip != null) {
            alertClip.close();
        }
    }
}
