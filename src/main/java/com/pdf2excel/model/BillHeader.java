package com.pdf2excel.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BillHeader {
    private String groupId;
    private String groupName;
    private String billPeriodStart;
    private String billPeriodEnd;
    private String address;
    private int totalAccounts;
    private BigDecimal totalPower;
    private BigDecimal totalFee;
    private String meterDate;
    private String printDate;
    private BigDecimal avgPrice;
}
