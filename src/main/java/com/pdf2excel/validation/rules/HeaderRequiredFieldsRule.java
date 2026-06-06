package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillHeader;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

public class HeaderRequiredFieldsRule implements ValidationRule {

    @Override
    public String name() {
        return "表头必填字段校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillHeader header = context.getBillData().getHeader();
        if (header == null) {
            result.addError("[" + name() + "] 表头数据为空");
            return;
        }

        if (isBlank(header.getGroupId())) {
            result.addWarning("[" + name() + "] 集团户编号为空");
        }
        if (isBlank(header.getGroupName())) {
            result.addWarning("[" + name() + "] 集团户名称为空");
        }
        if (header.getTotalPower() == null) {
            result.addError("[" + name() + "] 本期电量为空");
        }
        if (header.getTotalFee() == null) {
            result.addError("[" + name() + "] 本期电费为空");
        }
        if (header.getTotalAccounts() <= 0) {
            result.addWarning("[" + name() + "] 总户数未解析到或为0");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
