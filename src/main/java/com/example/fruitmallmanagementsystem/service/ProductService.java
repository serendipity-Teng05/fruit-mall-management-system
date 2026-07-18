package com.example.fruitmallmanagementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fruitmallmanagementsystem.entity.Product;
import com.example.fruitmallmanagementsystem.dto.ProductSaveDTO;

public interface ProductService extends IService<Product> {
    void changeStock(Long productId, Integer change);
    void changeStock(Long productId, Integer change, Long operatorId, String remark);
    Product saveProduct(ProductSaveDTO dto);
    void changeStatus(Long productId, Integer status);
}
