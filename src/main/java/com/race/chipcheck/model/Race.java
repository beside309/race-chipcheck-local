package com.race.chipcheck.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * 赛事信息模型
 */
public class Race {
    private final LongProperty id;
    private final StringProperty name;
    private final ObjectProperty<LocalDateTime> createdTime;

    public Race() {
        this(null, "", LocalDateTime.now());
    }

    public Race(Long id, String name, LocalDateTime createdTime) {
        this.id = new SimpleLongProperty(id != null ? id : 0L);
        this.name = new SimpleStringProperty(name);
        this.createdTime = new SimpleObjectProperty<>(createdTime);
    }

    // JavaFX Properties（用于TableView绑定）
    public LongProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<LocalDateTime> createdTimeProperty() {
        return createdTime;
    }

    // 标准getter/setter
    public Long getId() {
        return id.get();
    }

    public void setId(Long value) {
        id.set(value);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public LocalDateTime getCreatedTime() {
        return createdTime.get();
    }

    public void setCreatedTime(LocalDateTime value) {
        createdTime.set(value);
    }

    @Override
    public String toString() {
        return name.get();
    }
}
