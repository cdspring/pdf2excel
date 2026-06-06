package com.pdf2excel;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.ValidationResult;
import com.pdf2excel.service.PdfParseService;
import com.pdf2excel.validation.BillValidator;
import com.pdf2excel.validation.DefaultRuleSet;

import java.io.*;

public class ValidationRunner {

    public static void main(String[] args) throws Exception {
        System.exit(run(args, System.out));
    }

    public static int run(String[] args, PrintStream console) throws Exception {
        if (isHelp(args)) {
            printUsage(console);
            return 0;
        }
        if (args.length > 2) {
            console.println("参数过多");
            printUsage(console);
            return 1;
        }

        String dir = args.length > 0 ? args[0] : ".";
        String outputFile = args.length > 1 ? args[1] : null;

        File folder = new File(dir);
        if (!folder.isDirectory()) {
            console.println("输入目录不存在或不是目录: " + folder.getAbsolutePath());
            return 1;
        }

        File[] pdfFiles = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));

        if (pdfFiles == null || pdfFiles.length == 0) {
            console.println("未找到 PDF 文件: " + folder.getAbsolutePath());
            return 1;
        }

        PdfParseService parseService = new PdfParseService();
        BillValidator validator = DefaultRuleSet.createDefaultValidator();

        PrintStream out = console;
        if (outputFile != null) {
            File report = new File(outputFile);
            File parent = report.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                console.println("无法创建报告目录: " + parent.getAbsolutePath());
                return 1;
            }
            if (report.exists() && report.isDirectory()) {
                console.println("报告路径是目录: " + report.getAbsolutePath());
                return 1;
            }
            out = new PrintStream(new FileOutputStream(report), true, "UTF-8");
        }

        out.println("========================================");
        out.println("PDF 账单验证报告");
        out.println("目录: " + folder.getAbsolutePath());
        out.println("文件数: " + pdfFiles.length);
        out.println("========================================\n");

        int passCount = 0;
        int warningPassCount = 0;
        int validationErrorCount = 0;
        int parseFailCount = 0;

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
                        validationErrorCount++;
                        for (String err : result.getErrors()) {
                            out.println("  [ERROR] " + err);
                        }
                    } else {
                        passCount++;
                        warningPassCount++;
                        out.println("  [PASS-WARN] 校验通过，存在需复核的业务警告");
                    }
                    for (String warn : result.getWarnings()) {
                        out.println("  [WARN]  " + warn);
                    }
                }
            } catch (Exception e) {
                out.println("  [FAIL] 解析异常: " + e.getMessage());
                parseFailCount++;
            }
            out.println();
        }

        out.println("========================================");
        out.println(String.format("汇总: %d 通过, %d 带警告通过, %d 校验错误, %d 解析失败, 共 %d 个文件",
                passCount, warningPassCount, validationErrorCount, parseFailCount, pdfFiles.length));
        out.println("========================================");

        if (outputFile != null) {
            out.close();
            console.println("验证报告已写入: " + outputFile);
        }
        if (parseFailCount > 0) {
            return 1;
        }
        if (validationErrorCount > 0) {
            return 2;
        }
        return 0;
    }

    private static boolean isHelp(String[] args) {
        return args != null && args.length > 0
                && ("-h".equalsIgnoreCase(args[0]) || "--help".equalsIgnoreCase(args[0]));
    }

    private static void printUsage(PrintStream out) {
        out.println("ValidationRunner: 批量校验目录下的 PDF 账单");
        out.println("用法: java -jar pdf2excel-1.0.0.jar validate <pdf目录> [报告文件]");
        out.println("说明: 提供报告文件时会写入 UTF-8 文本报告。");
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
