package com.pdf2excel;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.ValidationResult;
import com.pdf2excel.service.ExcelWriterService;
import com.pdf2excel.service.PdfParseService;
import com.pdf2excel.validation.BillValidator;
import com.pdf2excel.validation.DefaultRuleSet;

import java.io.*;

/**
 * 命令行入口：将目录下所有 PDF 转换为 Excel
 * 用法: java -cp ... com.pdf2excel.ConvertRunner <pdf目录> [输出目录]
 */
public class ConvertRunner {

    public static void main(String[] args) {
        System.exit(run(args, System.out));
    }

    public static int run(String[] args, PrintStream out) {
        if (isHelp(args)) {
            printUsage(out);
            return 0;
        }
        if (args.length > 2) {
            out.println("参数过多");
            printUsage(out);
            return 1;
        }

        String inputDir = args.length > 0 ? args[0] : ".";
        String outputDir = args.length > 1 ? args[1] : inputDir;

        File inFolder = new File(inputDir);
        if (!inFolder.isDirectory()) {
            out.println("输入目录不存在或不是目录: " + inFolder.getAbsolutePath());
            return 1;
        }

        File outFolder = new File(outputDir);
        if (!outFolder.exists() && !outFolder.mkdirs()) {
            out.println("无法创建输出目录: " + outFolder.getAbsolutePath());
            return 1;
        }
        if (!outFolder.isDirectory()) {
            out.println("输出路径不是目录: " + outFolder.getAbsolutePath());
            return 1;
        }

        File[] pdfFiles = inFolder.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            out.println("未找到 PDF 文件: " + inFolder.getAbsolutePath());
            return 1;
        }

        PdfParseService parser = new PdfParseService();
        BillValidator validator = DefaultRuleSet.createDefaultValidator();
        ExcelWriterService writer = new ExcelWriterService();

        int processed = 0, warn = 0, fail = 0, validationErrors = 0;
        for (File pdf : pdfFiles) {
            String baseName = pdf.getName().replaceAll("(?i)\\.pdf$", "");
            File outFile = new File(outFolder, baseName + ".xlsx");
            out.print(pdf.getName() + " -> ");

            try (FileInputStream fis = new FileInputStream(pdf)) {
                BillData data = parser.parse(fis);
                ValidationResult vr = validator.validate(data, pdf.getName());
                data.setValidation(vr);

                writer.write(data, outFile);

                if (vr.isPassed() && vr.getWarnings().isEmpty()) {
                    out.println("OK  " + outFile.getName());
                } else if (vr.isPassed()) {
                    out.println("WARN  " + outFile.getName());
                    vr.getWarnings().forEach(w -> out.println("  [WARN]  " + w));
                    warn++;
                } else {
                    out.println("ERROR  " + outFile.getName());
                    vr.getErrors().forEach(e -> out.println("  [ERROR] " + e));
                    vr.getWarnings().forEach(w -> out.println("  [WARN]  " + w));
                    validationErrors++;
                }
                processed++;
            } catch (Exception e) {
                out.println("FAIL  " + e.getMessage());
                fail++;
            }
        }

        out.printf("%n汇总: %d 处理, %d 警告, %d 校验错误, %d 运行失败, 共 %d 个文件%n",
                processed, warn, validationErrors, fail, pdfFiles.length);
        if (fail > 0) {
            return 1;
        }
        if (validationErrors > 0) {
            return 2;
        }
        return 0;
    }

    private static boolean isHelp(String[] args) {
        return args != null && args.length > 0
                && ("-h".equalsIgnoreCase(args[0]) || "--help".equalsIgnoreCase(args[0]));
    }

    private static void printUsage(PrintStream out) {
        out.println("ConvertRunner: 批量将 PDF 转为 Excel");
        out.println("用法: java -jar pdf2excel-1.0.0.jar convert <pdf目录> [输出目录]");
        out.println("说明: 未提供输出目录时，默认写回输入目录。");
    }
}
