package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.FeeDetail;
import com.pdf2excel.model.PowerDetail;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

import java.util.HashSet;
import java.util.Set;

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
            int powerCount = distinctAccountCount(data.getPowerDetails());
            if (powerCount != expected) {
                result.addWarning(String.format(
                        "[%s] 表头总户数=%d, 电量明细户数=%d",
                        name(), expected, powerCount));
            }
        }

        if (data.getFeeDetails() != null && !data.getFeeDetails().isEmpty()) {
            int feeCount = distinctAccountCount(data.getFeeDetails());
            if (feeCount != expected) {
                result.addWarning(String.format(
                        "[%s] 表头总户数=%d, 电费明细户数=%d",
                        name(), expected, feeCount));
            }
        }
    }

    private int distinctAccountCount(java.util.List<?> details) {
        Set<String> accounts = new HashSet<>();
        for (Object detail : details) {
            if (detail instanceof PowerDetail) {
                String accountId = ((PowerDetail) detail).getAccountId();
                if (accountId != null && !accountId.trim().isEmpty()) {
                    accounts.add(accountId.trim());
                }
            } else if (detail instanceof FeeDetail) {
                String accountId = ((FeeDetail) detail).getAccountId();
                if (accountId != null && !accountId.trim().isEmpty()) {
                    accounts.add(accountId.trim());
                }
            }
        }
        return accounts.size();
    }
}
