package com.pdf2excel.service;

import com.pdf2excel.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 输出格式完全模仿参考 Excel（原总表）：
 * - 单 Sheet "原总表"
 * - R1-10: 账单头部
 * - R11起: 电量明细（A:F | G:M | N:P | Q:R 合并表头）
 * - 分隔行后: 电费明细（E:H | I:K | L:M | N:O | P:Q 合并表头）
 * - 验证不通过的行标红
 */
public class ExcelWriterService {

    // 列索引常量（0-based）
    private static final int COL_A = 0, COL_B = 1, COL_C = 2, COL_D = 3,
            COL_E = 4, COL_F = 5, COL_G = 6, COL_H = 7, COL_I = 8,
            COL_J = 9, COL_K = 10, COL_L = 11, COL_M = 12, COL_N = 13,
            COL_O = 14, COL_P = 15, COL_Q = 16, COL_R = 17,
            COL_S = 18, COL_T = 19, COL_U = 20;

    public void write(BillData data, File outputFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("原总表");
            setColumnWidths(sheet);

            CellStyle baseStyle   = baseStyle(wb);
            CellStyle headerStyle = headerStyle(wb);
            CellStyle titleStyle  = titleStyle(wb);
            CellStyle errorStyle  = errorStyle(wb);

            BillHeader h = data.getHeader();
            ValidationResult vr = data.getValidation();

            // ── 头部 R1-10 ──────────────────────────────────────────
            int row = writeHeader(sheet, h, titleStyle, baseStyle);

            // ── 电量明细 ─────────────────────────────────────────────
            // 标题行
            Row titleRow = sheet.createRow(row++);
            titleRow.setHeightInPoints(20);
            mergedCell(sheet, titleRow, COL_G, COL_M, "用电明细列表", headerStyle);
            mergedCell(sheet, titleRow, COL_N, COL_P, "单位：千瓦时", baseStyle);

            // 表头行
            Row powerHeader = sheet.createRow(row++);
            powerHeader.setHeightInPoints(22.5f);
            mergedCell(sheet, powerHeader, COL_A, COL_F,
                    "序号    户号        户名     电能表编号     上期示数", headerStyle);
            mergedCell(sheet, powerHeader, COL_G, COL_M,
                    "本期示数    倍率    线损    变损    退补    电量", headerStyle);
            mergedCell(sheet, powerHeader, COL_N, COL_P, "电量", headerStyle);
            mergedCell(sheet, powerHeader, COL_Q, COL_R, "备注", headerStyle);

            // 数据行
            int powerHeaderRow = row;
            List<PowerDetail> powers = data.getPowerDetails();
            if (powers != null) {
                for (PowerDetail d : powers) {
                    Row r = sheet.createRow(row++);
                    r.setHeightInPoints(42f);
                    boolean err = hasError(vr, d.getAccountId());
                    CellStyle s = err ? errorStyle : baseStyle;

                    // A列：序号
                    cell(r, COL_A, d.getSeq(), s);
                    // B列：户号
                    cell(r, COL_B, d.getAccountId(), s);
                    // C列：户名
                    cell(r, COL_C, d.getAccountName(), s);
                    // D列：电能表编号
                    cell(r, COL_D, d.getMeterId(), s);
                    // E:F 合并：上期示数
                    mergedCell(sheet, r, COL_E, COL_F, d.getPrevReading(), s);
                    // G:M 合并：本期示数 倍率 线损 变损 退补
                    String gVal = join(d.getCurrReading(), d.getMultiplier(),
                            d.getLineLoss(), d.getTransformerLoss(), d.getRefund());
                    mergedCell(sheet, r, COL_G, COL_M, gVal, s);
                    // N:P 合并：电量
                    mergedCell(sheet, r, COL_N, COL_P, d.getPower(), s);
                    // Q:R 合并：备注
                    mergedCell(sheet, r, COL_Q, COL_R, nvl(d.getRemark()), s);
                }
            }

            // ── 电费明细 ─────────────────────────────────────────────
            // 空行分隔
            sheet.createRow(row++).setHeightInPoints(10f);

            // 标题行
            Row feeTitle = sheet.createRow(row++);
            feeTitle.setHeightInPoints(20f);
            mergedCell(sheet, feeTitle, COL_E, COL_H, "费用明细列表", headerStyle);
            mergedCell(sheet, feeTitle, COL_N, COL_P, "单位：元", baseStyle);

            // 表头行
            Row feeHeader = sheet.createRow(row++);
            feeHeader.setHeightInPoints(22.5f);
            cell(feeHeader, COL_A, "序号", headerStyle);
            cell(feeHeader, COL_B, "账号", headerStyle);
            cell(feeHeader, COL_C, "单位名称", headerStyle);
            cell(feeHeader, COL_D, "供电单位", headerStyle);
            mergedCell(sheet, feeHeader, COL_E, COL_H, "用电地址", headerStyle);
            mergedCell(sheet, feeHeader, COL_I, COL_K, "电费(元)", headerStyle);
            mergedCell(sheet, feeHeader, COL_L, COL_M, "容量费(元)", headerStyle);
            mergedCell(sheet, feeHeader, COL_N, COL_O, "农网维护费(元)", headerStyle);
            mergedCell(sheet, feeHeader, COL_P, COL_Q, "政府性基金(元)", headerStyle);
            cell(feeHeader, COL_R, "备注", headerStyle);

            // 数据行
            List<FeeDetail> fees = data.getFeeDetails();
            if (fees != null) {
                for (FeeDetail d : fees) {
                    Row r = sheet.createRow(row++);
                    r.setHeightInPoints(42f);
                    boolean err = hasError(vr, d.getAccountId());
                    CellStyle s = err ? errorStyle : baseStyle;

                    cell(r, COL_A, d.getSeq(), s);
                    cell(r, COL_B, d.getAccountId(), s);
                    cell(r, COL_C, d.getAccountName(), s);
                    cell(r, COL_D, d.getPowerSupplyUnit(), s);
                    mergedCell(sheet, r, COL_E, COL_H, d.getAddress(), s);
                    mergedCell(sheet, r, COL_I, COL_K, d.getFee(), s);
                    mergedCell(sheet, r, COL_L, COL_M, d.getCapacityFee(), s);
                    mergedCell(sheet, r, COL_N, COL_O, d.getRuralMaintenanceFee(), s);
                    mergedCell(sheet, r, COL_P, COL_Q, d.getGovernmentFund(), s);
                    cell(r, COL_R, nvl(d.getRemark()), s);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                wb.write(fos);
            }
        }
    }

    /** 写账单头部 R1-10，返回下一个可用行号（0-based） */
    private int writeHeader(Sheet sheet, BillHeader h, CellStyle titleStyle, CellStyle base) {
        String groupName  = h != null ? nvl(h.getGroupName())  : "";
        String groupId    = h != null ? nvl(h.getGroupId())    : "";
        String periodStart= h != null ? nvl(h.getBillPeriodStart()) : "";
        String periodEnd  = h != null ? nvl(h.getBillPeriodEnd())   : "";
        String address    = h != null ? nvl(h.getAddress())    : "";
        String meterDate  = h != null ? nvl(h.getMeterDate())  : "";
        String printDate  = h != null ? nvl(h.getPrintDate())  : "";
        int    total      = h != null ? h.getTotalAccounts()   : 0;
        String power      = h != null ? nvl(h.getTotalPower()) : "";
        String fee        = h != null ? nvl(h.getTotalFee())   : "";
        String avgPrice   = h != null ? nvl(h.getAvgPrice())   : "";

        // R1: 公司名称
        Row r1 = sheet.createRow(0); r1.setHeightInPoints(26f);
        mergedCell(sheet, r1, COL_A, COL_U, "国 网 四 川 省 电 力 公 司", titleStyle);

        // R2: 账单标题
        Row r2 = sheet.createRow(1); r2.setHeightInPoints(16.6f);
        mergedCell(sheet, r2, COL_G, COL_N, "电 费 账 单", titleStyle);

        // R3: 账单编号 | 集团客户编号 | 账单顺序账户号
        Row r3 = sheet.createRow(2); r3.setHeightInPoints(10.5f);
        cell(r3, COL_B, "账单编号", base);
        mergedCell(sheet, r3, COL_F, COL_O, "集团客户编号  " + groupId, base);
        cell(r3, COL_P, "账单顺序账户号", base);

        // R4: 账期开始 | 集团客户名称
        Row r4 = sheet.createRow(3); r4.setHeightInPoints(12.2f);
        cell(r4, COL_B, periodStart, base);
        mergedCell(sheet, r4, COL_F, COL_O, "集团客户名称  " + groupName, base);
        cell(r4, COL_P, "集团客户名称", base);
        mergedCell(sheet, r4, COL_T, COL_U, groupName, base);

        // R5: 单位
        Row r5 = sheet.createRow(4); r5.setHeightInPoints(11.45f);
        cell(r5, COL_C, "元", base);

        // R6: 账期结束 | 账单地址 | 总户数
        Row r6 = sheet.createRow(5); r6.setHeightInPoints(10.5f);
        cell(r6, COL_B, periodEnd, base);
        mergedCell(sheet, r6, COL_G, COL_P, "账单地址  " + address, base);
        cell(r6, COL_Q, "总汇总", base);
        cell(r6, COL_T, total, base);

        // R7: 打印日期
        Row r7 = sheet.createRow(6); r7.setHeightInPoints(10.5f);
        mergedCell(sheet, r7, COL_K, COL_P, "账单打印日期：" + printDate, base);

        // R8: 本期电量 | 本期电费 | 抄表日期
        Row r8 = sheet.createRow(7); r8.setHeightInPoints(12f);
        mergedCell(sheet, r8, COL_A, COL_F,
                "本期电量              " + power + "千瓦时", base);
        mergedCell(sheet, r8, COL_G, COL_P,
                "本期电费                    " + fee + "元", base);
        mergedCell(sheet, r8, COL_Q, COL_U,
                "抄表日期                 " + meterDate, base);

        // R9: 账单说明
        Row r9 = sheet.createRow(8); r9.setHeightInPoints(28.9f);
        mergedCell(sheet, r9, COL_A, COL_S,
                "账单说明（单位：千瓦时、千乏时、千瓦，元）", base);
        mergedCell(sheet, r9, COL_T, COL_U, "综合费率", base);

        // R10: 平均电价
        Row r10 = sheet.createRow(9); r10.setHeightInPoints(20.25f);
        mergedCell(sheet, r10, COL_A, COL_F,
                "平均电价：" + avgPrice + "元/千瓦时", base);

        return 10; // 下一行从第11行（index=10）开始
    }

    // ── 样式 ────────────────────────────────────────────────────────

    private CellStyle baseStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setWrapText(true);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = baseStyle(wb);
        s.setAlignment(HorizontalAlignment.CENTER);
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        return s;
    }

    private CellStyle titleStyle(Workbook wb) {
        CellStyle s = headerStyle(wb);
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        return s;
    }

    private CellStyle errorStyle(Workbook wb) {
        CellStyle s = baseStyle(wb);
        s.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    // ── 列宽（模仿参考 Excel）───────────────────────────────────────

    private void setColumnWidths(Sheet sheet) {
        // 列宽单位：1/256 字符宽，约 256*字符数
        double[] chars = {
            3.6, 10.9, 9.1, 11.9, 5.2, 2.0, 7.6, 7.0,
            2.1, 2.8, 3.7, 3.1, 2.4, 2.1, 2.5, 3.2,
            1.7, 3.5, 0.9, 11.8, 11.9
        };
        for (int i = 0; i < chars.length; i++) {
            sheet.setColumnWidth(i, (int)(chars[i] * 256));
        }
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    private void mergedCell(Sheet sheet, Row row, int c1, int c2, Object value, CellStyle style) {
        Cell cell = row.createCell(c1);
        setValue(cell, value);
        if (style != null) cell.setCellStyle(style);
        if (c1 < c2) {
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), c1, c2));
            for (int c = c1 + 1; c <= c2; c++) {
                Cell blank = row.createCell(c);
                if (style != null) blank.setCellStyle(style);
            }
        }
    }

    private void cell(Row row, int col, Object value, CellStyle style) {
        Cell c = row.createCell(col);
        setValue(c, value);
        if (style != null) c.setCellStyle(style);
    }

    private void setValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /** 拼接电量明细 G 列：本期示数 倍率 线损 变损 退补 */
    private String join(Object... vals) {
        StringBuilder sb = new StringBuilder();
        for (Object v : vals) {
            if (sb.length() > 0) sb.append("    ");
            sb.append(v == null ? "0" : v.toString());
        }
        return sb.toString();
    }

    private boolean hasError(ValidationResult v, String accountId) {
        if (v == null || v.isPassed() || accountId == null) return false;
        for (String err : v.getErrors()) {
            if (err.contains(accountId)) return true;
        }
        return false;
    }

    private String nvl(Object v) {
        return v == null ? "" : v.toString();
    }
}
