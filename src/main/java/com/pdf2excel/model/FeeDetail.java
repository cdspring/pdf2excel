package com.pdf2excel.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FeeDetail {
    private int seq;
    private String accountId;
    private String accountName;
    private String powerSupplyUnit;
    private String address;
    private BigDecimal fee;
    private BigDecimal capacityFee;
    private BigDecimal powerFactorFee;
    private BigDecimal ruralMaintenanceFee;
    private BigDecimal governmentFund;
    private String remark;
}
