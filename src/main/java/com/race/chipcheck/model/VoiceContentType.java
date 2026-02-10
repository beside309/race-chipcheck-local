package com.race.chipcheck.model;

/**
 * 语音播报内容类型
 */
public enum VoiceContentType {
    BIB_NUMBER("号码布"),
    NAME("姓名");

    private final String displayName;

    VoiceContentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
