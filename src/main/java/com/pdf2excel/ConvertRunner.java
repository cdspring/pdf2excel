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

    public static void main(String[] args) throws Exception {
        String inputDir = args.length > 0 ? args[0] : ".";
        String outputDir = args.length > 1 ? args[1] : inputDir;

        File inFolder = new File(inputDir);
        File outFolder = new File(outputDir);
        if (!outFolder.exists()) outFolder.mkdirs();

        File[] pdfFiles = inFolder.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("未找到 PDF 文件: " + inFolder.getAbsolutePath());
            return;
        }

        PdfParseService parser = new PdfParseService();
        BillValidator validator = DefaultRuleSet.createDefaultValidator();
        ExcelWriterService writer = new ExcelWriterService();

        int ok = 0, fail = 0;
        for (File pdf : pdfFiles) {
            String baseName = pdf.getName().replaceAll("(?i)\\.pdf$", "");
            File outFile = new File(outFolder, baseName + ".xlsx");
            System.out.print(pdf.getName() + " -> ");

            try (FileInputStream fis = new FileInputStream(pdf)) {
                BillData data = parser.parse(fis);
                ValidationResult vr = validator.validate(data, pdf.getName());
                data.setValidation(vr);

                writer.write(data, outFile);

                if (vr.isPassed()) {
                    System.out.println("OK  " + outFile.getName());
                } else {
                    System.out.println("WARN(验证不通过，差异行已标红)  " + outFile.getName());
                    vr.getErrors().forEach(e -> System.out.println("  [ERROR] " + e));
                }
                ok++;
            } catch (Exception e) {
                System.out.println("FAIL  " + e.getMessage());
                fail++;
            }
        }

        System.out.printf("%n汇总: %d 成功, %d 失败, 共 %d 个文件%n", ok, fail, pdfFiles.length);
    }
}
