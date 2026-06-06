package com.pdf2excel.validation.rules;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.PowerDetail;
import com.pdf2excel.validation.ValidationContext;
import com.pdf2excel.validation.ValidationRule;
import com.pdf2excel.model.ValidationResult;

import java.math.BigDecimal;

public class MeterReadingRule implements ValidationRule {

    @Override
    public String name() {
        return "抄表读数逻辑校验";
    }

    @Override
    public void validate(ValidationContext context, ValidationResult result) {
        BillData data = context.getBillData();
        if (data.getPowerDetails() == null) return;

        for (PowerDetail d : data.getPowerDetails()) {
            if (d.getPrevReading() == null || d.getCurrReading() == null) continue;

            if (d.getCurrReading().compareTo(d.getPrevReading()) < 0) {
                result.addWarning(String.format(
                        "[%s] 序号%d: 本期示数(%s) < 上期示数(%s)，可能存在翻转或数据错误",
                        name(), d.getSeq(), d.getCurrReading(), d.getPrevReading()));
            }

            if (d.getMultiplier() > 0 && d.getPower() != null) {
                BigDecimal diff = d.getCurrReading().subtract(d.getPrevReading());
                BigDecimal expectedPower = diff.multiply(BigDecimal.valueOf(d.getMultiplier()));
                BigDecimal actualWithLoss = d.getPower();
                if (d.getLineLoss() != null) actualWithLoss = actualWithLoss.subtract(d.getLineLoss());
                if (d.getTransformerLoss() != null) actualWithLoss = actualWithLoss.subtract(d.getTransformerLoss());
                if (d.getRefund() != null) actualWithLoss = actualWithLoss.subtract(d.getRefund());

                if (expectedPower.compareTo(BigDecimal.ZERO) > 0
                        && actualWithLoss.subtract(expectedPower).abs().compareTo(BigDecimal.ONE) > 0) {
                    result.addWarning(String.format(
                            "[%s] 序号%d: (本期-上期)*倍率=%s, 电量-线损-变损-退补=%s, 差异较大",
                            name(), d.getSeq(), expectedPower, actualWithLoss));
                }
            }
        }
    }
}
