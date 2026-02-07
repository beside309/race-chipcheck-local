package com.race.chipcheck.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 主窗口控制器
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab raceManagementTab;

    @FXML
    private Tab athleteManagementTab;

    @FXML
    private Tab verificationTab;

    @FXML
    private Tab exportTab;

    @FXML
    public void initialize() {
        logger.info("主窗口初始化");

        try {
            // 加载赛事管理界面
            loadTab(raceManagementTab, "/fxml/race_management.fxml");

            // 加载选手维护界面
            loadTab(athleteManagementTab, "/fxml/athlete_management.fxml");

            // 加载芯片核验界面
            loadTab(verificationTab, "/fxml/verification.fxml");

            // 加载数据导出界面
            loadTab(exportTab, "/fxml/export.fxml");

            // 默认选中芯片核验Tab
            mainTabPane.getSelectionModel().select(verificationTab);

        } catch (Exception e) {
            logger.error("加载界面失败", e);
        }
    }

    /**
     * 加载Tab内容
     */
    private void loadTab(Tab tab, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            BorderPane content = loader.load();
            tab.setContent(content);
        } catch (IOException e) {
            logger.error("加载Tab失败：{}", fxmlPath, e);
        }
    }
}
