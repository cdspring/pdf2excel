package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.PowerDetail;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PowerTotalRule implements ValidationRule {

    @Override
    public String name() {
        return "电量合计校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillData data = context.getBillData();
        if (data.getHeader() == null || data.getHeader().getTotalPower() == null) {
            return;
        }
        List<PowerDetail> details = data.getPowerDetails();
        if (details == null || details.isEmpty()) {
            result.addWarning("[" + name() + "] 电量明细为空，无法校验合计");
            return;
        }

        BigDecimal sum = details.stream()
                .map(PowerDetail::getPower)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expected = data.getHeader().getTotalPower();
        BigDecimal diff = expected.subtract(sum).abs();
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal pct = expected.compareTo(BigDecimal.ZERO) != 0
                    ? diff.multiply(new BigDecimal("100")).divide(expected.abs(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String msg = String.format("[%s] 表头电量=%s, 明细合计=%s, 差异=%s (%.2f%%)",
                    name(), expected, sum, expected.subtract(sum), pct);
            if (pct.compareTo(new BigDecimal("1")) > 0) {
                result.addError(msg);
            } else {
                result.addWarning(msg);
            }
        }
    }
}
