package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.FeeDetail;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class FeeTotalRule implements ValidationRule {

    @Override
    public String name() {
        return "电费合计校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillData data = context.getBillData();
        if (data.getHeader() == null || data.getHeader().getTotalFee() == null) {
            return;
        }
        List<FeeDetail> details = data.getFeeDetails();
        if (details == null || details.isEmpty()) {
            result.addWarning("[" + name() + "] 电费明细为空，无法校验合计");
            return;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (FeeDetail d : details) {
            // 当前解析器稳定提取的是主电费与容量费；其余费用列尚未可靠拆出，
            // 校验口径先与当前解析结果保持一致，避免把未确认字段重复计入。
            if (d.getFee() != null) sum = sum.add(d.getFee());
            if (d.getCapacityFee() != null) sum = sum.add(d.getCapacityFee());
        }

        BigDecimal expected = data.getHeader().getTotalFee();
        BigDecimal diff = expected.subtract(sum).abs();
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal pct = expected.compareTo(BigDecimal.ZERO) != 0
                    ? diff.multiply(new BigDecimal("100")).divide(expected.abs(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String msg = String.format("[%s] 表头电费=%s, 明细合计=%s, 差异=%s (%.2f%%)",
                    name(), expected, sum, expected.subtract(sum), pct);
            if (pct.compareTo(new BigDecimal("1")) > 0) {
                result.addError(msg);
            } else {
                result.addWarning(msg);
            }
        }
    }
}
