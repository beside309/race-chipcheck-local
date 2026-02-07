package com.race.chipcheck.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * 核验记录模型
 */
public class VerificationRecord {
    private final LongProperty id;
    private final LongProperty raceId;
    private final LongProperty athleteId;
    private final ObjectProperty<LocalDateTime> verificationTime;
    private final StringProperty chipId;
    private final StringProperty bibNumber;
    private final StringProperty name;
    private final StringProperty status;
    private final StringProperty remark;

    public VerificationRecord() {
        this(null, null, null, LocalDateTime.now(), "", "", "", "失败", "");
    }

    public VerificationRecord(Long id, Long raceId, Long athleteId, LocalDateTime time,
                             String chipId, String bibNumber, String name, String status, String remark) {
        this.id = new SimpleLongProperty(id != null ? id : 0L);
        this.raceId = new SimpleLongProperty(raceId != null ? raceId : 0L);
        this.athleteId = new SimpleLongProperty(athleteId != null ? athleteId : 0L);
        this.verificationTime = new SimpleObjectProperty<>(time);
        this.chipId = new SimpleStringProperty(chipId);
        this.bibNumber = new SimpleStringProperty(bibNumber != null ? bibNumber : "未知");
        this.name = new SimpleStringProperty(name != null ? name : "未知选手");
        this.status = new SimpleStringProperty(status);
        this.remark = new SimpleStringProperty(remark != null ? remark : "");
    }

    // JavaFX Properties（用于TableView绑定）
    public LongProperty idProperty() {
        return id;
    }

    public LongProperty raceIdProperty() {
        return raceId;
    }

    public LongProperty athleteIdProperty() {
        return athleteId;
    }

    public ObjectProperty<LocalDateTime> verificationTimeProperty() {
        return verificationTime;
    }

    public StringProperty chipIdProperty() {
        return chipId;
    }

    public StringProperty bibNumberProperty() {
        return bibNumber;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty remarkProperty() {
        return remark;
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

    public Long getAthleteId() {
        return athleteId.get();
    }

    public void setAthleteId(Long value) {
        athleteId.set(value);
    }

    public LocalDateTime getVerificationTime() {
        return verificationTime.get();
    }

    public void setVerificationTime(LocalDateTime value) {
        verificationTime.set(value);
    }

    public String getChipId() {
        return chipId.get();
    }

    public void setChipId(String value) {
        chipId.set(value);
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

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String value) {
        status.set(value);
    }

    public String getRemark() {
        return remark.get();
    }

    public void setRemark(String value) {
        remark.set(value);
    }
}
