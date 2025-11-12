package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.enums.BusinessType;
import com.crm.query.ContractQuery;
import com.crm.service.ContractService;
import com.crm.vo.ContractVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Tag(name = "合同管理")
@RestController
@RequestMapping("/contract")
@AllArgsConstructor
public class ContractController {
    private final ContractService contractService;

    @PostMapping("page")
    @Operation(summary = "合同分页查询")
    @Log(title = "合同分页查询", businessType = BusinessType.SELECT)
    public Result<PageResult<ContractVO>> getPage(@RequestBody @Valid ContractQuery contractQuery) {
        return Result.ok(contractService.getPage(contractQuery));
    }

    @PostMapping("saveOrUpdate")
    @Operation(summary = "保存或更新合同")
    @Log(title = "保存或更新合同", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result saveOrUpdate(@RequestBody @Valid ContractVO contractVO) {
        contractService.saveOrUpdate(contractVO);
        return Result.ok();
    }
}
