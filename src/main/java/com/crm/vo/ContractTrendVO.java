package com.crm.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractTrendVO {
    private String contractName;

    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
}
