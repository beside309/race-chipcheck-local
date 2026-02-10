package com.race.chipcheck;

import com.race.chipcheck.config.AppConfig;
import com.race.chipcheck.service.RaceListManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.race.chipcheck.util.AlertHelper;

/**
 * JavaFX应用主入口
 */
public class RaceChipCheckApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(RaceChipCheckApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // 确保AppConfig先初始化（设置系统属性给logback使用）
            String dataDir = AppConfig.getUserDataDirectory();
            logger.info("应用数据目录：{}", dataDir);
            logger.info("数据库路径：{}", AppConfig.getDatabasePath());
            logger.info("日志目录：{}", AppConfig.getLogsDirectory());

            // 初始化共享赛事列表
            RaceListManager.getInstance().refreshRaces();

            // 加载赛事选择页面（启动页面）
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/race_selection.fxml"));
            Parent root = loader.load();

            // 设置场景
            Scene scene = new Scene(root, 1400, 800);

            // 加载CSS样式（如果存在）
            try {
                scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            } catch (Exception e) {
                logger.warn("CSS样式文件未找到，使用默认样式");
            }

            // 设置舞台
            primaryStage.setTitle(AlertHelper.APP_NAME + " " + AlertHelper.APP_VERSION);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(700); 
            primaryStage.show();

            logger.info("应用启动成功");

        } catch (Exception e) {
            logger.error("应用启动失败", e);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        logger.info("应用正在关闭");
        // 清理资源
        try {
            com.race.chipcheck.service.DatabaseService.getInstance().close();
        } catch (Exception e) {
            logger.error("关闭数据库连接失败", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
