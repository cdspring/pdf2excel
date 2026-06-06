package com.pdf2excel.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BillData {
    private BillHeader header;
    private List<PowerDetail> powerDetails;
    private List<FeeDetail> feeDetails;
    private ValidationResult validation;
}
