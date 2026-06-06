package com.pdf2excel.validation;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class BillValidator {

    private final List<ValidationRule> rules = new ArrayList<>();

    public BillValidator addRule(ValidationRule rule) {
        rules.add(rule);
        return this;
    }

    public BillValidator addRules(List<ValidationRule> rules) {
        this.rules.addAll(rules);
        return this;
    }

    public ValidationResult validate(BillData billData, String fileName) {
        ValidationResult result = new ValidationResult();
        result.setPassed(true);
        ValidationContext context = new ValidationContext(billData, fileName);

        for (ValidationRule rule : rules) {
            rule.validate(context, result);
        }

        return result;
    }
}
