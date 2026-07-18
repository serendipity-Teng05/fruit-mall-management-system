package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fruitmallmanagementsystem.dto.UserRoleView;
import com.example.fruitmallmanagementsystem.dto.UserSaveDTO;
import com.example.fruitmallmanagementsystem.dto.CustomerRegisterDTO;
import com.example.fruitmallmanagementsystem.entity.Role;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.mapper.UserMapper;
import com.example.fruitmallmanagementsystem.service.UserService;
import com.example.fruitmallmanagementsystem.service.UserRoleService;
import com.example.fruitmallmanagementsystem.service.RoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final RoleService roleService;

    public UserServiceImpl(PasswordEncoder passwordEncoder,
                           UserRoleService userRoleService,
                           RoleService roleService) {
        this.passwordEncoder = passwordEncoder;
        this.userRoleService = userRoleService;
        this.roleService = roleService;
    }

    // =========================
    // 修复编译报错：实现接口方法
    @Override
    public User getByUsername(String username) {
        // 调用 MyBatis-Plus 提供的 lambda 查询方式
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        return baseMapper.getPermissionCodesByUserId(userId);
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return baseMapper.getRoleCodesByUserId(userId);
    }

    @Override
    public Page<User> pageWithRoles(Integer pageNum, Integer pageSize, String keyword, Boolean includeDisabled) {
        int safePageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int safePageSize = Math.min(100, Math.max(1, pageSize == null ? 10 : pageSize));

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (!Boolean.TRUE.equals(includeDisabled)) {
            wrapper.eq(User::getStatus, 1);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(query -> query.like(User::getUsername, value)
                    .or().like(User::getRealName, value)
                    .or().like(User::getUserNo, value));
        }
        wrapper.orderByAsc(User::getUserNo).orderByAsc(User::getId);

        Page<User> page = page(new Page<>(safePageNum, safePageSize), wrapper);
        attachRoles(page.getRecords());
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User saveManagedUser(UserSaveDTO dto, User operator) {
        validateUser(dto);
        List<Long> roleIds = normalizeRoleIds(dto.getRoleIds());

        User sameName = getByUsername(dto.getUsername().trim());
        if (sameName != null && !Objects.equals(sameName.getId(), dto.getId())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (StringUtils.hasText(dto.getPhone())) {
            User samePhone = lambdaQuery().eq(User::getPhone, dto.getPhone().trim()).one();
            if (samePhone != null && !Objects.equals(samePhone.getId(), dto.getId())) {
                throw new IllegalArgumentException("手机号已被其他人员使用");
            }
        }

        User user;
        if (dto.getId() == null) {
            user = new User();
            user.setUserNo(nextUserNo("EMP"));
            user.setUsername(dto.getUsername().trim());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setRealName(trimToNull(dto.getRealName()));
            user.setPhone(trimToNull(dto.getPhone()));
            user.setStatus(1);
            user.setTokenVersion(0);
            user.setCreateTime(LocalDateTime.now());
            save(user);
        } else {
            user = getById(dto.getId());
            if (user == null) {
                throw new IllegalArgumentException("人员不存在");
            }
            user.setRealName(trimToNull(dto.getRealName()));
            user.setPhone(trimToNull(dto.getPhone()));
            if (StringUtils.hasText(dto.getPassword())) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
                user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            }
            updateById(user);
        }

        userRoleService.assignRolesByOperator(user.getId(), roleIds, operator);
        attachRoles(List.of(user));
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User registerCustomer(CustomerRegisterDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername())
                || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getRealName())
                || !StringUtils.hasText(dto.getPhone())) {
            throw new IllegalArgumentException("用户名、密码、姓名和手机号不能为空");
        }
        String username = dto.getUsername().trim();
        String phone = dto.getPhone().trim();
        if (!username.matches("[A-Za-z0-9_@.\\-]{3,50}")) {
            throw new IllegalArgumentException("用户名格式不正确");
        }
        if (dto.getPassword().length() < 8 || dto.getPassword().length() > 72) {
            throw new IllegalArgumentException("密码长度应为 8-72 位");
        }
        if (!phone.matches("1[3-9]\\d{9}")) {
            throw new IllegalArgumentException("请输入正确的手机号");
        }
        if (getByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (lambdaQuery().eq(User::getPhone, phone).one() != null) {
            throw new IllegalArgumentException("手机号已注册");
        }
        if (lambdaQuery().and(wrapper -> wrapper
                .eq(User::getUsername, phone)
                .or()
                .eq(User::getPhone, username)).count() > 0) {
            throw new IllegalArgumentException("用户名或手机号与已有登录账号冲突");
        }

        Role customerRole = roleService.getAllRoles().stream()
                .filter(role -> "CUSTOMER".equals(role.getRoleCode()))
                .findFirst()
                .orElse(null);
        if (customerRole == null) {
            throw new IllegalStateException("CUSTOMER 角色未初始化");
        }

        User user = new User();
        user.setUserNo(nextUserNo("CUS"));
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName().trim());
        user.setPhone(phone);
        user.setStatus(1);
        user.setTokenVersion(0);
        user.setCreateTime(LocalDateTime.now());
        save(user);
        userRoleService.assignRoles(user.getId(), List.of(customerRole.getRoleId()));
        attachRoles(List.of(user));
        return user;
    }

    @Override
    public void changeStatus(Long userId, Integer status, Long operatorId) {
        if (userId == null || (status == null || (status != 0 && status != 1))) {
            throw new IllegalArgumentException("人员状态参数不正确");
        }
        if (Objects.equals(userId, operatorId) && status == 0) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        User user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("人员不存在");
        }
        if ("admin".equalsIgnoreCase(user.getUsername()) && status == 0) {
            throw new IllegalArgumentException("系统管理员账号不能停用");
        }
        user.setStatus(status);
        if (status == 0) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        }
        updateById(user);
    }

    @Override
    public void attachRoles(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(User::getId).filter(Objects::nonNull).toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        users.forEach(user -> user.setRoles(new ArrayList<>()));
        for (UserRoleView view : baseMapper.selectRoleViewsByUserIds(userIds)) {
            User user = userMap.get(view.getUserId());
            if (user == null) continue;
            Role role = new Role();
            role.setRoleId(view.getRoleId());
            role.setRoleCode(view.getRoleCode());
            role.setRoleName(view.getRoleName());
            user.getRoles().add(role);
        }
    }

    private void validateUser(UserSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!dto.getUsername().trim().matches("[A-Za-z0-9_@.\\-]{3,50}")) {
            throw new IllegalArgumentException("用户名格式不正确");
        }
        if (!StringUtils.hasText(dto.getRealName())) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (dto.getId() == null && !StringUtils.hasText(dto.getPassword())) {
            throw new IllegalArgumentException("创建人员时必须设置初始密码");
        }
        if (StringUtils.hasText(dto.getPhone()) && dto.getPhone().trim().length() > 20) {
            throw new IllegalArgumentException("手机号长度不能超过20位");
        }
        if (dto.getId() == null && StringUtils.hasText(dto.getPassword())
                && (dto.getPassword().length() < 8 || dto.getPassword().length() > 72)) {
            throw new IllegalArgumentException("初始密码长度应为 8-72 位");
        }
        if (dto.getId() != null && StringUtils.hasText(dto.getPassword())
                && (dto.getPassword().length() < 8 || dto.getPassword().length() > 72)) {
            throw new IllegalArgumentException("重置密码长度应为 8-72 位");
        }
    }

    private List<Long> normalizeRoleIds(Collection<Long> roleIds) {
        if (roleIds == null) {
            throw new IllegalArgumentException("请至少选择一个角色");
        }
        List<Long> normalized = new ArrayList<>(new LinkedHashSet<>(roleIds.stream()
                .filter(Objects::nonNull)
                .toList()));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个角色");
        }
        return normalized;
    }

    private String nextUserNo(String prefix) {
        Long number = baseMapper.lockNextUserNumber();
        if (number == null || baseMapper.incrementUserNumber() != 1) {
            throw new IllegalStateException("人员编号序列未初始化，请执行数据库升级脚本");
        }
        return prefix + String.format("%06d", number);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
