package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.PowerDetail;
import com.pdf2excel.model.FeeDetail;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

import java.util.HashSet;
import java.util.Set;

public class DuplicateEntryRule implements ValidationRule {

    @Override
    public String name() {
        return "重复条目校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillData data = context.getBillData();

        if (data.getPowerDetails() != null) {
            Set<String> seen = new HashSet<>();
            int dupCount = 0;
            for (PowerDetail d : data.getPowerDetails()) {
                String key = d.getAccountId() + "|" + d.getMeterId();
                if (!seen.add(key)) {
                    dupCount++;
                }
            }
            if (dupCount > 0) {
                result.addWarning(String.format(
                        "[%s] 电量明细中发现%d条重复（户号+电表号相同）",
                        name(), dupCount));
            }
        }

        if (data.getFeeDetails() != null) {
            Set<String> seen = new HashSet<>();
            int dupCount = 0;
            for (FeeDetail d : data.getFeeDetails()) {
                if (!seen.add(d.getAccountId())) {
                    dupCount++;
                }
            }
            if (dupCount > 0) {
                result.addWarning(String.format(
                        "[%s] 电费明细中发现%d条重复户号",
                        name(), dupCount));
            }
        }
    }
}
