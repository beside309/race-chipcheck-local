package com.race.chipcheck.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel导入校验结果
 */
public class ImportValidationResult {
    private boolean valid;
    private final List<String> duplicateBibNumbers;
    private final List<String> duplicateChips;
    private final List<Integer> errorRows;
    private final List<String> errorMessages;
    private int importedCount;

    public ImportValidationResult() {
        this.valid = true;
        this.duplicateBibNumbers = new ArrayList<>();
        this.duplicateChips = new ArrayList<>();
        this.errorRows = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
        this.importedCount = 0;
    }

    public void addDuplicateBibNumber(String bibNumber, int row) {
        valid = false;
        duplicateBibNumbers.add(bibNumber);
        errorRows.add(row);
        errorMessages.add("第" + row + "行：参赛号码重复 - " + bibNumber);
    }

    public void addDuplicateChip(String chipId, int row) {
        valid = false;
        duplicateChips.add(chipId);
        errorRows.add(row);
        errorMessages.add("第" + row + "行：芯片号重复 - " + chipId);
    }

    public void addError(int row, String message) {
        valid = false;
        errorRows.add(row);
        errorMessages.add("第" + row + "行：" + message);
    }

    public boolean isValid() {
        return valid && errorMessages.isEmpty();
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getDuplicateBibNumbers() {
        return duplicateBibNumbers;
    }

    public List<String> getDuplicateChips() {
        return duplicateChips;
    }

    public List<Integer> getErrorRows() {
        return errorRows;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public String getErrorSummary() {
        if (errorMessages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("导入失败，发现以下错误：\n\n");
        for (String message : errorMessages) {
            sb.append(message).append("\n");
        }
        return sb.toString();
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }
}
