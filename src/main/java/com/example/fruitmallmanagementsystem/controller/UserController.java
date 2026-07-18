package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.dto.PasswordDto;
import com.example.fruitmallmanagementsystem.dto.UserSaveDTO;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys_user")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/list")
    @RequirePermission("USER_MANAGE")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        return Result.success(userService.pageWithRoles(pageNum, pageSize, username, includeDisabled));
    }

    @PostMapping("/save")
    @RequirePermission("USER_MANAGE")
    public Result save(@RequestBody UserSaveDTO user,
                       @RequestAttribute("currentUser") User currentUser) {
        return Result.success(userService.saveManagedUser(user, currentUser));
    }

    /**
     * 停用人员而不是物理删除，保留订单、支付和操作日志的历史关联。
     */
    @PostMapping("/delete")
    @RequirePermission("USER_MANAGE")
    public Result delete(@RequestParam Long id,
                         @RequestAttribute("currentUser") User currentUser) {
        return changeStatus(id, 0, currentUser);
    }

    @PostMapping("/status")
    @RequirePermission("USER_MANAGE")
    public Result changeStatus(@RequestParam Long id,
                               @RequestParam Integer status,
                               @RequestAttribute("currentUser") User currentUser) {
        userService.changeStatus(id, status, currentUser.getId());
        return Result.success(status == 1 ? "人员已启用" : "人员已停用");
    }

    @PutMapping("/password")
    public Result updatePassword(@RequestBody PasswordDto passwordDto,
                                 @RequestAttribute("currentUser") User currentUser) {
        if (!StringUtils.hasText(passwordDto.getOldPassword())
                || !StringUtils.hasText(passwordDto.getNewPassword())) {
            return Result.error("密码不能为空");
        }
        if (passwordDto.getNewPassword().length() < 8 || passwordDto.getNewPassword().length() > 72) {
            return Result.error("新密码长度应为 8-72 位");
        }

        User dbUser = userService.getById(currentUser.getId());
        if (dbUser == null) {
            return Result.error("用户不存在");
        }
        if (!passwordMatches(passwordDto.getOldPassword(), dbUser.getPassword())) {
            return Result.error("原密码错误，请检查");
        }

        dbUser.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
        dbUser.setTokenVersion((dbUser.getTokenVersion() == null ? 0 : dbUser.getTokenVersion()) + 1);
        userService.updateById(dbUser);
        return Result.success("密码修改成功，请重新登录");
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        return isBcrypt(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : storedPassword != null && storedPassword.equals(rawPassword);
    }

    private boolean isBcrypt(String password) {
        return password != null && (password.startsWith("$2a$")
                || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }
}
