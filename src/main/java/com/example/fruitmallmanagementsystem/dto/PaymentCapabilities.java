package com.example.fruitmallmanagementsystem.dto;

import java.util.List;

public record PaymentCapabilities(boolean demoMode, List<Channel> channels) {
    public record Channel(String value, String label, String description, boolean simulated) {}
}

