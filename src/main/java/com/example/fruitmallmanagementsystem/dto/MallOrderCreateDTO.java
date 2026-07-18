package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class MallOrderCreateDTO {
    private String requestId;
    private Long addressId;
    private List<Item> items;

    @Data
    public static class Item {
        private Long productId;
        private Integer count;
    }
}
