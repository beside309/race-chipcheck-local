package com.race.chipcheck.controller;

import com.race.chipcheck.model.Race;
import com.race.chipcheck.service.RaceListManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
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
    private Tab athleteManagementTab;

    @FXML
    private Tab verificationTab;

    @FXML
    private Tab exportTab;

    @FXML
    private Label raceNameLabel;

    @FXML
    private Button backButton;

    // 当前选中的赛事
    private Race currentRace;

    @FXML
    public void initialize() {
        logger.info("主窗口初始化");

        try {
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
     * 设置当前赛事
     */
    public void setCurrentRace(Race race) {
        this.currentRace = race;
        raceNameLabel.setText("当前赛事：" + race.getName());
        logger.info("当前赛事设置为：{} (ID: {})", race.getName(), race.getId());

        // 通知所有子Controller当前赛事
        notifyControllersRaceChanged(race);
    }

    /**
     * 通知所有子Controller赛事已改变
     */
    private void notifyControllersRaceChanged(Race race) {
        try {
            // 获取选手维护Controller
            BorderPane athleteContent = (BorderPane) athleteManagementTab.getContent();
            if (athleteContent != null && athleteContent.getUserData() instanceof AthleteManagementController) {
                AthleteManagementController controller = (AthleteManagementController) athleteContent.getUserData();
                controller.setMainController(this);  // 设置MainController引用
                controller.setCurrentRace(race);
            }

            // 获取芯片核验Controller
            BorderPane verificationContent = (BorderPane) verificationTab.getContent();
            if (verificationContent != null && verificationContent.getUserData() instanceof VerificationController) {
                VerificationController controller = (VerificationController) verificationContent.getUserData();
                controller.setCurrentRace(race);
            }

            // 获取数据导出Controller
            BorderPane exportContent = (BorderPane) exportTab.getContent();
            if (exportContent != null && exportContent.getUserData() instanceof ExportController) {
                ExportController controller = (ExportController) exportContent.getUserData();
                controller.setCurrentRace(race);
            }
        } catch (Exception e) {
            logger.error("通知子Controller失败", e);
        }
    }

    /**
     * 通知VerificationController刷新统计数据（选手列表变化时调用）
     */
    public void notifyAthleteListChanged() {
        try {
            BorderPane verificationContent = (BorderPane) verificationTab.getContent();
            if (verificationContent != null && verificationContent.getUserData() instanceof VerificationController) {
                VerificationController controller = (VerificationController) verificationContent.getUserData();
                controller.refreshStatistics();
                logger.debug("已通知芯片核验页面刷新统计数据");
            }
        } catch (Exception e) {
            logger.error("通知芯片核验页面失败", e);
        }
    }

    /**
     * 加载Tab内容
     */
    private void loadTab(Tab tab, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            BorderPane content = loader.load();

            // 保存Controller到content的UserData中
            content.setUserData(loader.getController());

            tab.setContent(content);
        } catch (IOException e) {
            logger.error("加载Tab失败：{}", fxmlPath, e);
        }
    }

    /**
     * 返回赛事选择页面
     */
    @FXML
    private void handleBack() {
        try {
            logger.info("返回赛事选择页面");

            // 加载赛事选择页面
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/race_selection.fxml"));
            Parent root = loader.load();

            // 获取当前Stage并切换场景
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("赛事选手芯片核验系统");

        } catch (Exception e) {
            logger.error("返回赛事选择页面失败", e);
        }
    }

    /**
     * 获取当前赛事
     */
    public Race getCurrentRace() {
        return currentRace;
    }
}
