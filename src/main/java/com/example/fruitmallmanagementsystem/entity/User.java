package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@TableName("sys_user")
public class User {
    // 对应 id (bigint)
    @TableId(type = IdType.AUTO)
    private Long id;

    // 对外展示的稳定人员编号，避免把数据库主键当作业务编号。
    private String userNo;

    // 对应 username (varchar)
    private String username;

    // 对应 password (varchar)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // 对应 real_name (varchar) -> 自动映射为 realName
    private String realName;

    // 对应 phone (varchar)
    private String phone;

    // 1 启用，0 停用；保留历史业务数据，不再物理删除有引用的人员。
    private Integer status;

    private Integer tokenVersion;

    // 对应 create_time (datetime) -> 自动映射为 createTime
    private LocalDateTime createTime;

    // 角色只从 sys_user_role -> sys_role 读取，不再在 sys_user 中重复保存。
    @TableField(exist = false)
    private List<Role> roles;

}

