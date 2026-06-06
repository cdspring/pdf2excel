package com.pdf2excel.integration;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.ValidationResult;
import com.pdf2excel.service.PdfParseService;
import com.pdf2excel.validation.BillValidator;
import com.pdf2excel.validation.DefaultRuleSet;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PdfPipelineIntegrationTest {

    @Test
    void knownPassSample_shouldParseAndValidateCleanly() throws Exception {
        BillData data = parse("电信2026年2月电费账单.pdf");
        ValidationResult result = validate(data);

        assertEquals(221, data.getHeader().getTotalAccounts());
        assertEquals(221, data.getPowerDetails().size());
        assertEquals(221, data.getFeeDetails().size());
        assertTrue(result.isPassed());
        assertTrue(result.getWarnings().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void knownFeeMismatchSample_shouldFlagFeeError() throws Exception {
        BillData data = parse("统一账单  03月电信.pdf");
        ValidationResult result = validate(data);

        assertEquals(494, data.getHeader().getTotalAccounts());
        assertEquals(498, data.getPowerDetails().size());
        assertEquals(494, data.getFeeDetails().size());
        assertTrue(result.isPassed());
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("电费合计校验")));
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void anomalySample_shouldStillExposeReadingWarning() throws Exception {
        BillData data = parse("金牛小电信账单.pdf");
        ValidationResult result = validate(data);

        assertEquals(370, data.getHeader().getTotalAccounts());
        assertEquals(371, data.getPowerDetails().size());
        assertEquals(370, data.getFeeDetails().size());
        assertTrue(result.getWarnings().stream().anyMatch(s -> s.contains("抄表读数逻辑校验")));
    }

    private BillData parse(String fileName) throws Exception {
        Path path = Paths.get(fileName);
        assertTrue(Files.exists(path), "Missing sample file: " + path.toAbsolutePath());

        PdfParseService parser = new PdfParseService();
        try (InputStream in = Files.newInputStream(path)) {
            return parser.parse(in);
        }
    }

    private ValidationResult validate(BillData data) {
        BillValidator validator = DefaultRuleSet.createDefaultValidator();
        ValidationResult result = validator.validate(data, "sample.pdf");
        data.setValidation(result);
        return result;
    }
}
