package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.entity.FollowUp;
import com.crm.entity.Lead;
import com.crm.enums.BusinessType;
import com.crm.query.IdQuery;
import com.crm.query.LeadQuery;
import com.crm.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@RestController
@RequestMapping("/lead")
@Tag(name = "线索管理")
@AllArgsConstructor
public class LeadController {
    private final LeadService leadService;

    @PostMapping("Page")
    @Operation(summary = "线索分页查询")
    @Log(title = "线索分页查询", businessType = BusinessType.SELECT)
    public Result<PageResult<Lead>> getPage(@RequestBody LeadQuery query) {
        return Result.ok(leadService.getPage(query));
    }

    @PostMapping("saveOrEdit")
    @Operation(summary = "保存或修改")
    @Log(title = "保存或修改", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result saveOrEdit(@RequestBody @Valid Lead lead) {
        leadService.saveOrEdit(lead);
        return Result.ok();
    }

    @PostMapping("followLead")
    @Operation(summary = "跟进线索")
    @Log(title = "跟进线索", businessType = BusinessType.OTHER)
    public Result followLead(@RequestBody @Valid FollowUp followUp) {
        leadService.followLead(followUp);
        return Result.ok();
    }

    @PostMapping("convertToCustomer")
    @Operation(summary = "线索转客户")
    @Log(title = "线索转客户", businessType = BusinessType.OTHER)
    public Result convertToCustomer(@RequestBody @Valid IdQuery idQuery) {
        leadService.convertToCustomer(idQuery);
        return Result.ok();
    }

}
