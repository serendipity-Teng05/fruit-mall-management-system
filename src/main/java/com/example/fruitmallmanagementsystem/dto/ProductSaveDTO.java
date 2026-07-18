package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductSaveDTO {
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String unit;
    private String image;
    private String description;
    private String origin;
    private Integer status;
    private Integer stockThreshold;
}
