package com.race.chipcheck.controller;

import com.race.chipcheck.model.UnverifiedChip;
import com.race.chipcheck.service.VerificationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * “未核验芯片”窗口控制器
 */
public class UnverifiedChipDialogController {
    private static final Logger logger = LoggerFactory.getLogger(UnverifiedChipDialogController.class);

    @FXML
    private Label summaryLabel;

    @FXML
    private TableView<UnverifiedChip> chipTable;

    @FXML
    private TableColumn<UnverifiedChip, String> bibColumn;

    @FXML
    private TableColumn<UnverifiedChip, String> nameColumn;

    @FXML
    private TableColumn<UnverifiedChip, String> chipColumn;

    @FXML
    private TableColumn<UnverifiedChip, String> statusColumn;

    @FXML
    private Button refreshButton;

    @FXML
    private Button closeButton;

    private final ObservableList<UnverifiedChip> chips = FXCollections.observableArrayList();

    private VerificationService verificationService;

    public void setVerificationService(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @FXML
    public void initialize() {
        bibColumn.setCellValueFactory(new PropertyValueFactory<>("bibNumber"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        chipColumn.setCellValueFactory(new PropertyValueFactory<>("chipId"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        chipTable.setItems(chips);
    }

    /**
     * 刷新未核验芯片列表
     */
    public void refreshData() {
        if (verificationService == null) {
            return;
        }
        List<UnverifiedChip> list = verificationService.getUnverifiedChips();
        chips.setAll(list);
        summaryLabel.setText("检测到共有 " + list.size() + " 个芯片未核验");
        logger.info("刷新未核验芯片列表：{} 条", list.size());
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}

