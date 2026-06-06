package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.PowerDetail;
import com.pdf2excel.model.FeeDetail;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

import java.math.BigDecimal;

public class NonNegativeAmountRule implements ValidationRule {

    @Override
    public String name() {
        return "金额非负校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillData data = context.getBillData();

        if (data.getPowerDetails() != null) {
            for (PowerDetail d : data.getPowerDetails()) {
                if (isNegative(d.getPower())) {
                    result.addWarning(String.format(
                            "[%s] 电量明细序号%d电量为负: %s",
                            name(), d.getSeq(), d.getPower()));
                }
            }
        }

        if (data.getFeeDetails() != null) {
            for (FeeDetail d : data.getFeeDetails()) {
                if (isNegative(d.getFee())) {
                    result.addWarning(String.format(
                            "[%s] 电费明细序号%d电费为负: %s",
                            name(), d.getSeq(), d.getFee()));
                }
            }
        }
    }

    private boolean isNegative(BigDecimal val) {
        return val != null && val.compareTo(BigDecimal.ZERO) < 0;
    }
}
