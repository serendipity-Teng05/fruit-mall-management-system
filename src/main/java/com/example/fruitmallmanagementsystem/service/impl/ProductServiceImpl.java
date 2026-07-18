package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.dto.ProductSaveDTO;
import com.example.fruitmallmanagementsystem.entity.Product;
import com.example.fruitmallmanagementsystem.mapper.ProductMapper;
import com.example.fruitmallmanagementsystem.mapper.InventoryMovementMapper;
import com.example.fruitmallmanagementsystem.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    private final InventoryMovementMapper inventoryMovementMapper;

    public ProductServiceImpl(InventoryMovementMapper inventoryMovementMapper) {
        this.inventoryMovementMapper = inventoryMovementMapper;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product saveProduct(ProductSaveDTO dto) {
        validate(dto);
        Product product = dto.getId() == null ? new Product() : getById(dto.getId());
        if (product == null) throw new IllegalArgumentException("商品不存在");
        boolean created = product.getId() == null;
        product.setName(dto.getName().trim());
        product.setCategory(trim(dto.getCategory()));
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setUnit(trim(dto.getUnit()));
        product.setImage(trim(dto.getImage()));
        product.setDescription(trim(dto.getDescription()));
        product.setOrigin(trim(dto.getOrigin()));
        product.setStockThreshold(dto.getStockThreshold() == null ? 10 : dto.getStockThreshold());
        product.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        product.setUpdateTime(LocalDateTime.now());
        if (created) {
            product.setSalesCount(0);
            product.setVersion(0);
            product.setCreateTime(LocalDateTime.now());
            save(product);
        } else if (!updateById(product)) {
            throw new IllegalStateException("商品已被其他操作修改，请刷新后重试");
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long productId, Integer status) {
        if (productId == null || (status == null || (status != 0 && status != 1))) {
            throw new IllegalArgumentException("商品状态参数不正确");
        }
        Product product = getById(productId);
        if (product == null) throw new IllegalArgumentException("商品不存在");
        product.setStatus(status);
        product.setUpdateTime(LocalDateTime.now());
        if (!updateById(product)) throw new IllegalStateException("商品已被其他操作修改，请刷新后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStock(Long productId, Integer change) {
        changeStock(productId, change, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStock(Long productId, Integer change, Long operatorId, String remark) {
        if (productId == null || change == null || change == 0 || Math.abs((long) change) > 1_000_000) {
            throw new IllegalArgumentException("商品ID和合理的非零库存变化量不能为空");
        }
        if (baseMapper.changeStock(productId, change) != 1) {
            Product product = getById(productId);
            if (product == null) throw new IllegalArgumentException("商品不存在");
            throw new IllegalStateException("库存不足，当前库存为：" + product.getStock());
        }
        inventoryMovementMapper.record(productId, change, "MANUAL_ADJUST", null, operatorId,
                StringUtils.hasText(remark) ? remark.trim() : "后台手工调整库存");
    }

    private void validate(ProductSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) throw new IllegalArgumentException("商品名称不能为空");
        if (dto.getName().trim().length() > 100) throw new IllegalArgumentException("商品名称不能超过100个字符");
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0
                || dto.getPrice().compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("商品价格范围不正确");
        }
        if (dto.getStock() == null || dto.getStock() < 0) throw new IllegalArgumentException("库存不能为负数");
        if (dto.getStockThreshold() != null && dto.getStockThreshold() < 0) {
            throw new IllegalArgumentException("库存预警值不能为负数");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new IllegalArgumentException("商品状态参数不正确");
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
