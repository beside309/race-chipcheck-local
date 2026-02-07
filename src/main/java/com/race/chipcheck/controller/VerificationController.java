package com.race.chipcheck.controller;

import com.race.chipcheck.model.Race;
import com.race.chipcheck.model.VerificationRecord;
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

/**
 * 芯片核验控制器
 */
public class VerificationController {
    private static final Logger logger = LoggerFactory.getLogger(VerificationController.class);

    @FXML
    private ComboBox<Race> raceComboBox;

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
    private Label recordCountLabel;

    @FXML
    private Label athleteCountLabel;

    // Services
    private final DatabaseService databaseService;
    private final RaceService raceService;
    private final AthleteService athleteService;
    private final AlertSoundService alertSoundService;
    private final RfidReaderService rfidReaderService;
    private VerificationService verificationService;

    // 数据
    private final ObservableList<VerificationRecord> records = FXCollections.observableArrayList();

    public VerificationController() {
        // 初始化Services
        this.databaseService = DatabaseService.getInstance();
        this.raceService = new RaceService(databaseService);
        this.athleteService = new AthleteService(databaseService);
        this.alertSoundService = new AlertSoundService();
        this.rfidReaderService = new RfidReaderService();
    }

    @FXML
    public void initialize() {
        logger.info("芯片核验界面初始化");

        // 初始化VerificationService
        verificationService = new VerificationService(
            athleteService,
            databaseService,
            alertSoundService,
            records
        );

        // 初始化TableView
        setupTableView();

        // 初始化赛事下拉框
        loadRaces();

        // 初始化连接状态
        updateConnectionStatus(false);

        // 设置RFID读卡器监听器
        setupRfidListener();

        // 绑定统计标签
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
     * 加载赛事列表
     */
    private void loadRaces() {
        List<Race> races = raceService.getAllRaces();
        raceComboBox.setItems(FXCollections.observableArrayList(races));

        if (!races.isEmpty()) {
            raceComboBox.getSelectionModel().selectFirst();
            onRaceSelected();
        }
    }

    /**
     * 赛事选择事件
     */
    @FXML
    private void onRaceSelected() {
        Race selectedRace = raceComboBox.getValue();
        if (selectedRace != null) {
            verificationService.setCurrentRaceId(selectedRace.getId());
            logger.info("选择赛事：{} (ID: {})", selectedRace.getName(), selectedRace.getId());
        }
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
            // 连接读卡器
            // 默认使用COM3端口，波特率115200
            boolean success = rfidReaderService.connect("COM3", 115200);
            if (success) {
                connectButton.setText("断开连接");
            } else {
                AlertHelper.showError("连接失败", "无法连接到读卡器，请检查设备是否连接并选择正确的端口。");
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
                updateStatistics();
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
        recordCountLabel.setText("核验记录数: " + verificationService.getTotalRecordCount());
        athleteCountLabel.setText("核验人数: " + verificationService.getVerifiedAthleteCount());
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
}
