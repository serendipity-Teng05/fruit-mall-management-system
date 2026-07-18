package com.example.fruitmallmanagementsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.dto.UserRoleView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
        SELECT DISTINCT p.permission_code
        FROM sys_user_role ur
        JOIN sys_role_permission rp ON ur.role_id = rp.role_id
        JOIN sys_permission p ON rp.permission_id = p.permission_id
        WHERE ur.user_id = #{userId}
    """)
    List<String> getPermissionCodesByUserId(Long userId);

    @Select("""
        SELECT DISTINCT r.role_code
        FROM sys_user_role ur
        JOIN sys_role r ON r.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
        """)
    List<String> getRoleCodesByUserId(Long userId);

    @Select("""
        <script>
        SELECT ur.user_id, r.role_id, r.role_code, r.role_name
        FROM sys_user_role ur
        JOIN sys_role r ON r.role_id = ur.role_id
        WHERE ur.user_id IN
        <foreach collection='userIds' item='userId' open='(' separator=',' close=')'>
            #{userId}
        </foreach>
        ORDER BY r.role_name, r.role_id
        </script>
        """)
    List<UserRoleView> selectRoleViewsByUserIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT next_value FROM sys_sequence WHERE sequence_key = 'USER_NO' FOR UPDATE")
    Long lockNextUserNumber();

    @Update("UPDATE sys_sequence SET next_value = next_value + 1, update_time = CURRENT_TIMESTAMP WHERE sequence_key = 'USER_NO'")
    int incrementUserNumber();
}
