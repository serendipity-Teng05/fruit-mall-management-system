package com.example.fruitmallmanagementsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.dto.ProductSaveDTO;
import com.example.fruitmallmanagementsystem.dto.ProductStatusDTO;
import com.example.fruitmallmanagementsystem.dto.StockChangeDTO;
import com.example.fruitmallmanagementsystem.entity.Product;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.ProductService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sys_product")
@RequirePermission("PRODUCT_MANAGE")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/list")
    public Result<Page<Product>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String sortField,
                                      @RequestParam(required = false) String sortOrder) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        query.like(StringUtils.hasText(name), Product::getName, name == null ? null : name.trim())
                .eq(status != null, Product::getStatus, status)
                .eq(StringUtils.hasText(category), Product::getCategory, category);
        boolean asc = "ascending".equals(sortOrder) || "asc".equalsIgnoreCase(sortOrder);
        if ("price".equals(sortField)) query.orderBy(true, asc, Product::getPrice);
        else if ("stock".equals(sortField)) query.orderBy(true, asc, Product::getStock);
        else if ("createTime".equals(sortField)) query.orderBy(true, asc, Product::getCreateTime);
        else if ("id".equals(sortField)) query.orderBy(true, asc, Product::getId);
        else query.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);
        int current = Math.max(1, pageNum == null ? 1 : pageNum);
        int size = Math.min(100, Math.max(1, pageSize == null ? 10 : pageSize));
        return Result.success(productService.page(new Page<>(current, size), query));
    }

    @PostMapping("/save")
    public Result<Product> save(@RequestBody ProductSaveDTO product) {
        return Result.success(productService.saveProduct(product));
    }

    @PutMapping("/status")
    public Result<String> status(@RequestBody ProductStatusDTO dto) {
        productService.changeStatus(dto.getId(), dto.getStatus());
        return Result.success(dto.getStatus() == 1 ? "商品已上架" : "商品已下架");
    }

    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        productService.changeStatus(id, 0);
        return Result.success("商品已下架，历史订单仍保留");
    }

    @PutMapping("/stock")
    public Result<String> updateStock(@RequestBody StockChangeDTO dto,
                                      @RequestAttribute("currentUser") User currentUser) {
        productService.changeStock(dto.getId(), dto.getQuantityChange(), currentUser.getId(), dto.getRemark());
        return Result.success("库存更新成功");
    }
}
