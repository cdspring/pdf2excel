package com.pdf2excel.validation;

import com.pdf2excel.model.ValidationResult;

public interface ValidationRule {

    String name();

    void validate(ValidationContext context, ValidationResult result);
}
