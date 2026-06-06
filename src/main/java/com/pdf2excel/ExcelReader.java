package com.pdf2excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

public class ExcelReader {
    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "D:/pdf2excel/2026.3月电信集团户账单明细(2).xlsx";
        FileInputStream fis = new FileInputStream(path);
        Workbook wb = new XSSFWorkbook(fis);
        for (int s = 0; s < wb.getNumberOfSheets(); s++) {
            Sheet sheet = wb.getSheetAt(s);
            System.out.println("=== Sheet: " + sheet.getSheetName() + " ===");
            int maxRows = Math.min(sheet.getLastRowNum() + 1, 15);
            for (int r = 0; r < maxRows; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Row %2d: ", r));
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) { sb.append("| "); continue; }
                    CellType type = cell.getCellType();
                    if (type == CellType.STRING) sb.append("|").append(cell.getStringCellValue());
                    else if (type == CellType.NUMERIC) sb.append("|").append(cell.getNumericCellValue());
                    else if (type == CellType.BLANK) sb.append("| ");
                    else sb.append("|?");
                }
                System.out.println(sb.toString());
            }
        }
        wb.close();
    }
}
