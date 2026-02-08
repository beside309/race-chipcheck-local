package com.race.chipcheck.service;

import com.race.chipcheck.model.Race;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 赛事列表管理器（单例）
 * 提供共享的赛事列表，供所有Controller使用
 */
public class RaceListManager {
    private static final Logger logger = LoggerFactory.getLogger(RaceListManager.class);
    private static RaceListManager instance;

    private final ObservableList<Race> races;
    private final RaceService raceService;

    private RaceListManager() {
        this.races = FXCollections.observableArrayList();
        this.raceService = new RaceService(DatabaseService.getInstance());
    }

    public static synchronized RaceListManager getInstance() {
        if (instance == null) {
            instance = new RaceListManager();
        }
        return instance;
    }

    /**
     * 获取共享的赛事列表
     */
    public ObservableList<Race> getRaces() {
        return races;
    }

    /**
     * 从数据库刷新赛事列表
     */
    public void refreshRaces() {
        races.clear();
        List<Race> raceList = raceService.getAllRaces();
        races.addAll(raceList);
        logger.info("刷新赛事列表，共 {} 个赛事", raceList.size());
    }

    /**
     * 添加赛事到列表
     */
    public void addRace(Race race) {
        races.add(0, race);  // 插入到顶部
        logger.info("添加赛事到共享列表：{} (ID: {})", race.getName(), race.getId());
    }

    /**
     * 更新赛事
     */
    public void updateRace(Race race) {
        // ObservableList会自动通知更新
        logger.info("更新赛事：{} (ID: {})", race.getName(), race.getId());
    }

    /**
     * 从列表中删除赛事
     */
    public void removeRace(Race race) {
        races.remove(race);
        logger.info("从共享列表删除赛事：{} (ID: {})", race.getName(), race.getId());
    }

    /**
     * 根据ID查找赛事
     */
    public Race getRaceById(Long id) {
        return races.stream()
                .filter(race -> race.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
