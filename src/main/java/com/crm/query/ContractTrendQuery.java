package com.crm.query;

import lombok.Data;

import java.util.List;

@Data
public class ContractTrendQuery {
    private List<String> timeRange;
    private String transactionType;
}
