package com.pdf2excel;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.ValidationResult;
import com.pdf2excel.service.PdfParseService;
import com.pdf2excel.validation.BillValidator;
import com.pdf2excel.validation.DefaultRuleSet;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ValidationRunner {

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : ".";
        String outputFile = args.length > 1 ? args[1] : null;

        File folder = new File(dir);
        File[] pdfFiles = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));

        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("未找到 PDF 文件: " + folder.getAbsolutePath());
            return;
        }

        PdfParseService parseService = new PdfParseService();
        BillValidator validator = DefaultRuleSet.createDefaultValidator();

        PrintStream out = System.out;
        if (outputFile != null) {
            out = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
        }

        out.println("========================================");
        out.println("PDF 账单验证报告");
        out.println("目录: " + folder.getAbsolutePath());
        out.println("文件数: " + pdfFiles.length);
        out.println("========================================\n");

        int passCount = 0;
        int failCount = 0;

        for (File pdf : pdfFiles) {
            out.println("--- " + pdf.getName() + " ---");
            try (FileInputStream fis = new FileInputStream(pdf)) {
                BillData data = parseService.parse(fis);

                printParseSummary(data, out);

                ValidationResult result = validator.validate(data, pdf.getName());

                if (result.isPassed() && result.getWarnings().isEmpty()) {
                    out.println("  [PASS] 所有校验通过");
                    passCount++;
                } else {
                    if (!result.isPassed()) {
                        failCount++;
                        for (String err : result.getErrors()) {
                            out.println("  [ERROR] " + err);
                        }
                    } else {
                        passCount++;
                    }
                    for (String warn : result.getWarnings()) {
                        out.println("  [WARN]  " + warn);
                    }
                }
            } catch (IOException e) {
                out.println("  [FAIL] 解析异常: " + e.getMessage());
                failCount++;
            }
            out.println();
        }

        out.println("========================================");
        out.println(String.format("汇总: %d 通过, %d 失败, 共 %d 个文件",
                passCount, failCount, pdfFiles.length));
        out.println("========================================");

        if (outputFile != null) {
            out.close();
            System.out.println("验证报告已写入: " + outputFile);
        }
    }

    private static void printParseSummary(BillData data, PrintStream out) {
        if (data.getHeader() != null) {
            out.println(String.format("  表头: 集团户=%s, 电量=%s千瓦时, 电费=%s元, 总户数=%d",
                    data.getHeader().getGroupName(),
                    data.getHeader().getTotalPower(),
                    data.getHeader().getTotalFee(),
                    data.getHeader().getTotalAccounts()));
        }
        int powerCount = data.getPowerDetails() != null ? data.getPowerDetails().size() : 0;
        int feeCount = data.getFeeDetails() != null ? data.getFeeDetails().size() : 0;
        out.println(String.format("  解析: 电量明细%d条, 电费明细%d条", powerCount, feeCount));
    }
}
