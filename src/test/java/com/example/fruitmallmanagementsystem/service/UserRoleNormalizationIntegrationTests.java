package com.example.fruitmallmanagementsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fruitmallmanagementsystem.dto.UserSaveDTO;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class UserRoleNormalizationIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void schemaUsesBusinessUserNumberAndSingleRoleSource() {
        Integer legacyRoleColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'sys_user'
                  AND column_name = 'role'
                """, Integer.class);
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user", Integer.class);
        Integer distinctUserNumbers = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_no) FROM sys_user", Integer.class);
        Integer usersWithoutRoles = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user u
                WHERE NOT EXISTS (
                    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id
                )
                """, Integer.class);

        assertThat(legacyRoleColumns).isZero();
        assertThat(distinctUserNumbers).isEqualTo(userCount);
        assertThat(usersWithoutRoles).isZero();

        Page<User> page = userService.pageWithRoles(1, 100, null, true);
        User admin = page.getRecords().stream()
                .filter(user -> "admin".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(admin.getUserNo()).matches("EMP\\d{6}");
        assertThat(admin.getRoles()).extracting("roleCode").contains("ADMIN");
    }

    @Test
    void creatingPersonnelAllocatesNumberHashesPasswordAndAssignsRoleAtomically() {
        Long staffRoleId = jdbcTemplate.queryForObject(
                "SELECT role_id FROM sys_role WHERE role_code = 'STAFF'", Long.class);
        UserSaveDTO dto = new UserSaveDTO();
        dto.setUsername("integration_staff");
        dto.setPassword("test123456");
        dto.setRealName("集成测试员工");
        dto.setPhone("13900009999");
        dto.setRoleIds(List.of(staffRoleId));

        User operator = userService.getByUsername("admin");
        User saved = userService.saveManagedUser(dto, operator);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserNo()).matches("EMP\\d{6}");
        assertThat(saved.getPassword()).isNotEqualTo("test123456");
        assertThat(passwordEncoder.matches("test123456", saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).extracting("roleCode").containsExactly("STAFF");
        Integer relationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ? AND role_id = ?",
                Integer.class, saved.getId(), staffRoleId);
        assertThat(relationCount).isEqualTo(1);
    }

    @Test
    void legacyDeleteEndpointCannotRemoveTheLastRole() {
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        UserRole onlyRelation = userRoleService.getRolesByUserId(adminId).get(0);

        assertThatThrownBy(() -> userRoleService.deleteUserRole(onlyRelation.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("每名人员至少保留一个角色");
    }
}
