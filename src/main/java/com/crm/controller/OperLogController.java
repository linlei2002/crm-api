package com.crm.controller;

import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.entity.OperLog;
import com.crm.query.OperLogQuery;
import com.crm.service.OperLogService;
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
 * 操作日志记录 前端控制器
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@RestController
@Tag(name = "操作日志管理")
@RequestMapping("/operLog")
@AllArgsConstructor
public class OperLogController {
    private final OperLogService operLogService;
    @PostMapping("page")
    @Operation(summary = "操作日志分页查询")
    public Result<PageResult<OperLog>> getPage(@RequestBody OperLogQuery  query){
        return Result.ok(operLogService.getPage(query));
    }


}
