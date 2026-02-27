package com.race.chipcheck.service;

import com.race.chipcheck.model.VoiceContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 用户配置服务
 * 负责管理用户偏好设置（语音播报、报警声音等）
 */
public class PreferenceService {
    private static final Logger logger = LoggerFactory.getLogger(PreferenceService.class);
    private final DatabaseService databaseService;

    public PreferenceService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * 获取语音播报开关状态
     * @return true=启用，false=禁用
     */
    public boolean isVoiceEnabled() {
        return Boolean.parseBoolean(getValue("voice_enabled", "true"));
    }

    /**
     * 设置语音播报开关
     * @param enabled true=启用，false=禁用
     */
    public void setVoiceEnabled(boolean enabled) {
        setValue("voice_enabled", String.valueOf(enabled));
    }

    /**
     * 是否播报参赛号码
     */
    public boolean isVoiceContentBibEnabled() {
        migrateVoiceContentIfNeeded();
        return Boolean.parseBoolean(getValue("voice_content_bib", "true"));
    }

    public void setVoiceContentBibEnabled(boolean enabled) {
        setValue("voice_content_bib", String.valueOf(enabled));
    }

    /**
     * 是否播报姓名
     */
    public boolean isVoiceContentNameEnabled() {
        migrateVoiceContentIfNeeded();
        return Boolean.parseBoolean(getValue("voice_content_name", "false"));
    }

    public void setVoiceContentNameEnabled(boolean enabled) {
        setValue("voice_content_name", String.valueOf(enabled));
    }

    /** 从旧的 voice_content 迁移到 voice_content_bib/name（仅执行一次） */
    private void migrateVoiceContentIfNeeded() {
        if (getValue("voice_content_bib", null) != null) {
            return;
        }
        String old = getValue("voice_content", "BIB_NUMBER");
        if ("NAME".equals(old)) {
            setValue("voice_content_bib", "false");
            setValue("voice_content_name", "true");
        } else {
            setValue("voice_content_bib", "true");
            setValue("voice_content_name", "false");
        }
    }

    /** 兼容旧 API：按当前两个布尔值返回等效类型（供测试等使用） */
    public VoiceContentType getVoiceContent() {
        migrateVoiceContentIfNeeded();
        boolean bib = Boolean.parseBoolean(getValue("voice_content_bib", "true"));
        boolean name = Boolean.parseBoolean(getValue("voice_content_name", "false"));
        if (name && !bib) {
            return VoiceContentType.NAME;
        }
        return VoiceContentType.BIB_NUMBER;
    }

    /** 兼容旧 API：设置单一类型时另一项关闭 */
    public void setVoiceContent(VoiceContentType type) {
        setValue("voice_content_bib", type == VoiceContentType.BIB_NUMBER ? "true" : "false");
        setValue("voice_content_name", type == VoiceContentType.NAME ? "true" : "false");
    }

    /**
     * 获取报警声音开关状态
     * @return true=启用，false=禁用
     */
    public boolean isAlertSoundEnabled() {
        return Boolean.parseBoolean(getValue("alert_sound_enabled", "true"));
    }

    /**
     * 设置报警声音开关
     * @param enabled true=启用，false=禁用
     */
    public void setAlertSoundEnabled(boolean enabled) {
        setValue("alert_sound_enabled", String.valueOf(enabled));
    }

    /**
     * 从数据库读取配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private String getValue(String key, String defaultValue) {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (PreparedStatement pstmt = databaseService.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            logger.error("读取配置失败: {}", key, e);
        }
        return defaultValue;
    }

    /**
     * 保存配置值到数据库
     * @param key 配置键
     * @param value 配置值
     */
    private void setValue(String key, String value) {
        String sql = "INSERT OR REPLACE INTO app_settings (key, value, updated_time) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = databaseService.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
            logger.debug("保存配置: {} = {}", key, value);
        } catch (SQLException e) {
            logger.error("保存配置失败: {}", key, e);
        }
    }
}
