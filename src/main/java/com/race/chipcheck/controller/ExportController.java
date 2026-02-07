package com.race.chipcheck.controller;

import com.race.chipcheck.model.Race;
import com.race.chipcheck.service.DatabaseService;
import com.race.chipcheck.service.DataExportService;
import com.race.chipcheck.service.RaceService;
import com.race.chipcheck.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * 数据导出控制器
 */
public class ExportController {
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    @FXML
    private ComboBox<Race> raceComboBox;

    @FXML
    private Button exportButton;

    @FXML
    private Label infoLabel;

    // Services
    private final DatabaseService databaseService;
    private final RaceService raceService;
    private final DataExportService dataExportService;

    public ExportController() {
        this.databaseService = DatabaseService.getInstance();
        this.raceService = new RaceService(databaseService);
        this.dataExportService = new DataExportService(databaseService);
    }

    @FXML
    public void initialize() {
        logger.info("数据导出界面初始化");

        // 加载赛事列表
        loadRaces();
    }

    /**
     * 加载赛事列表
     */
    private void loadRaces() {
        List<Race> races = raceService.getAllRaces();
        raceComboBox.setItems(FXCollections.observableArrayList(races));

        if (!races.isEmpty()) {
            raceComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * 导出核验记录
     */
    @FXML
    private void handleExport() {
        Race selectedRace = raceComboBox.getValue();
        if (selectedRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        // 文件选择对话框
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存核验记录");
        fileChooser.setInitialFileName(
            DataExportService.generateExportFileName(selectedRace.getName())
        );
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            try {
                dataExportService.exportVerificationRecords(
                    selectedRace.getId(),
                    selectedRace.getName(),
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
}
