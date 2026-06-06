package com.pdf2excel.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Data
public class ValidationResult {
    private boolean passed;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public void addError(String msg) {
        errors.add(msg);
        passed = false;
    }

    public void addWarning(String msg) {
        warnings.add(msg);
    }
}
