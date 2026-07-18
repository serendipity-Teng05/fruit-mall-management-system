package com.example.fruitmallmanagementsystem.dto;

import com.example.fruitmallmanagementsystem.entity.Payment;
import lombok.Data;

@Data
public class PaymentCreateResult {
    private Payment payment;
    private String channelName;
    private boolean demoMode;
    private String instruction;
}
