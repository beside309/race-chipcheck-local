package com.race.chipcheck.model;

/**
 * 未核验芯片信息（用于“未核验芯片”窗口展示）
 */
public class UnverifiedChip {
    private final String bibNumber;
    private final String name;
    private final String chipId;
    private final String status;

    public UnverifiedChip(String bibNumber, String name, String chipId, String status) {
        this.bibNumber = bibNumber;
        this.name = name;
        this.chipId = chipId;
        this.status = status;
    }

    public String getBibNumber() {
        return bibNumber;
    }

    public String getName() {
        return name;
    }

    public String getChipId() {
        return chipId;
    }

    public String getStatus() {
        return status;
    }
}

