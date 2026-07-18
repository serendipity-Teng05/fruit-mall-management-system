package com.example.fruitmallmanagementsystem.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryMovementMapper {
    @Insert("INSERT INTO sys_inventory_movement(product_id, change_quantity, movement_type, business_no, operator_id, remark) " +
            "VALUES(#{productId}, #{changeQuantity}, #{movementType}, #{businessNo}, #{operatorId}, #{remark})")
    int record(@Param("productId") Long productId,
               @Param("changeQuantity") Integer changeQuantity,
               @Param("movementType") String movementType,
               @Param("businessNo") String businessNo,
               @Param("operatorId") Long operatorId,
               @Param("remark") String remark);
}
