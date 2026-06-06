package com.pdf2excel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;

public class PdfTextDumper {

    public static void main(String[] args) throws Exception {
        String filePath = args.length > 0 ? args[0] : "D:/pdf2excel/金牛小电信账单.pdf";
        File pdf = new File(filePath);

        PDDocument doc = PDDocument.load(new FileInputStream(pdf));
        int totalPages = doc.getNumberOfPages();
        System.out.println("Total pages: " + totalPages);
        System.out.println();

        int startPage = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int endPage = args.length > 2 ? Integer.parseInt(args[2]) : Math.min(totalPages, startPage + 3);
        for (int i = startPage; i <= endPage; i++) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            String text = stripper.getText(doc);
            System.out.println("=== PAGE " + i + " ===");
            String[] lines = text.split("\n");
            for (int j = 0; j < lines.length; j++) {
                System.out.printf("%3d: %s%n", j + 1, lines[j]);
            }
            System.out.println();
        }
        doc.close();
    }
}
