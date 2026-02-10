package com.race.chipcheck.controller;

import com.race.chipcheck.model.Race;
import com.race.chipcheck.model.VerificationRecord;
import com.race.chipcheck.model.VoiceContentType;
import com.race.chipcheck.service.*;
import com.race.chipcheck.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
    private CheckBox voiceEnabledCheckbox;

    @FXML
    private RadioButton voiceContentBibRadio;

    @FXML
    private RadioButton voiceContentNameRadio;

    @FXML
    private CheckBox alertSoundCheckbox;

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

        // 预热 TTS 系统（在后台完成，避免首次使用时延迟）
        ttsService.warmup();
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

        recordCountLabel.setText("核验记录数: " + verificationService.getTotalRecordCount());
        totalAthleteCountLabel.setText("总人数: " + totalCount);
        athleteCountLabel.setText("已核验人数: " + verifiedCount);
        unverifiedCountLabel.setText("未核验人数: " + unverifiedCount);
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
        // RadioButton 分组
        ToggleGroup voiceContentGroup = new ToggleGroup();
        voiceContentBibRadio.setToggleGroup(voiceContentGroup);
        voiceContentNameRadio.setToggleGroup(voiceContentGroup);

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

        VoiceContentType contentType = preferenceService.getVoiceContent();
        if (contentType == VoiceContentType.NAME) {
            voiceContentNameRadio.setSelected(true);
        } else {
            voiceContentBibRadio.setSelected(true);
        }

        logger.info("配置加载完成：语音={}, 报警={}, 播报内容={}",
            preferenceService.isVoiceEnabled(),
            preferenceService.isAlertSoundEnabled(),
            contentType);
    }

    /**
     * 设置配置监听器
     */
    private void setupPreferenceListeners() {
        // 语音播报开关
        voiceEnabledCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setVoiceEnabled(newVal);
            logger.info("语音播报已{}", newVal ? "启用" : "禁用");
        });

        // 播报内容切换
        voiceContentBibRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                preferenceService.setVoiceContent(VoiceContentType.BIB_NUMBER);
                logger.info("播报内容切换为：号码布");
            }
        });

        voiceContentNameRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                preferenceService.setVoiceContent(VoiceContentType.NAME);
                logger.info("播报内容切换为：姓名");
            }
        });

        // 报警声音开关
        alertSoundCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            preferenceService.setAlertSoundEnabled(newVal);
            logger.info("报警声音已{}", newVal ? "启用" : "禁用");
        });
    }
}
