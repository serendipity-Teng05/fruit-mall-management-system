package com.example.fruitmallmanagementsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fruitmallmanagementsystem.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE sys_product SET stock = stock - #{count} " +
            "WHERE id = #{productId} AND status = 1 AND stock >= #{count}")
    int deductStock(@Param("productId") Long productId, @Param("count") Integer count);

    @Update("UPDATE sys_product SET stock = stock + #{count} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("count") Integer count);

    @Update("UPDATE sys_product SET stock = stock + #{change} " +
            "WHERE id = #{productId} AND stock + #{change} >= 0")
    int changeStock(@Param("productId") Long productId, @Param("change") Integer change);

    @Update("UPDATE sys_product SET sales_count = sales_count + #{count} WHERE id = #{productId}")
    int addSales(@Param("productId") Long productId, @Param("count") Integer count);
}
