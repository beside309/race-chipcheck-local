package com.race.chipcheck.service;

import com.race.chipcheck.model.Athlete;
import com.race.chipcheck.model.ImportValidationResult;
import com.race.chipcheck.util.ExcelReader;
import com.race.chipcheck.util.ExcelReader.AthleteRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Excel导入服务（含数据校验）
 */
public class ExcelImportService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelImportService.class);
    private final AthleteService athleteService;

    public ExcelImportService(AthleteService athleteService) {
        this.athleteService = athleteService;
    }

    /**
     * 导入Excel名单
     * @param excelFile Excel文件
     * @param raceId 赛事ID
     * @return 校验结果，包含错误信息
     */
    public ImportValidationResult importAthletes(File excelFile, Long raceId) {
        ImportValidationResult result = new ImportValidationResult();

        try {
            // 1. 读取Excel文件
            List<AthleteRow> rows = ExcelReader.readAthletes(excelFile);

            if (rows.isEmpty()) {
                result.addError(0, "Excel文件中没有数据");
                return result;
            }

            // 2. 校验数据
            validateData(rows, raceId, result);

            if (!result.isValid()) {
                // 有错误，直接返回，不导入任何数据
                logger.warn("Excel导入校验失败：{} 个错误", result.getErrorMessages().size());
                return result;
            }

            // 3. 批量导入数据库
            for (AthleteRow row : rows) {
                Athlete athlete = new Athlete();
                athlete.setRaceId(raceId);
                athlete.setBibNumber(row.getBibNumber());
                athlete.setName(row.getName());
                athlete.setChip1(row.getChip1());
                athlete.setChip2(row.getChip2());
                athlete.setChip3(row.getChip3());
                athlete.setChip4(row.getChip4());

                athleteService.createAthlete(athlete);
            }

            result.setValid(true);
            result.setImportedCount(rows.size());
            logger.info("成功导入 {} 个选手", rows.size());

        } catch (Exception e) {
            logger.error("导入Excel失败", e);
            result.addError(0, "导入失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 校验数据：检查重复参赛号和重复芯片号
     */
    private void validateData(List<AthleteRow> rows, Long raceId, ImportValidationResult result) {
        // 检查Excel内部重复
        Set<String> bibNumbers = new HashSet<>();
        Set<String> chips = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            AthleteRow row = rows.get(i);
            int rowNum = i + 2; // Excel行号（从第2行开始，第1行是表头）

            // 检查必填字段
            if (row.getBibNumber() == null || row.getBibNumber().trim().isEmpty()) {
                result.addError(rowNum, "参赛号码不能为空");
            }
            if (row.getChip1() == null || row.getChip1().trim().isEmpty()) {
                result.addError(rowNum, "芯片1不能为空");
            }

            // 检查Excel内重复参赛号
            if (row.getBibNumber() != null && !row.getBibNumber().trim().isEmpty()) {
                if (!bibNumbers.add(row.getBibNumber())) {
                    result.addDuplicateBibNumber(row.getBibNumber(), rowNum);
                }
            }

            // 检查Excel内重复芯片号（芯片1-4）
            List<String> chipList = Arrays.asList(
                row.getChip1(), row.getChip2(), row.getChip3(), row.getChip4()
            );

            for (String chip : chipList) {
                if (chip != null && !chip.trim().isEmpty()) {
                    if (!chips.add(chip)) {
                        result.addDuplicateChip(chip, rowNum);
                    }
                }
            }
        }

        // 检查与数据库中已有数据的重复
        List<Athlete> existingAthletes = athleteService.getAthletesByRaceId(raceId);

        for (Athlete existing : existingAthletes) {
            // 检查参赛号重复
            for (int i = 0; i < rows.size(); i++) {
                AthleteRow row = rows.get(i);
                int rowNum = i + 2;

                if (row.getBibNumber() != null && row.getBibNumber().equals(existing.getBibNumber())) {
                    result.addDuplicateBibNumber(row.getBibNumber(), rowNum);
                }

                // 检查芯片号重复
                List<String> chipList = Arrays.asList(
                    row.getChip1(), row.getChip2(), row.getChip3(), row.getChip4()
                );

                for (String chip : chipList) {
                    if (chip != null && !chip.trim().isEmpty() && existing.hasChip(chip)) {
                        result.addDuplicateChip(chip, rowNum);
                    }
                }
            }
        }
    }
}
