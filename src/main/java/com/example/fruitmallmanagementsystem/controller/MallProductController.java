package com.example.fruitmallmanagementsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.entity.Product;
import com.example.fruitmallmanagementsystem.service.ProductService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mall/products")
public class MallProductController {
    private final ProductService productService;

    public MallProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<Page<Product>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "12") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String category) {
        int current = Math.max(1, pageNum == null ? 1 : pageNum);
        int size = Math.min(48, Math.max(1, pageSize == null ? 12 : pageSize));
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Product::getName, keyword == null ? null : keyword.trim())
                        .or()
                        .like(Product::getOrigin, keyword == null ? null : keyword.trim()))
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .orderByDesc(Product::getSalesCount)
                .orderByDesc(Product::getCreateTime)
                .orderByAsc(Product::getId);
        return Result.success(productService.page(new Page<>(current, size), query));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        Product product = productService.getOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getStatus, 1));
        if (product == null) {
            return Result.error("商品不存在或已下架");
        }
        return Result.success(product);
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        List<String> categories = productService.list(new LambdaQueryWrapper<Product>()
                        .select(Product::getCategory)
                        .eq(Product::getStatus, 1)
                        .isNotNull(Product::getCategory)
                        .groupBy(Product::getCategory)
                        .orderByAsc(Product::getCategory))
                .stream()
                .map(Product::getCategory)
                .filter(StringUtils::hasText)
                .toList();
        return Result.success(categories);
    }
}
