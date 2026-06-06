package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.PowerDetail;
import com.pdf2excel.model.FeeDetail;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

import java.util.regex.Pattern;
import java.util.List;

public class AccountIdFormatRule implements ValidationRule {

    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^\\d{7,13}$");

    @Override
    public String name() {
        return "户号格式校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillData data = context.getBillData();

        if (data.getPowerDetails() != null) {
            checkList(data.getPowerDetails(), result, "电量明细");
        }
        if (data.getFeeDetails() != null) {
            checkFeeList(data.getFeeDetails(), result, "电费明细");
        }
    }

    private void checkList(List<PowerDetail> details, ValidationResult result, String section) {
        int invalidCount = 0;
        for (PowerDetail d : details) {
            if (d.getAccountId() == null || !ACCOUNT_ID_PATTERN.matcher(d.getAccountId()).matches()) {
                invalidCount++;
            }
        }
        if (invalidCount > 0) {
            result.addWarning(String.format(
                    "[%s] %s中有%d条户号格式异常（期望7-13位纯数字）",
                    name(), section, invalidCount));
        }
    }

    private void checkFeeList(List<FeeDetail> details, ValidationResult result, String section) {
        int invalidCount = 0;
        for (FeeDetail d : details) {
            if (d.getAccountId() == null || !ACCOUNT_ID_PATTERN.matcher(d.getAccountId()).matches()) {
                invalidCount++;
            }
        }
        if (invalidCount > 0) {
            result.addWarning(String.format(
                    "[%s] %s中有%d条户号格式异常（期望7-13位纯数字）",
                    name(), section, invalidCount));
        }
    }
}
