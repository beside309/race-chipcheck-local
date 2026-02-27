package com.race.chipcheck.controller;

import com.race.chipcheck.model.Athlete;
import com.race.chipcheck.model.ImportValidationResult;
import com.race.chipcheck.model.Race;
import com.race.chipcheck.service.*;
import com.race.chipcheck.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 选手维护控制器
 */
public class AthleteManagementController {
    private static final Logger logger = LoggerFactory.getLogger(AthleteManagementController.class);

    @FXML
    private Button importButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button addButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<Athlete> athleteTable;

    @FXML
    private TableColumn<Athlete, String> bibNumberColumn;

    @FXML
    private TableColumn<Athlete, String> nameColumn;

    @FXML
    private TableColumn<Athlete, String> chip1Column;

    @FXML
    private TableColumn<Athlete, String> chip2Column;

    @FXML
    private TableColumn<Athlete, String> chip3Column;

    @FXML
    private TableColumn<Athlete, String> chip4Column;

    @FXML
    private Label athleteCountLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button clearSearchButton;

    @FXML
    private Label searchResultLabel;

    // Services
    private final DatabaseService databaseService;
    private final RaceService raceService;
    private final AthleteService athleteService;
    private final ExcelImportService excelImportService;
    private final DataExportService dataExportService;
    private final RaceListManager raceListManager;

    // 数据
    private final ObservableList<Athlete> athletes = FXCollections.observableArrayList();
    private List<Athlete> allAthletes = new ArrayList<>();  // 保存所有选手，用于搜索

    // 当前赛事
    private Race currentRace;

    // 主控制器引用（用于通知其他Controller）
    private MainController mainController;

    public AthleteManagementController() {
        this.databaseService = DatabaseService.getInstance();
        this.raceService = new RaceService(databaseService);
        this.athleteService = new AthleteService(databaseService);
        this.excelImportService = new ExcelImportService(athleteService);
        this.dataExportService = new DataExportService(databaseService);
        this.raceListManager = RaceListManager.getInstance();
    }

    @FXML
    public void initialize() {
        logger.info("选手维护界面初始化");

        // 初始化TableView
        setupTableView();

        // 设置搜索框回车键支持
        setupSearchFieldEnterKey();
    }

    /**
     * 设置搜索框回车键支持
     */
    private void setupSearchFieldEnterKey() {
        searchField.setOnAction(event -> handleSearch());
    }

    /**
     * 设置当前赛事
     */
    public void setCurrentRace(Race race) {
        this.currentRace = race;
        logger.info("选手维护页面设置当前赛事：{} (ID: {})", race.getName(), race.getId());

        // 加载选手列表
        loadAthletes(race.getId());
    }

    /**
     * 设置主控制器引用
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * 设置TableView（可编辑）
     */
    private void setupTableView() {
        bibNumberColumn.setCellValueFactory(new PropertyValueFactory<>("bibNumber"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        chip1Column.setCellValueFactory(new PropertyValueFactory<>("chip1"));
        chip2Column.setCellValueFactory(new PropertyValueFactory<>("chip2"));
        chip3Column.setCellValueFactory(new PropertyValueFactory<>("chip3"));
        chip4Column.setCellValueFactory(new PropertyValueFactory<>("chip4"));

        // 设置可编辑
        athleteTable.setEditable(true);
        bibNumberColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        chip1Column.setCellFactory(TextFieldTableCell.forTableColumn());
        chip2Column.setCellFactory(TextFieldTableCell.forTableColumn());
        chip3Column.setCellFactory(TextFieldTableCell.forTableColumn());
        chip4Column.setCellFactory(TextFieldTableCell.forTableColumn());

        // 编辑提交事件
        bibNumberColumn.setOnEditCommit(event -> {
            event.getRowValue().setBibNumber(event.getNewValue());
            saveAthlete(event.getRowValue());
        });

        nameColumn.setOnEditCommit(event -> {
            event.getRowValue().setName(event.getNewValue());
            saveAthlete(event.getRowValue());
        });

        chip1Column.setOnEditCommit(event -> handleChipEdit(
            event.getRowValue(), event.getNewValue(), event.getOldValue(),
            (a, v) -> a.setChip1(v)));

        chip2Column.setOnEditCommit(event -> handleChipEdit(
            event.getRowValue(), event.getNewValue(), event.getOldValue(),
            (a, v) -> a.setChip2(v)));

        chip3Column.setOnEditCommit(event -> handleChipEdit(
            event.getRowValue(), event.getNewValue(), event.getOldValue(),
            (a, v) -> a.setChip3(v)));

        chip4Column.setOnEditCommit(event -> handleChipEdit(
            event.getRowValue(), event.getNewValue(), event.getOldValue(),
            (a, v) -> a.setChip4(v)));

        // 绑定数据
        athleteTable.setItems(athletes);
    }

    /**
     * 处理芯片编辑（含重复校验）
     */
    private void handleChipEdit(Athlete athlete, String newValue, String oldValue,
                               BiConsumer<Athlete, String> setter) {
        if (currentRace == null) {
            setter.accept(athlete, oldValue);
            athleteTable.refresh();
            return;
        }

        setter.accept(athlete, newValue);
        String error = athleteService.validateChipsForUpdate(currentRace.getId(), athlete);
        if (error != null) {
            setter.accept(athlete, oldValue);
            athleteTable.refresh();
            AlertHelper.showWarning("保存失败", error);
            return;
        }
        saveAthlete(athlete);
    }

    /**
     * 保存选手修改
     */
    private void saveAthlete(Athlete athlete) {
        try {
            athleteService.updateAthlete(athlete);
            logger.info("更新选手：{}", athlete.getBibNumber());
        } catch (Exception e) {
            AlertHelper.showError("保存失败", "保存选手信息失败：" + e.getMessage());
            logger.error("保存选手失败", e);
        }
    }

    /**
     * 加载选手列表
     */
    private void loadAthletes(Long raceId) {
        athletes.clear();
        List<Athlete> athleteList = athleteService.getAthletesByRaceId(raceId);
        allAthletes = new ArrayList<>(athleteList);  // 保存所有选手
        athletes.addAll(athleteList);
        updateAthleteCount();
        logger.info("加载 {} 个选手", athleteList.size());

        // 清空搜索框和搜索结果
        searchField.clear();
        searchResultLabel.setText("");

        // 通知芯片核验页面刷新统计
        notifyAthleteListChanged();
    }

    /**
     * 更新选手数量
     */
    private void updateAthleteCount() {
        athleteCountLabel.setText("选手数量: " + athletes.size());
    }

    /**
     * 通知芯片核验页面刷新统计数据
     */
    private void notifyAthleteListChanged() {
        if (mainController != null) {
            mainController.notifyAthleteListChanged();
        }
    }

    /**
     * 导入Excel
     */
    @FXML
    private void handleImport() {
        if (currentRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls")
        );

        File file = fileChooser.showOpenDialog(importButton.getScene().getWindow());
        if (file != null) {
            try {
                ImportValidationResult result = excelImportService.importAthletes(file, currentRace.getId());

                if (result.isValid()) {
                    AlertHelper.showInfo("导入成功", "成功导入 " + result.getImportedCount() + " 个选手");
                    loadAthletes(currentRace.getId());
                } else {
                    AlertHelper.showErrorWithDetails("导入失败", "数据校验失败，未导入任何数据", result.getErrorSummary());
                }

            } catch (Exception e) {
                AlertHelper.showError("导入失败", "导入Excel文件失败：" + e.getMessage());
                logger.error("导入Excel失败", e);
            }
        }
    }

    /**
     * 导出名单
     */
    @FXML
    private void handleExport() {
        if (currentRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        if (athletes.isEmpty()) {
            AlertHelper.showWarning("无数据", "当前赛事没有选手数据");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存Excel文件");
        fileChooser.setInitialFileName(DataExportService.generateExportFileName(currentRace.getName()));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            try {
                dataExportService.exportVerificationRecords(currentRace.getId(), currentRace.getName(), file);
                AlertHelper.showInfo("导出成功", "选手名单已导出到：\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertHelper.showError("导出失败", "导出失败：" + e.getMessage());
                logger.error("导出失败", e);
            }
        }
    }

    /**
     * 新增选手
     */
    @FXML
    private void handleAdd() {
        if (currentRace == null) {
            AlertHelper.showWarning("未选择赛事", "请先选择赛事");
            return;
        }

        // 创建空选手
        Athlete athlete = new Athlete();
        athlete.setRaceId(currentRace.getId());
        athlete.setBibNumber("请输入参赛号");
        athlete.setName("请输入姓名");
        athlete.setChip1("请输入芯片号");

        try {
            athleteService.createAthlete(athlete);
            athletes.add(athlete);
            updateAthleteCount();
            notifyAthleteListChanged();  // 通知芯片核验页面
            AlertHelper.showInfo("成功", "已添加新选手，请双击单元格进行编辑");
        } catch (Exception e) {
            AlertHelper.showError("添加失败", "添加选手失败：" + e.getMessage());
            logger.error("添加选手失败", e);
        }
    }

    /**
     * 删除选手
     */
    @FXML
    private void handleDelete() {
        Athlete selectedAthlete = athleteTable.getSelectionModel().getSelectedItem();
        if (selectedAthlete == null) {
            AlertHelper.showWarning("未选择", "请先选择要删除的选手");
            return;
        }

        boolean confirmed = AlertHelper.showConfirm(
            "确认删除",
            "确定要删除选手 \"" + selectedAthlete.getBibNumber() + " - " + selectedAthlete.getName() + "\" 吗？"
        );

        if (confirmed) {
            try {
                athleteService.deleteAthlete(selectedAthlete.getId());
                athletes.remove(selectedAthlete);
                updateAthleteCount();
                notifyAthleteListChanged();  // 通知芯片核验页面
                AlertHelper.showInfo("成功", "选手删除成功");
            } catch (Exception e) {
                AlertHelper.showError("删除失败", "删除选手失败：" + e.getMessage());
                logger.error("删除选手失败", e);
            }
        }
    }

    /**
     * 刷新列表
     */
    @FXML
    private void handleRefresh() {
        if (currentRace != null) {
            loadAthletes(currentRace.getId());
            AlertHelper.showInfo("刷新成功", "选手列表已刷新");
        }
    }

    /**
     * 搜索选手
     */
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.trim().isEmpty()) {
            AlertHelper.showWarning("输入错误", "请输入搜索关键词");
            return;
        }

        keyword = keyword.trim().toLowerCase();

        // 在所有选手中搜索
        List<Athlete> searchResults = new ArrayList<>();
        for (Athlete athlete : allAthletes) {
            // 按参赛号、姓名、芯片1-4搜索
            if (matchKeyword(athlete.getBibNumber(), keyword) ||
                matchKeyword(athlete.getName(), keyword) ||
                matchKeyword(athlete.getChip1(), keyword) ||
                matchKeyword(athlete.getChip2(), keyword) ||
                matchKeyword(athlete.getChip3(), keyword) ||
                matchKeyword(athlete.getChip4(), keyword)) {
                searchResults.add(athlete);
            }
        }

        // 更新显示
        athletes.clear();
        athletes.addAll(searchResults);
        updateAthleteCount();

        // 显示搜索结果
        searchResultLabel.setText("找到 " + searchResults.size() + " 个匹配结果");
        logger.info("搜索关键词：{}，找到 {} 个结果", keyword, searchResults.size());
    }

    /**
     * 清空搜索
     */
    @FXML
    private void handleClearSearch() {
        searchField.clear();
        searchResultLabel.setText("");

        // 恢复显示所有选手
        athletes.clear();
        athletes.addAll(allAthletes);
        updateAthleteCount();

        logger.info("清空搜索，恢复显示所有选手");
    }

    /**
     * 匹配关键词（不区分大小写）
     */
    private boolean matchKeyword(String value, String keyword) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return value.toLowerCase().contains(keyword);
    }
}
