package com.pdf2excel.service;

import com.pdf2excel.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfParseService {

    private static final Pattern HEADER_POWER_PATTERN = Pattern.compile("本期电量\\s+(\\d+)千瓦时");
    private static final Pattern HEADER_FEE_PATTERN = Pattern.compile("本期电费\\s+([\\d.]+)元");
    private static final Pattern HEADER_GROUP_ID_PATTERN = Pattern.compile("集团户编号\\s+(\\d+)");
    private static final Pattern HEADER_TOTAL_ACCOUNTS_PATTERN = Pattern.compile("总户数\\s+(\\d+)");
    private static final Pattern HEADER_AVG_PRICE_PATTERN = Pattern.compile("平均电价为([\\d.]+)元");
    private static final Pattern HEADER_METER_DATE_PATTERN = Pattern.compile("抄表日期\\s+(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern HEADER_PRINT_DATE_PATTERN = Pattern.compile("账单打印日期：(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern HEADER_PERIOD_START_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    public BillData parse(InputStream inputStream) throws IOException {
        PDDocument document = PDDocument.load(inputStream);
        try {
            BillData billData = new BillData();
            String firstPageText = extractPageText(document, 1);
            String[] firstPageLines = firstPageText.split("\n");

            billData.setHeader(parseHeader(firstPageLines, firstPageText));

            List<String> allLines = extractAllLines(document);
            int feeStartIdx = findFeeStartIndex(allLines);

            if (feeStartIdx > 0) {
                billData.setPowerDetails(parsePowerSection(allLines.subList(0, feeStartIdx)));
                billData.setFeeDetails(parseFeeSection(allLines.subList(feeStartIdx, allLines.size())));
            } else {
                billData.setPowerDetails(parsePowerSection(allLines));
                billData.setFeeDetails(new ArrayList<>());
            }

            return billData;
        } finally {
            document.close();
        }
    }

    private String extractPageText(PDDocument document, int page) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return stripper.getText(document);
    }

    private List<String> extractAllLines(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(document.getNumberOfPages());
        String text = stripper.getText(document);
        String[] arr = text.split("\n");
        List<String> lines = new ArrayList<>(arr.length);
        for (String line : arr) {
            lines.add(line.trim());
        }
        return lines;
    }

    private int findFeeStartIndex(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("电费明细列表") || lines.get(i).contains("电费明细")) {
                // 找到表头行（序号 户号 户名 供电单位...）
                for (int j = i; j < Math.min(i + 5, lines.size()); j++) {
                    if (lines.get(j).contains("序号") && lines.get(j).contains("户号")) {
                        return j + 1;
                    }
                }
                return i + 1;
            }
        }
        return -1;
    }

    private BillHeader parseHeader(String[] lines, String fullText) {
        BillHeader header = new BillHeader();

        Matcher m = HEADER_POWER_PATTERN.matcher(fullText);
        if (m.find()) header.setTotalPower(new BigDecimal(m.group(1)));

        m = HEADER_FEE_PATTERN.matcher(fullText);
        if (m.find()) header.setTotalFee(new BigDecimal(m.group(1)));

        m = HEADER_GROUP_ID_PATTERN.matcher(fullText);
        if (m.find()) header.setGroupId(m.group(1));

        m = HEADER_TOTAL_ACCOUNTS_PATTERN.matcher(fullText);
        if (m.find()) header.setTotalAccounts(Integer.parseInt(m.group(1)));

        m = HEADER_AVG_PRICE_PATTERN.matcher(fullText);
        if (m.find()) header.setAvgPrice(new BigDecimal(m.group(1)));

        m = HEADER_METER_DATE_PATTERN.matcher(fullText);
        if (m.find()) header.setMeterDate(m.group(1));

        m = HEADER_PRINT_DATE_PATTERN.matcher(fullText);
        if (m.find()) header.setPrintDate(m.group(1));

        List<String> dates = new ArrayList<>();
        m = HEADER_PERIOD_START_PATTERN.matcher(fullText);
        while (m.find()) dates.add(m.group(1));
        if (dates.size() >= 2) {
            header.setBillPeriodStart(dates.get(0));
            header.setBillPeriodEnd(dates.get(1));
        }

        for (String line : lines) {
            if (line.contains("集团户名称")) {
                String rest = line.substring(line.indexOf("集团户名称") + 5).trim();
                int end = rest.indexOf("销账");
                header.setGroupName(end > 0 ? rest.substring(0, end).trim() : rest);
                break;
            }
        }

        for (String line : lines) {
            if (line.contains("用电地址")) {
                String rest = line.substring(line.indexOf("用电地址") + 4).trim();
                int end = rest.indexOf("总户数");
                header.setAddress(end > 0 ? rest.substring(0, end).trim() : rest);
                break;
            }
        }

        return header;
    }

    // ========== 电量明细解析 ==========

    private List<PowerDetail> parsePowerSection(List<String> lines) {
        List<PowerDetail> details = new ArrayList<>();
        int i = 0;

        // 跳过表头区域
        while (i < lines.size()) {
            if (lines.get(i).contains("电量明细列表") || lines.get(i).contains("序号")) {
                i++;
                if (i < lines.size() && lines.get(i).contains("序号")) i++;
                break;
            }
            i++;
        }

        while (i < lines.size()) {
            Integer seq = extractSeqAt(lines, i);
            if (seq != null) {
                List<PowerDetail> segs = parsePowerEntry(lines, i);
                details.addAll(segs);
                i = findNextSeqIndex(lines, nextContentIndexAfterSeq(lines, i));
            } else {
                i++;
            }
        }

        // 重新编号（多计量段展开后序号连续）
        for (int j = 0; j < details.size(); j++) {
            details.get(j).setSeq(j + 1);
        }
        return details;
    }

    private void detectAndFixPowerSource(List<PowerDetail> details) {
        // 清理 remark 字段（已在解析时处理完毕）
        for (PowerDetail d : details) {
            d.setRemark(null);
        }
    }

    /**
     * 解析单条电量明细，返回一个或多个 PowerDetail（多计量段时每段独立一行）。
     * 调用方负责将结果全部加入列表。
     */
    private List<PowerDetail> parsePowerEntry(List<String> lines, int seqIdx) {
        List<PowerDetail> results = new ArrayList<>();

        Integer seqValue = extractSeqAt(lines, seqIdx);
        if (seqValue == null) return results;

        int seq = seqValue;
        int i = nextContentIndexAfterSeq(lines, seqIdx);
        if (i >= lines.size()) return results;

        // 户号：7位+6位数字
        String accountId = "";
        if (i < lines.size() && lines.get(i).matches("\\d{7}")) {
            accountId = lines.get(i++);
        }
        if (i < lines.size() && lines.get(i).matches("\\d{6}")) {
            accountId += lines.get(i++);
        }

        // 户名：连续中文行
        StringBuilder name = new StringBuilder();
        while (i < lines.size() && isChinese(lines.get(i))
                && !lines.get(i).startsWith("513") && !lines.get(i).startsWith("510")) {
            name.append(lines.get(i++));
        }
        String accountName = name.toString();

        // 解析每个计量段，每段生成独立行
        int segSeq = seq;
        while (i < lines.size()) {
            if (!lines.get(i).matches("51\\d{7}")) break;

            PowerDetail seg = new PowerDetail();
            seg.setSeq(segSeq++);
            seg.setAccountId(accountId);
            seg.setAccountName(accountName);

            String meterId = lines.get(i++);
            if (i < lines.size() && lines.get(i).matches("0{3,}\\d+")) {
                meterId += lines.get(i++);
            }
            String standalone = null;
            if (i < lines.size() && lines.get(i).matches("\\d+")) {
                standalone = lines.get(i++);
            }

            BigDecimal prevReading = null;
            BigDecimal currReading = null;
            if (i < lines.size() && lines.get(i).matches("[\\d.]+\\.")) {
                String prevStr = lines.get(i++);
                if (i < lines.size() && lines.get(i).matches("\\d+")) prevStr += lines.get(i++);
                prevReading = new BigDecimal(prevStr);

                if (i < lines.size() && lines.get(i).matches("[\\d.]+\\.")) {
                    String currStr = lines.get(i++);
                    if (i < lines.size() && lines.get(i).matches("\\d+")) currStr += lines.get(i++);
                    currReading = new BigDecimal(currStr);
                } else if (i < lines.size()) {
                    String[] tokens = lines.get(i).split("\\s+");
                    if (tokens.length >= 6 && tokens[0].matches("[\\d.]+") && tokens[0].contains(".")) {
                        currReading = new BigDecimal(tokens[0]);
                    }
                }
            }

            if (i < lines.size()) {
                String[] parts = lines.get(i).split("\\s+");
                List<BigDecimal> nums = new ArrayList<>();
                for (String p : parts) {
                    if (p.matches("[\\d.]+")) nums.add(new BigDecimal(p));
                    else break;
                }

                boolean parsed = false;
                if (prevReading != null && nums.size() >= 5) {
                    int offset = (currReading != null && nums.size() >= 6
                            && nums.get(0).compareTo(currReading) == 0) ? 1 : 0;
                    if (nums.size() >= offset + 5) {
                        seg.setMeterId(meterId + (standalone != null ? padLeft(standalone, 4) : ""));
                        seg.setPrevReading(prevReading);
                        seg.setCurrReading(currReading);
                        seg.setMultiplier(nums.get(offset).intValue());
                        seg.setLineLoss(nums.get(offset + 1));
                        seg.setTransformerLoss(nums.get(offset + 2));
                        seg.setRefund(nums.get(offset + 3));
                        seg.setPower(nums.get(offset + 4));
                        i++; parsed = true;
                    }
                } else if (nums.size() >= 7) {
                    seg.setMeterId(meterId + (standalone != null ? padLeft(standalone, 4) : ""));
                    seg.setPrevReading(nums.get(0));
                    seg.setCurrReading(nums.get(1));
                    seg.setMultiplier(nums.get(2).intValue());
                    seg.setLineLoss(nums.get(3));
                    seg.setTransformerLoss(nums.get(4));
                    seg.setRefund(nums.get(5));
                    seg.setPower(nums.get(6));
                    i++; parsed = true;
                } else if (nums.size() >= 6) {
                    seg.setMeterId(meterId + (standalone != null ? padLeft(standalone, 4) : ""));
                    seg.setPrevReading(nums.get(0));
                    seg.setCurrReading(nums.get(1));
                    seg.setMultiplier(nums.get(2).intValue());
                    seg.setLineLoss(nums.get(3));
                    seg.setTransformerLoss(nums.get(4));
                    seg.setRefund(nums.get(5));
                    seg.setPower(BigDecimal.ZERO);
                    i++; parsed = true;
                } else if (nums.size() == 5 && standalone != null) {
                    seg.setMeterId(meterId + padLeft(standalone, 4));
                    seg.setMultiplier(nums.get(0).intValue());
                    seg.setLineLoss(nums.get(1));
                    seg.setTransformerLoss(nums.get(2));
                    seg.setRefund(nums.get(3));
                    seg.setPower(nums.get(4));
                    i++; parsed = true;
                }
                if (!parsed) {
                    seg.setMeterId(meterId + (standalone != null ? padLeft(standalone, 4) : ""));
                    seg.setPower(standalone != null ? new BigDecimal(standalone) : BigDecimal.ZERO);
                    break;
                }
            }

            if (seg.getPower() == null) seg.setPower(BigDecimal.ZERO);
            results.add(seg);
        }

        // 无计量段时返回空壳行（保持序号连续）
        if (results.isEmpty()) {
            PowerDetail d = new PowerDetail();
            d.setSeq(seq);
            d.setAccountId(accountId);
            d.setAccountName(accountName);
            d.setPower(BigDecimal.ZERO);
            results.add(d);
        }

        return results;
    }

    /** 保留旧签名供内部兼容，实际委托给 parsePowerEntry */
    private PowerDetail parseSinglePowerEntry(List<String> lines, int seqIdx) {
        List<PowerDetail> list = parsePowerEntry(lines, seqIdx);
        return list.isEmpty() ? null : list.get(0);
    }

    private String padLeft(String s, int len) {
        while (s.length() < len) s = "0" + s;
        return s;
    }

    // ========== 电费明细解析 ==========

    private List<FeeDetail> parseFeeSection(List<String> lines) {
        List<FeeDetail> details = new ArrayList<>();
        int i = 0;

        while (i < lines.size()) {
            Integer seq = extractSeqAt(lines, i);
            if (seq != null) {
                FeeDetail detail = parseSingleFeeEntry(lines, i);
                if (detail != null) {
                    details.add(detail);
                }
                i = findNextSeqIndex(lines, nextContentIndexAfterSeq(lines, i));
            } else {
                i++;
            }
        }
        return details;
    }

    private FeeDetail parseSingleFeeEntry(List<String> lines, int seqIdx) {
        FeeDetail detail = new FeeDetail();
        Integer seqValue = extractSeqAt(lines, seqIdx);
        if (seqValue == null) return null;
        detail.setSeq(seqValue);

        int i = nextContentIndexAfterSeq(lines, seqIdx);
        if (i >= lines.size()) return null;

        // 户号：7位+6位数字
        String accountId = "";
        if (i < lines.size() && lines.get(i).matches("\\d{7}")) {
            accountId = lines.get(i);
            i++;
        }
        if (i < lines.size() && lines.get(i).matches("\\d{6}")) {
            accountId += lines.get(i);
            i++;
        }
        detail.setAccountId(accountId);

        // 找到本条目的结束位置（下一个序号行）
        int nextSeq = findNextSeqIndex(lines, i);

        // 从条目末尾向前搜索金额行
        // 金额格式：3个数字（电费 容量费 政府性基金），可能跨行
        List<BigDecimal> amounts = extractFeeAmountsReverse(lines, i, nextSeq);
        if (amounts.size() >= 1) detail.setFee(amounts.get(0));
        if (amounts.size() >= 2) detail.setCapacityFee(amounts.get(1));
        if (amounts.size() >= 3) detail.setGovernmentFund(amounts.get(2));

        return detail;
    }

    private static final Pattern FEE_AMOUNT_PATTERN = Pattern.compile(
            "(-?[\\d]+\\.?\\d*)\\s+(-?[\\d]+\\.?\\d*)\\s+(-?[\\d]+\\.?\\d*)\\s*$");
    private static final Pattern FEE_TRAILING_PATTERN = Pattern.compile(
            "(-?[\\d]+\\.?\\d*)\\s+(-?[\\d]+\\.?\\d*)\\s*$");

    private List<BigDecimal> extractFeeAmountsReverse(List<String> lines, int startIdx, int endIdx) {
        List<BigDecimal> amounts = new ArrayList<>();

        // 从startIdx向前搜索：跳过中文行，找到第一个含数字的位置
        // 策略：从条目区域内找第一个金额行（从前向后）
        StringBuilder amountText = new StringBuilder();
        for (int i = startIdx; i < endIdx; i++) {
            String line = lines.get(i);
            // 纯数字/小数行
            if (line.matches("[-\\d.\\s]+")) {
                amountText.append(" ").append(line);
                // 继续收集紧跟的纯数字行（处理跨行小数）
                for (int j = i + 1; j < endIdx; j++) {
                    String next = lines.get(j);
                    if (next.matches("-?[\\d.]+")) {
                        amountText.append(" ").append(next);
                    } else {
                        break;
                    }
                }
                break;
            }
            // 混合行：末尾有数字（如 "黄金小区FTTB-1 559.42 0"）
            Matcher m = Pattern.compile(".*[^\\d.\\s]\\s+([-\\d.]+(?:\\s+[-\\d.]+)*)\\s*$").matcher(line);
            if (m.matches()) {
                amountText.append(" ").append(m.group(1));
                // 继续收集紧跟的纯数字行
                for (int j = i + 1; j < endIdx; j++) {
                    String next = lines.get(j);
                    if (next.matches("-?[\\d.]+")) {
                        amountText.append(" ").append(next);
                    } else {
                        break;
                    }
                }
                break;
            }
        }

        String text = amountText.toString().trim();
        if (text.isEmpty()) return amounts;

        // 处理跨行小数：如 "15. 94" → "15.94"
        text = text.replaceAll("\\.(\\s+)(\\d)", ".$2");

        String[] parts = text.split("\\s+");
        // 只取前3个有效数字作为 电费、容量费、政府性基金
        int count = 0;
        for (String p : parts) {
            if (count >= 3) break;
            if (p.matches("-?[\\d.]+") && !p.isEmpty()) {
                try {
                    amounts.add(new BigDecimal(p));
                    count++;
                } catch (NumberFormatException e) {
                    // skip
                }
            }
        }

        return amounts;
    }

    // ========== 工具方法 ==========

    private boolean isSeqLine(String line) {
        if (line == null || line.isEmpty()) return false;
        // 纯数字，1-4位，值在1-9999之间
        if (!line.matches("\\d{1,4}")) return false;
        int val = Integer.parseInt(line);
        return val >= 1 && val <= 9999;
    }

    /**
     * 处理被 PDFBox 拆开的四位序号，例如：
     *   100
     *   1
     *   5100002
     *   285056
     * 应识别为业务序号 1001。
     */
    private Integer extractSeqAt(List<String> lines, int idx) {
        if (idx >= lines.size()) return null;
        String line = lines.get(idx);
        if (!isSeqLine(line)) return null;

        int val = Integer.parseInt(line);

        // 普通序号后面直接跟 7 位户号
        if (idx + 1 < lines.size() && lines.get(idx + 1).matches("\\d{7}")) {
            return val;
        }

        // 被拆开的四位序号：3位 + 1位，后面再跟 7 位户号 + 6 位户号
        if (line.matches("\\d{3}")
                && idx + 3 < lines.size()
                && lines.get(idx + 1).matches("\\d")
                && lines.get(idx + 2).matches("\\d{7}")
                && lines.get(idx + 3).matches("\\d{6}")) {
            return Integer.parseInt(line + lines.get(idx + 1));
        }

        return null;
    }

    private int nextContentIndexAfterSeq(List<String> lines, int seqIdx) {
        if (seqIdx + 3 < lines.size()
                && lines.get(seqIdx).matches("\\d{3}")
                && lines.get(seqIdx + 1).matches("\\d")
                && lines.get(seqIdx + 2).matches("\\d{7}")
                && lines.get(seqIdx + 3).matches("\\d{6}")) {
            return seqIdx + 2;
        }
        return seqIdx + 1;
    }

    private int findNextSeqIndex(List<String> lines, int fromIdx) {
        for (int i = fromIdx; i < lines.size(); i++) {
            Integer seq = extractSeqAt(lines, i);
            if (seq != null) {
                return i;
            }
        }
        return lines.size();
    }

    private boolean isChinese(String line) {
        if (line == null || line.isEmpty()) return false;
        for (char c : line.toCharArray()) {
            if (c >= '一' && c <= '龥') return true;
        }
        return false;
    }

    private boolean containsDigit(String line) {
        for (char c : line.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private boolean isAddressLine(String line) {
        if (line == null || line.isEmpty()) return false;
        if (line.matches("[\\d.\\s]+")) return false;
        if (isSeqLine(line)) return false;
        return line.contains("省") || line.contains("市") || line.contains("区")
                || line.contains("路") || line.contains("街") || line.contains("镇")
                || line.contains("村") || line.contains("号") || line.contains("栋")
                || line.contains("单元") || line.contains("楼") || line.contains("委会")
                || line.contains("社区") || line.contains("小区");
    }
}
