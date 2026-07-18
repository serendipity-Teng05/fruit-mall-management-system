package com.example.fruitmallmanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateResult {
    private String orderNo;
    private BigDecimal totalAmount;
    private LocalDateTime expireTime;
}
