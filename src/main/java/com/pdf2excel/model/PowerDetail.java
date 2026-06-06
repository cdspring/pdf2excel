package com.pdf2excel.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PowerDetail {
    private int seq;
    private String accountId;
    private String accountName;
    private String meterId;
    private BigDecimal prevReading;
    private BigDecimal currReading;
    private int multiplier;
    private BigDecimal lineLoss;
    private BigDecimal transformerLoss;
    private BigDecimal refund;
    private BigDecimal power;
    private String remark;
}
