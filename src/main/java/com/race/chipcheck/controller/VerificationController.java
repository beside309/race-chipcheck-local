package com.race.chipcheck.controller;

import com.race.chipcheck.model.Race;
import com.race.chipcheck.model.VerificationRecord;
import com.race.chipcheck.service.*;
import com.race.chipcheck.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 芯片核验控制器
 */
public class VerificationController {
    private static final Logger logger = LoggerFactory.getLogger(VerificationController.class);

    @FXML
    private Button connectButton;

    @FXML
    private Circle readerStatusIndicator;

    @FXML
    private Label readerStatusLabel;

    @FXML
    private TableView<VerificationRecord> recordTable;

    @FXML
    private TableColumn<VerificationRecord, LocalDateTime> timeColumn;

    @FXML
    private TableColumn<VerificationRecord, String> chipIdColumn;

    @FXML
    private TableColumn<VerificationRecord, String> bibNumberColumn;

    @FXML
    private TableColumn<VerificationRecord, String> nameColumn;

    @FXML
    private TableColumn<VerificationRecord, String> statusColumn;

    @FXML
    private TableColumn<VerificationRecord, String> remarkColumn;

    @FXML
    private Button clearButton;

    @FXML
    private Button clearStatisticsButton;

    @FXML
    private TextField testChipIdField;

    @FXML
    private Button testButton;

    @FXML
    private Label recordCountLabel;

    @FXML
    private Label totalAthleteCountLabel;

    @FXML
    private Label athleteCountLabel;

    @FXML
    private Label unverifiedCountLabel;

    @FXML
    private Label totalChipCountLabel;

    @FXML
    private Label verifiedChipCountLabel;

    @FXML
    private Label unverifiedChipCountLabel;

    @FXML
    private CheckBox voiceEnabledCheckbox;

    @FXML
    private CheckBox voiceContentBibCheckbox;

    @FXML
    private CheckBox voiceContentNameCheckbox;

    @FXML
    private CheckBox alertSoundCheckbox;

    @FXML
    private CheckBox displayBibCheckbox;

    @FXML
    private CheckBox displayNameCheckbox;

    @FXML
    private Label resultBibLabel;

    @FXML
    private Label resultNameLabel;

    // Services
    private final DatabaseService databaseService;
    private final RaceService raceService;
    private final AthleteService athleteService;
    private final PreferenceService preferenceService;
    private final AlertSoundService alertSoundService;
    private final TTSService ttsService;
    private final RfidReaderService rfidReaderService;
    private final RaceListManager raceListManager;
    private VerificationService verificationService;

    // 数据
    private final ObservableList<VerificationRecord> records = FXCollections.observableArrayList();

    // 当前赛事
    private Race currentRace;

    // 最近一次核验结果（用于右侧显示，受“核验信息设置”复选框控制）
    private String lastResultBib = "";
    private String lastResultName = "";

    // “未核验芯片”窗口
    private Stage unverifiedChipStage;
    private UnverifiedChipDialogController unverifiedChipDialogController;
    private boolean autoUnverifiedDialogShown = false;

    public VerificationController() {
        // 初始化Services
        this.databaseService = DatabaseService.getInstance();
        this.raceService = new RaceService(databaseService);
        this.athleteService = new AthleteService(databaseService);
        this.preferenceService = new PreferenceService(databaseService);
        this.alertSoundService = new AlertSoundService(preferenceService);
        this.ttsService = new TTSService(preferenceService);
        this.rfidReaderService = new RfidReaderService();
        this.raceListManager = RaceListManager.getInstance();
    }

    @FXML
    public void initialize() {
        logger.info("芯片核验界面初始化");

        // 初始化VerificationService
        verificationService = new VerificationService(
            athleteService,
            databaseService,
            alertSoundService,
            ttsService,
            preferenceService,
            records
        );

        // 初始化TableView
        setupTableView();

        // 初始化连接状态
        updateConnectionStatus(false);

        // 设置RFID读卡器监听器
        setupRfidListener();

        // 绑定统计标签
        updateStatistics();

        // 设置测试输入框回车键支持
        setupTestFieldEnterKey();

        // 设置清空按钮右键菜单
        setupClearButtonContextMenu();

        // 初始化配置UI
        setupPreferences();

        // 核验结果回调：更新右侧显示
        verificationService.setResultCallback(this::onVerificationResult);

        // 右侧“核验信息设置”复选框变化时刷新显示
        if (displayBibCheckbox != null) {
            displayBibCheckbox.selectedProperty().addListener((o, a, b) -> updateResultLabels());
        }
        if (displayNameCheckbox != null) {
            displayNameCheckbox.selectedProperty().addListener((o, a, b) -> updateResultLabels());
        }

        // 预热 TTS 系统（在后台完成，避免首次使用时延迟）
        ttsService.warmup();
    }

    /**
     * 核验结果回调：更新右侧参赛号、姓名显示，并在“未核验芯片”窗口打开时刷新列表
     */
    private void onVerificationResult(String chipId, String bibNumber, String name, boolean success) {
        lastResultBib = bibNumber != null ? bibNumber : "";
        lastResultName = name != null ? name : "";
        updateResultLabels();

        if (success && unverifiedChipStage != null && unverifiedChipStage.isShowing() && unverifiedChipDialogController != null) {
            unverifiedChipDialogController.refreshData();
        }
    }

    /**
     * 根据“核验信息设置”复选框更新右侧标签内容与可见性
     */
    private void updateResultLabels() {
        if (resultBibLabel == null || resultNameLabel == null) {
            return;
        }
        boolean showBib = displayBibCheckbox != null && displayBibCheckbox.isSelected();
        boolean showName = displayNameCheckbox != null && displayNameCheckbox.isSelected();

        resultBibLabel.setText(showBib ? lastResultBib : "");
        resultBibLabel.setVisible(showBib);
        resultBibLabel.setManaged(showBib);

        resultNameLabel.setText(showName ? lastResultName : "");
        resultNameLabel.setVisible(showName);
        resultNameLabel.setManaged(showName);
    }

    /**
     * 设置测试输入框回车键支持
     */
    private void setupTestFieldEnterKey() {
        testChipIdField.setOnAction(event -> handleTest());
    }

    /**
     * 设置当前赛事
     */
    public void setCurrentRace(Race race) {
        this.currentRace = race;
        logger.info("芯片核验页面设置当前赛事：{} (ID: {})", race.getName(), race.getId());

        // 设置VerificationService的当前赛事ID
        verificationService.setCurrentRaceId(race.getId());

        // 立即更新统计信息
        updateStatistics();

        // 切换赛事时重置自动弹窗标记
        autoUnverifiedDialogShown = false;
    }

    /**
     * 刷新统计数据（供外部调用，如选手列表变化时）
     */
    public void refreshStatistics() {
        updateStatistics();
    }

    /**
     * 设置TableView
     */
    private void setupTableView() {
        // 设置列
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("verificationTime"));
        chipIdColumn.setCellValueFactory(new PropertyValueFactory<>("chipId"));
        bibNumberColumn.setCellValueFactory(new PropertyValueFactory<>("bibNumber"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));

        // 自定义时间列显示格式
        timeColumn.setCellFactory(column -> new TableCell<>() {
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

        // 自定义状态列颜色
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("成功".equals(item)) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // 绑定数据
        recordTable.setItems(records);
    }

    /**
     * 连接/断开读卡器
     */
    @FXML
    private void handleConnect() {
        if (rfidReaderService.isConnected()) {
            // 断开连接
            rfidReaderService.disconnect();
            connectButton.setText("连接读卡器");
        } else {
            // 获取可用串口列表
            List<String> availablePorts = RfidReaderService.getAvailablePorts();

            if (availablePorts.isEmpty()) {
                AlertHelper.showWarning("无可用串口", "未检测到可用的串口，请检查读卡器是否已连接。");
                return;
            }

            // 显示端口选择对话框
            ChoiceDialog<String> dialog = new ChoiceDialog<>(availablePorts.get(0), availablePorts);
            dialog.setTitle("选择串口");
            dialog.setHeaderText("请选择读卡器所在的串口");
            dialog.setContentText("串口：");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String selectedPort = result.get();
                // 连接读卡器，默认波特率115200
                boolean success = rfidReaderService.connect(selectedPort, 115200);
                if (success) {
                    connectButton.setText("断开连接");
                } else {
                    AlertHelper.showError("连接失败", "无法连接到读卡器端口：" + selectedPort);
                }
            }
        }
    }

    /**
     * 设置RFID读卡器监听器
     */
    private void setupRfidListener() {
        rfidReaderService.addListener(new RfidReaderService.TagReadListener() {
            @Override
            public void onTagRead(String epc) {
                logger.info("读取到芯片：{}", epc);
                verificationService.processChip(epc);
                // 在Platform.runLater中更新统计，确保记录已添加
                javafx.application.Platform.runLater(() -> {
                    updateStatistics();
                });
            }

            @Override
            public void onConnectionChanged(boolean connected) {
                updateConnectionStatus(connected);
            }

            @Override
            public void onError(String message) {
                logger.error("读卡器错误：{}", message);
            }
        });
    }

    /**
     * 更新连接状态
     */
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            readerStatusIndicator.setFill(Color.GREEN);
            readerStatusLabel.setText("已连接");
            readerStatusLabel.setStyle("-fx-text-fill: green;");
        } else {
            readerStatusIndicator.setFill(Color.GRAY);
            readerStatusLabel.setText("未连接");
            readerStatusLabel.setStyle("-fx-text-fill: gray;");
        }
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        int totalCount = verificationService.getTotalAthleteCount();
        int verifiedCount = verificationService.getVerifiedAthleteCount();
        int unverifiedCount = totalCount - verifiedCount;

        int totalChipCount = verificationService.getTotalChipCount();
        int verifiedChipCount = verificationService.getVerifiedChipCount();
        int unverifiedChipCount = totalChipCount - verifiedChipCount;

        recordCountLabel.setText("核验记录数: " + verificationService.getTotalRecordCount());
        totalAthleteCountLabel.setText("总人数: " + totalCount);
        athleteCountLabel.setText("已核验人数: " + verifiedCount);
        unverifiedCountLabel.setText("未核验人数: " + unverifiedCount);
        totalChipCountLabel.setText("芯片总数: " + totalChipCount);
        verifiedChipCountLabel.setText("已核验芯片数: " + verifiedChipCount);
        unverifiedChipCountLabel.setText("未核验芯片数: " + unverifiedChipCount);

        // 当未核验人数为 0 且仍有未核验芯片时，自动弹出“未核验芯片”窗口（每个赛事仅弹一次）
        if (totalCount > 0 && unverifiedCount == 0 && unverifiedChipCount > 0 && !autoUnverifiedDialogShown) {
            autoUnverifiedDialogShown = true;
            javafx.application.Platform.runLater(this::openUnverifiedChipDialog);
        }
    }

    /**
     * 手动打开“未核验芯片”窗口
     */
    @FXML
    private void handleShowUnverifiedChips() {
        openUnverifiedChipDialog();
    }

    private void openUnverifiedChipDialog() {
        try {
            if (unverifiedChipStage == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/unverified_chips.fxml"));
                Parent root = loader.load();
                unverifiedChipDialogController = loader.getController();
                unverifiedChipDialogController.setVerificationService(verificationService);

                unverifiedChipStage = new Stage();
                unverifiedChipStage.setTitle("未核验芯片");
                unverifiedChipStage.initOwner(recordTable.getScene().getWindow());
                unverifiedChipStage.initModality(Modality.NONE);
                unverifiedChipStage.setScene(new Scene(root));
            }

            unverifiedChipDialogController.refreshData();
            unverifiedChipStage.show();
            unverifiedChipStage.toFront();
        } catch (Exception e) {
            logger.error("打开未核验芯片窗口失败", e);
            AlertHelper.showError("打开失败", "无法打开未核验芯片窗口：" + e.getMessage());
        }
    }

    /**
     * 测试核验（手动输入芯片号）
     */
    @FXML
    private void handleTest() {
        String chipId = testChipIdField.getText();
        if (chipId == null || chipId.trim().isEmpty()) {
            AlertHelper.showWarning("输入错误", "请输入芯片号");
            return;
        }

        if (currentRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        // 调用核验服务处理芯片
        chipId = chipId.trim();
        verificationService.processChip(chipId);
        logger.info("测试核验芯片：{}", chipId);

        // 清空输入框
        testChipIdField.clear();

        // 在Platform.runLater中更新统计，确保记录已添加
        javafx.application.Platform.runLater(() -> {
            updateStatistics();
        });
    }

    /**
     * 清空记录
     */
    @FXML
    private void handleClear() {
        if (AlertHelper.showConfirm("确认清空", "确定要清空所有核验记录吗？")) {
            verificationService.clearRecords();
            updateStatistics();
            logger.info("清空核验记录");
        }
    }

    /**
     * 设置清空按钮右键菜单
     */
    private void setupClearButtonContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem clearDisplayItem = new MenuItem("清空显示记录");
        clearDisplayItem.setOnAction(event -> handleClear());

        MenuItem clearStatisticsItem = new MenuItem("清空核验统计");
        clearStatisticsItem.setOnAction(event -> handleClearStatistics());

        contextMenu.getItems().addAll(clearDisplayItem, clearStatisticsItem);

        // 右键显示菜单
        clearButton.setOnContextMenuRequested(event -> {
            contextMenu.show(clearButton, event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * 清空核验统计（核验人数归0 + 删除数据库记录）
     */
    @FXML
    private void handleClearStatistics() {
        if (currentRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        // 构建详细的确认信息
        String confirmMessage = String.format(
            "此操作将：\n" +
            "1. 清空核验人数统计（当前：%d 人）\n" +
            "2. 删除当前赛事的所有核验记录（当前：%d 条）\n" +
            "3. 清空显示界面\n\n" +
            "此操作不可恢复，确定要继续吗？",
            verificationService.getVerifiedAthleteCount(),
            verificationService.getTotalRecordCount()
        );

        if (AlertHelper.showConfirm("确认清空核验统计", confirmMessage)) {
            try {
                verificationService.clearVerificationStatistics();
                updateStatistics();
                logger.info("清空核验统计成功");
                AlertHelper.showInfo("操作成功", "核验统计已清空");
            } catch (Exception e) {
                logger.error("清空核验统计失败", e);
                AlertHelper.showError("操作失败", "清空核验统计失败：" + e.getMessage());
            }
        }
    }

    /**
     * 初始化配置UI
     */
    private void setupPreferences() {
        // 加载配置到 UI
        loadPreferences();

        // 监听 UI 变化并保存配置
        setupPreferenceListeners();
    }

    /**
     * 加载配置到UI
     */
    private void loadPreferences() {
        voiceEnabledCheckbox.setSelected(preferenceService.isVoiceEnabled());
        alertSoundCheckbox.setSelected(preferenceService.isAlertSoundEnabled());

        voiceContentBibCheckbox.setSelected(preferenceService.isVoiceContentBibEnabled());
        voiceContentNameCheckbox.setSelected(preferenceService.isVoiceContentNameEnabled());

        logger.info("配置加载完成：语音={}, 报警={}, 播报参赛号={}, 播报姓名={}",
            preferenceService.isVoiceEnabled(),
            preferenceService.isAlertSoundEnabled(),
            preferenceService.isVoiceContentBibEnabled(),
            preferenceService.isVoiceContentNameEnabled());
    }

    /**
     * 设置配置监听器
     */
    private void setupPreferenceListeners() {
        voiceEnabledCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setVoiceEnabled(newVal);
            logger.info("语音播报已{}", newVal ? "启用" : "禁用");
        });

        voiceContentBibCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setVoiceContentBibEnabled(newVal);
            logger.info("播报参赛号码已{}", newVal ? "启用" : "禁用");
        });

        voiceContentNameCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setVoiceContentNameEnabled(newVal);
            logger.info("播报姓名已{}", newVal ? "启用" : "禁用");
        });

        alertSoundCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setAlertSoundEnabled(newVal);
            logger.info("报警声音已{}", newVal ? "启用" : "禁用");
        });
    }
}
