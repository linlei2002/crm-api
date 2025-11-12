package com.crm.query;

import com.crm.common.model.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LeadQuery extends Query {
    @Schema(description = "线索名称")
    private String name;

    @Schema(description = "跟进状态")
    private String followStatus;

    @Schema(description = "线索状态")
    private String status;
}
