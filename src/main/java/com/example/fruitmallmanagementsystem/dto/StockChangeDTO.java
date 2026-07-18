package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

@Data
public class StockChangeDTO {
    private Long id;
    private Integer quantityChange;
    private String remark;
}
