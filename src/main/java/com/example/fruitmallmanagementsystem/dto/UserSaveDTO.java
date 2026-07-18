package com.example.fruitmallmanagementsystem.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class UserSaveDTO {
    private Long id;
    private String username;
    private String password;

    @JsonAlias("name")
    private String realName;

    private String phone;
    private List<Long> roleIds;
}
