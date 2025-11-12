package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.entity.Product;
import com.crm.enums.BusinessType;
import com.crm.query.ProductQuery;
import com.crm.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Tag(name = "商品管理", description = "商品相关接口")
@RestController
@RequestMapping("/product")
@AllArgsConstructor
public class ProductController {
    private final ProductService productService;
    @Operation(summary = "分页查询商品列表", description = "根据条件分页查询商品信息")
    @PostMapping("/page")
    @Log(title = "分页查询商品列表", businessType = BusinessType.SELECT)
    public Result<PageResult<Product>> getPage(@RequestBody @Valid ProductQuery query) {
        return Result.ok(productService.getPage( query));
    }

    @PostMapping("saveOrEdit")
    @Operation(summary = "修改或保存")
    @Log(title = "修改或保存", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result saveOrEdit(@RequestBody @Valid Product product) {
        productService.saveOrEdit(product);
        return Result.ok();
    }

    @PostMapping("changeStatus")
    @Operation(summary = "修改商品状态")
    @Log(title = "修改商品状态", businessType = BusinessType.INSERT_OR_UPDATE)
    public Result changeStatus(@RequestBody @Valid Product product) {
        productService.changeStatus(product);
        return Result.ok();
    }

}
