package com.race.chipcheck.model;

import javafx.beans.property.*;

/**
 * 选手信息模型（包含芯片1-4）
 */
public class Athlete {
    private final LongProperty id;
    private final LongProperty raceId;
    private final StringProperty bibNumber;
    private final StringProperty name;
    private final StringProperty chip1;
    private final StringProperty chip2;
    private final StringProperty chip3;
    private final StringProperty chip4;

    public Athlete() {
        this(null, null, "", "", "", "", "", "");
    }

    public Athlete(Long id, Long raceId, String bibNumber, String name,
                   String chip1, String chip2, String chip3, String chip4) {
        this.id = new SimpleLongProperty(id != null ? id : 0L);
        this.raceId = new SimpleLongProperty(raceId != null ? raceId : 0L);
        this.bibNumber = new SimpleStringProperty(bibNumber);
        this.name = new SimpleStringProperty(name);
        this.chip1 = new SimpleStringProperty(chip1);
        this.chip2 = new SimpleStringProperty(chip2);
        this.chip3 = new SimpleStringProperty(chip3);
        this.chip4 = new SimpleStringProperty(chip4);
    }

    /**
     * 检查给定的芯片号是否属于该选手（芯片1-4任意匹配）
     */
    public boolean hasChip(String chipId) {
        if (chipId == null || chipId.trim().isEmpty()) {
            return false;
        }
        return chipId.equals(chip1.get()) ||
               chipId.equals(chip2.get()) ||
               chipId.equals(chip3.get()) ||
               chipId.equals(chip4.get());
    }

    // JavaFX Properties（用于TableView绑定）
    public LongProperty idProperty() {
        return id;
    }

    public LongProperty raceIdProperty() {
        return raceId;
    }

    public StringProperty bibNumberProperty() {
        return bibNumber;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty chip1Property() {
        return chip1;
    }

    public StringProperty chip2Property() {
        return chip2;
    }

    public StringProperty chip3Property() {
        return chip3;
    }

    public StringProperty chip4Property() {
        return chip4;
    }

    // 标准getter/setter
    public Long getId() {
        return id.get();
    }

    public void setId(Long value) {
        id.set(value);
    }

    public Long getRaceId() {
        return raceId.get();
    }

    public void setRaceId(Long value) {
        raceId.set(value);
    }

    public String getBibNumber() {
        return bibNumber.get();
    }

    public void setBibNumber(String value) {
        bibNumber.set(value);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public String getChip1() {
        return chip1.get();
    }

    public void setChip1(String value) {
        chip1.set(value);
    }

    public String getChip2() {
        return chip2.get();
    }

    public void setChip2(String value) {
        chip2.set(value);
    }

    public String getChip3() {
        return chip3.get();
    }

    public void setChip3(String value) {
        chip3.set(value);
    }

    public String getChip4() {
        return chip4.get();
    }

    public void setChip4(String value) {
        chip4.set(value);
    }

    @Override
    public String toString() {
        return bibNumber.get() + " - " + name.get();
    }
}
