package com.pdf2excel;
import com.pdf2excel.model.*;
import com.pdf2excel.service.PdfParseService;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
public class QuickCheck {
    public static void main(String[] args) throws Exception {
        PdfParseService svc = new PdfParseService();
        BillData data;
        try (FileInputStream fis = new FileInputStream(args[0])) { data = svc.parse(fis); }
        Set<Integer> seqs = new TreeSet<>();
        for (PowerDetail d : data.getPowerDetails()) seqs.add(d.getSeq());
        int max = Collections.max(seqs);
        List<Integer> missing = new ArrayList<>();
        for (int i = 1; i <= max; i++) { if (!seqs.contains(i)) missing.add(i); }
        System.out.printf("Parsed: %d, Max seq: %d, Missing: %s%n", seqs.size(), max, missing);
        // Also check: entries where power > 1000 and readings suggest high multiplier
        int highPower = 0;
        for (PowerDetail d : data.getPowerDetails()) {
            if (d.getPower() != null && d.getPower().compareTo(new BigDecimal("1000")) > 0) {
                highPower++;
                if (highPower <= 10) System.out.printf("  High power #%d: power=%s, prev=%s, curr=%s, mult=%d, meter=%s%n",
                    d.getSeq(), d.getPower(), d.getPrevReading(), d.getCurrReading(), d.getMultiplier(), d.getMeterId().substring(0, Math.min(18, d.getMeterId().length())));
            }
        }
        System.out.printf("Entries with power > 1000: %d%n", highPower);
    }
}
