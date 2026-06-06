package com.pdf2excel.validation;

import com.pdf2excel.model.BillData;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ValidationContext {

    private final BillData billData;
    private final String fileName;
    private final Map<String, Object> attributes = new HashMap<>();

    public ValidationContext(BillData billData, String fileName) {
        this.billData = billData;
        this.fileName = fileName;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}
