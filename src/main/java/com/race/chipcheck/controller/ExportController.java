package com.race.chipcheck.controller;

import com.race.chipcheck.model.Race;
import com.race.chipcheck.service.DatabaseService;
import com.race.chipcheck.service.DataExportService;
import com.race.chipcheck.service.RaceService;
import com.race.chipcheck.service.RaceListManager;
import com.race.chipcheck.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 数据导出控制器
 */
public class ExportController {
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    @FXML
    private Button exportButton;

    @FXML
    private Button exportUnverifiedButton;

    @FXML
    private Label infoLabel;

    // Services
    private final DatabaseService databaseService;
    private final RaceService raceService;
    private final DataExportService dataExportService;
    private final RaceListManager raceListManager;

    // 当前赛事
    private Race currentRace;

    public ExportController() {
        this.databaseService = DatabaseService.getInstance();
        this.raceService = new RaceService(databaseService);
        this.dataExportService = new DataExportService(databaseService);
        this.raceListManager = RaceListManager.getInstance();
    }

    @FXML
    public void initialize() {
        logger.info("数据导出界面初始化");
    }

    /**
     * 设置当前赛事
     */
    public void setCurrentRace(Race race) {
        this.currentRace = race;
        logger.info("数据导出页面设置当前赛事：{} (ID: {})", race.getName(), race.getId());
    }

    /**
     * 导出核验记录
     */
    @FXML
    private void handleExport() {
        if (currentRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        // 文件选择对话框
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存核验记录");
        fileChooser.setInitialFileName(
            DataExportService.generateExportFileName(currentRace.getName())
        );
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            try {
                dataExportService.exportVerificationRecords(
                    currentRace.getId(),
                    currentRace.getName(),
                    file
                );

                AlertHelper.showInfo(
                    "导出成功",
                    "核验记录已导出到：\n" + file.getAbsolutePath()
                );

                logger.info("导出核验记录成功：{}", file.getAbsolutePath());

            } catch (Exception e) {
                AlertHelper.showError("导出失败", "导出失败：" + e.getMessage());
                logger.error("导出失败", e);
            }
        }
    }

    /**
     * 导出未核验选手名单
     */
    @FXML
    private void handleExportUnverified() {
        if (currentRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        // 文件选择对话框
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存未核验选手名单");
        fileChooser.setInitialFileName(
            DataExportService.generateUnverifiedExportFileName(currentRace.getName())
        );
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(exportUnverifiedButton.getScene().getWindow());
        if (file != null) {
            try {
                dataExportService.exportUnverifiedAthletes(
                    currentRace.getId(),
                    currentRace.getName(),
                    file
                );

                AlertHelper.showInfo(
                    "导出成功",
                    "未核验选手名单已导出到：\n" + file.getAbsolutePath()
                );

                logger.info("导出未核验选手名单成功：{}", file.getAbsolutePath());

            } catch (Exception e) {
                AlertHelper.showError("导出失败", "导出失败：" + e.getMessage());
                logger.error("导出未核验选手名单失败", e);
            }
        }
    }
}
