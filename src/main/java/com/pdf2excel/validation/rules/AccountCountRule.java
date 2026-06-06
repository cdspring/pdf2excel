package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

public class AccountCountRule implements ValidationRule {

    @Override
    public String name() {
        return "户数一致性校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillData data = context.getBillData();
        if (data.getHeader() == null || data.getHeader().getTotalAccounts() <= 0) {
            return;
        }

        int expected = data.getHeader().getTotalAccounts();

        if (data.getPowerDetails() != null && !data.getPowerDetails().isEmpty()) {
            int powerCount = data.getPowerDetails().size();
            if (powerCount != expected) {
                result.addWarning(String.format(
                        "[%s] 表头总户数=%d, 电量明细条数=%d",
                        name(), expected, powerCount));
            }
        }

        if (data.getFeeDetails() != null && !data.getFeeDetails().isEmpty()) {
            int feeCount = data.getFeeDetails().size();
            if (feeCount != expected) {
                result.addWarning(String.format(
                        "[%s] 表头总户数=%d, 电费明细条数=%d",
                        name(), expected, feeCount));
            }
        }
    }
}
