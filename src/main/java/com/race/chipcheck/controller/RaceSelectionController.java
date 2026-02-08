package com.race.chipcheck.controller;

import com.race.chipcheck.model.Race;
import com.race.chipcheck.service.DatabaseService;
import com.race.chipcheck.service.RaceService;
import com.race.chipcheck.service.RaceListManager;
import com.race.chipcheck.util.AlertHelper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 赛事选择控制器
 */
public class RaceSelectionController {
    private static final Logger logger = LoggerFactory.getLogger(RaceSelectionController.class);

    @FXML
    private TableView<Race> raceTable;

    @FXML
    private TableColumn<Race, Long> idColumn;

    @FXML
    private TableColumn<Race, String> nameColumn;

    @FXML
    private TableColumn<Race, LocalDateTime> createdTimeColumn;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button selectButton;

    // Services
    private final DatabaseService databaseService;
    private final RaceService raceService;
    private final RaceListManager raceListManager;

    // 数据（使用共享列表）
    private final ObservableList<Race> races;

    public RaceSelectionController() {
        this.databaseService = DatabaseService.getInstance();
        this.raceService = new RaceService(databaseService);
        this.raceListManager = RaceListManager.getInstance();
        this.races = raceListManager.getRaces();
    }

    @FXML
    public void initialize() {
        logger.info("赛事选择界面初始化");

        // 初始化TableView
        setupTableView();

        // 加载赛事列表
        loadRaces();
    }

    /**
     * 设置TableView
     */
    private void setupTableView() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        createdTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createdTime"));

        // 自定义时间列显示格式
        createdTimeColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        // 绑定数据
        raceTable.setItems(races);

        // 双击进入赛事
        raceTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleSelect();
            }
        });
    }

    /**
     * 加载赛事列表
     */
    private void loadRaces() {
        raceListManager.refreshRaces();
        logger.info("加载 {} 个赛事", races.size());
    }

    /**
     * 新增赛事
     */
    @FXML
    private void handleAdd() {
        Optional<String> result = AlertHelper.showInputDialog(
            "新增赛事",
            "请输入赛事名称：",
            "赛事名称"
        );

        result.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                AlertHelper.showWarning("输入错误", "赛事名称不能为空");
                return;
            }

            try {
                Race race = raceService.createRace(name.trim());
                raceListManager.addRace(race);
                AlertHelper.showInfo("成功", "赛事创建成功：" + name);
                logger.info("创建赛事：{}", name);
            } catch (Exception e) {
                AlertHelper.showError("创建失败", "创建赛事失败：" + e.getMessage());
                logger.error("创建赛事失败", e);
            }
        });
    }

    /**
     * 编辑赛事
     */
    @FXML
    private void handleEdit() {
        Race selectedRace = raceTable.getSelectionModel().getSelectedItem();
        if (selectedRace == null) {
            AlertHelper.showWarning("未选择", "请先选择要编辑的赛事");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selectedRace.getName());
        dialog.setTitle("编辑赛事");
        dialog.setHeaderText("修改赛事名称：");
        dialog.setContentText("赛事名称：");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                AlertHelper.showWarning("输入错误", "赛事名称不能为空");
                return;
            }

            try {
                selectedRace.setName(name.trim());
                raceService.updateRace(selectedRace);
                raceListManager.updateRace(selectedRace);
                raceTable.refresh();
                AlertHelper.showInfo("成功", "赛事更新成功");
                logger.info("更新赛事：{} (ID: {})", name, selectedRace.getId());
            } catch (Exception e) {
                AlertHelper.showError("更新失败", "更新赛事失败：" + e.getMessage());
                logger.error("更新赛事失败", e);
            }
        });
    }

    /**
     * 删除赛事
     */
    @FXML
    private void handleDelete() {
        Race selectedRace = raceTable.getSelectionModel().getSelectedItem();
        if (selectedRace == null) {
            AlertHelper.showWarning("未选择", "请先选择要删除的赛事");
            return;
        }

        boolean confirmed = AlertHelper.showConfirm(
            "确认删除",
            "确定要删除赛事 \"" + selectedRace.getName() + "\" 吗？\n\n" +
            "警告：删除赛事将同时删除该赛事的所有选手和核验记录！"
        );

        if (confirmed) {
            try {
                raceService.deleteRace(selectedRace.getId());
                raceListManager.removeRace(selectedRace);
                AlertHelper.showInfo("成功", "赛事删除成功");
                logger.info("删除赛事：{} (ID: {})", selectedRace.getName(), selectedRace.getId());
            } catch (Exception e) {
                AlertHelper.showError("删除失败", "删除赛事失败：" + e.getMessage());
                logger.error("删除赛事失败", e);
            }
        }
    }

    /**
     * 刷新列表
     */
    @FXML
    private void handleRefresh() {
        loadRaces();
        AlertHelper.showInfo("刷新成功", "赛事列表已刷新");
    }

    /**
     * 选择赛事并进入主页面
     */
    @FXML
    private void handleSelect() {
        Race selectedRace = raceTable.getSelectionModel().getSelectedItem();
        if (selectedRace == null) {
            AlertHelper.showWarning("未选择", "请先选择要进入的赛事");
            return;
        }

        try {
            logger.info("选择赛事：{} (ID: {})", selectedRace.getName(), selectedRace.getId());

            // 加载主页面
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            // 获取MainController并传递选中的赛事
            MainController mainController = loader.getController();
            mainController.setCurrentRace(selectedRace);

            // 获取当前Stage并切换场景
            Stage stage = (Stage) raceTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("赛事选手芯片核验系统 - " + selectedRace.getName());

        } catch (Exception e) {
            logger.error("进入赛事失败", e);
            AlertHelper.showError("错误", "进入赛事失败：" + e.getMessage());
        }
    }
}
