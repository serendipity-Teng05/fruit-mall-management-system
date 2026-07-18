package com.example.fruitmallmanagementsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fruitmallmanagementsystem.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT COUNT(*) FROM sys_order WHERE status = #{status} AND archived = 0")
    long countByStatus(@Param("status") Integer status);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM sys_order WHERE status BETWEEN 1 AND 3 AND archived = 0")
    BigDecimal totalSales();

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM sys_order " +
            "WHERE status BETWEEN 1 AND 3 AND archived = 0 AND create_time >= #{start} AND create_time < #{end}")
    BigDecimal salesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}
