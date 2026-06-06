package com.pdf2excel.validation;

import com.pdf2excel.validation.rules.*;

import java.util.Arrays;
import java.util.List;

public class DefaultRuleSet {

    public static List<ValidationRule> allRules() {
        return Arrays.asList(
                new HeaderRequiredFieldsRule(),
                new AccountCountRule(),
                new AccountIdFormatRule(),
                new PowerTotalRule(),
                new FeeTotalRule(),
                new NonNegativeAmountRule(),
                new DuplicateEntryRule(),
                new MeterReadingRule()
        );
    }

    public static BillValidator createDefaultValidator() {
        return new BillValidator().addRules(allRules());
    }
}
