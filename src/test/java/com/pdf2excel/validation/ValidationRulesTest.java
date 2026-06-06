package com.pdf2excel.validation;

import com.pdf2excel.model.*;
import com.pdf2excel.validation.rules.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesTest {

    @Test
    void headerRequiredFieldsRule_reportsMissingFields() {
        BillData data = new BillData();
        BillHeader header = new BillHeader();
        header.setTotalAccounts(0);
        data.setHeader(header);

        ValidationResult result = validateWith(new HeaderRequiredFieldsRule(), data);

        assertFalse(result.isPassed());
        assertTrue(result.getErrors().stream().anyMatch(s -> s.contains("本期电量为空")));
        assertTrue(result.getErrors().stream().anyMatch(s -> s.contains("本期电费为空")));
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("集团户编号为空")));
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("集团户名称为空")));
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("总户数未解析到或为0")));
    }

    @Test
    void accountCountRule_reportsMismatchForPowerAndFeeDetails() {
        BillData data = billDataWithHeader(3, new BigDecimal("100"), new BigDecimal("200"));
        data.setPowerDetails(Arrays.asList(power(1, "1234567"), power(2, "1234568")));
        data.setFeeDetails(Collections.singletonList(fee(1, "1234567")));

        ValidationResult result = validateWith(new AccountCountRule(), data);

        assertTrue(result.isPassed());
        assertEquals(2, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("电量明细户数=2"));
        assertTrue(result.getWarnings().get(1).contains("电费明细户数=1"));
    }

    @Test
    void accountIdFormatRule_reportsInvalidAccountIds() {
        BillData data = billDataWithHeader(1, new BigDecimal("10"), new BigDecimal("20"));
        PowerDetail power = power(1, "12A");
        FeeDetail fee = fee(1, "123");
        data.setPowerDetails(Collections.singletonList(power));
        data.setFeeDetails(Collections.singletonList(fee));

        ValidationResult result = validateWith(new AccountIdFormatRule(), data);

        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("电量明细中有1条户号格式异常")));
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("电费明细中有1条户号格式异常")));
    }

    @Test
    void duplicateEntryRule_reportsDuplicates() {
        BillData data = billDataWithHeader(2, new BigDecimal("10"), new BigDecimal("20"));
        PowerDetail p1 = power(1, "1234567");
        p1.setMeterId("M1");
        PowerDetail p2 = power(2, "1234567");
        p2.setMeterId("M1");
        data.setPowerDetails(Arrays.asList(p1, p2));
        data.setFeeDetails(Arrays.asList(fee(1, "1234567"), fee(2, "1234567")));

        ValidationResult result = validateWith(new DuplicateEntryRule(), data);

        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("电量明细中发现1条重复")));
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("电费明细中发现1条重复户号")));
    }

    @Test
    void meterReadingRule_reportsReadingAndPowerMismatch() {
        BillData data = billDataWithHeader(1, new BigDecimal("10"), new BigDecimal("20"));
        PowerDetail detail = power(1, "1234567");
        detail.setPrevReading(new BigDecimal("100"));
        detail.setCurrReading(new BigDecimal("110"));
        detail.setMultiplier(2);
        detail.setLineLoss(new BigDecimal("1"));
        detail.setTransformerLoss(new BigDecimal("1"));
        detail.setRefund(BigDecimal.ZERO);
        detail.setPower(new BigDecimal("30"));
        data.setPowerDetails(Collections.singletonList(detail));

        ValidationResult result = validateWith(new MeterReadingRule(), data);

        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("差异较大")));
    }

    @Test
    void meterReadingRule_reportsReadingReversal() {
        BillData data = billDataWithHeader(1, new BigDecimal("10"), new BigDecimal("20"));
        PowerDetail detail = power(1, "1234567");
        detail.setPrevReading(new BigDecimal("100"));
        detail.setCurrReading(new BigDecimal("90"));
        detail.setMultiplier(2);
        detail.setPower(new BigDecimal("10"));
        data.setPowerDetails(Collections.singletonList(detail));

        ValidationResult result = validateWith(new MeterReadingRule(), data);

        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("本期示数(90) < 上期示数(100)")));
    }

    @Test
    void feeTotalRule_explainsSmallFeeDifferenceAsWarning() {
        BillData data = billDataWithHeader(1, BigDecimal.ONE, new BigDecimal("100.00"));
        FeeDetail fee = fee(1, "1234567");
        fee.setFee(new BigDecimal("99.50"));
        data.setFeeDetails(Collections.singletonList(fee));

        ValidationResult result = validateWith(new FeeTotalRule(), data);

        assertTrue(result.isPassed());
        assertTrue(result.getWarnings().stream()
                .anyMatch(s -> s.contains("费用口径差异")));
    }

    @Test
    void nonNegativeAmountRule_reportsNegativeAmounts() {
        BillData data = billDataWithHeader(1, new BigDecimal("10"), new BigDecimal("20"));
        PowerDetail detail = power(1, "1234567");
        detail.setPower(new BigDecimal("-1"));
        FeeDetail fee = fee(1, "1234567");
        fee.setFee(new BigDecimal("-2"));
        data.setPowerDetails(Collections.singletonList(detail));
        data.setFeeDetails(Collections.singletonList(fee));

        ValidationResult result = validateWith(new NonNegativeAmountRule(), data);

        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("电量为负")));
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("电费为负")));
    }

    @Test
    void defaultRuleSetContainsAllExpectedRules() {
        assertEquals(8, DefaultRuleSet.allRules().size());
    }

    private ValidationResult validateWith(ValidationRule rule, BillData data) {
        BillValidator validator = new BillValidator().addRule(rule);
        return validator.validate(data, "sample.pdf");
    }

    private BillData billDataWithHeader(int totalAccounts, BigDecimal totalPower, BigDecimal totalFee) {
        BillHeader header = new BillHeader();
        header.setGroupId("G001");
        header.setGroupName("测试集团");
        header.setTotalAccounts(totalAccounts);
        header.setTotalPower(totalPower);
        header.setTotalFee(totalFee);

        BillData data = new BillData();
        data.setHeader(header);
        return data;
    }

    private PowerDetail power(int seq, String accountId) {
        PowerDetail detail = new PowerDetail();
        detail.setSeq(seq);
        detail.setAccountId(accountId);
        detail.setAccountName("用户" + seq);
        detail.setMeterId("M" + seq);
        detail.setPower(BigDecimal.ONE);
        return detail;
    }

    private FeeDetail fee(int seq, String accountId) {
        FeeDetail detail = new FeeDetail();
        detail.setSeq(seq);
        detail.setAccountId(accountId);
        detail.setAccountName("用户" + seq);
        detail.setFee(BigDecimal.ONE);
        return detail;
    }
}
