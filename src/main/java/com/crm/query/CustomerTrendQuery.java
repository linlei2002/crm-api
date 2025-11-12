package com.crm.query;

import lombok.Data;

import java.util.List;

@Data
public class CustomerTrendQuery {

    private List<String> timeRange;

    private String transactionType;

    private String timeFormat;
}
