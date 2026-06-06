package com.pdf2excel;

import com.pdf2excel.model.BillData;
import com.pdf2excel.model.PowerDetail;
import com.pdf2excel.model.FeeDetail;
import com.pdf2excel.service.PdfParseService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;

public class ParseDebugger {

    public static void main(String[] args) throws IOException {
        String filePath = args.length > 0 ? args[0] : "D:/pdf2excel/金牛小电信账单.pdf";
        String mode = args.length > 1 ? args[1] : "normal";

        PdfParseService service = new PdfParseService();
        BillData data;
        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            data = service.parse(fis);
        }

        if ("outliers".equals(mode)) {
            System.out.println("=== Power entries with power > 10000 ===");
            if (data.getPowerDetails() != null) {
                BigDecimal sum = BigDecimal.ZERO;
                int outlierCount = 0;
                for (PowerDetail d : data.getPowerDetails()) {
                    if (d.getPower() != null) {
                        sum = sum.add(d.getPower());
                        if (d.getPower().compareTo(new BigDecimal("10000")) > 0) {
                            outlierCount++;
                            if (outlierCount <= 20) {
                                System.out.printf("  #%d: id=%s, meter=%s, power=%s, prev=%s, curr=%s, mult=%d%n",
                                        d.getSeq(), d.getAccountId(), d.getMeterId(),
                                        d.getPower(), d.getPrevReading(), d.getCurrReading(), d.getMultiplier());
                            }
                        }
                    }
                }
                System.out.printf("  Total power sum: %s, outliers(>10000): %d/%d%n",
                        sum, outlierCount, data.getPowerDetails().size());
            }
        } else {
            int maxEntries = 10;
            System.out.println("=== Power Details (first " + maxEntries + ") ===");
            if (data.getPowerDetails() != null) {
                for (int i = 0; i < Math.min(maxEntries, data.getPowerDetails().size()); i++) {
                    PowerDetail d = data.getPowerDetails().get(i);
                    System.out.printf("  #%d: id=%s, meter=%s, power=%s, prev=%s, curr=%s, mult=%d%n",
                            d.getSeq(), d.getAccountId(), d.getMeterId(),
                            d.getPower(), d.getPrevReading(), d.getCurrReading(), d.getMultiplier());
                }
            }
        }
    }
}
