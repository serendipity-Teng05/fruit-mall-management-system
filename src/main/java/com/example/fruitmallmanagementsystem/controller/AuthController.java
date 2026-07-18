package com.example.fruitmallmanagementsystem.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.ClientIpResolver;
import com.example.fruitmallmanagementsystem.dto.CustomerRegisterDTO;
import com.example.fruitmallmanagementsystem.dto.LoginDto;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.LoginAttemptService;
import com.example.fruitmallmanagementsystem.service.RefreshTokenService;
import com.example.fruitmallmanagementsystem.service.UserService;
import com.example.fruitmallmanagementsystem.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String CAPTCHA_CODE = "CAPTCHA_CODE";
    private static final String CAPTCHA_CREATED_AT = "CAPTCHA_CREATED_AT";
    private static final String CAPTCHA_FAILURES = "CAPTCHA_FAILURES";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final ClientIpResolver clientIpResolver;
    private final RefreshTokenService refreshTokenService;
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;
    private final long refreshDays;

    public AuthController(UserService userService, JwtUtils jwtUtils, PasswordEncoder passwordEncoder,
                          LoginAttemptService loginAttemptService, ClientIpResolver clientIpResolver,
                          RefreshTokenService refreshTokenService,
                          @Value("${app.jwt.refresh-cookie-name:fruit_mall_refresh}") String refreshCookieName,
                          @Value("${app.jwt.refresh-cookie-secure:false}") boolean refreshCookieSecure,
                          @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.clientIpResolver = clientIpResolver;
        this.refreshTokenService = refreshTokenService;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshDays = Math.max(1, Math.min(30, refreshDays));
    }

    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response, HttpSession session) throws IOException {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(160, 48, 4, 16);
        session.setAttribute(CAPTCHA_CODE, captcha.getCode());
        session.setAttribute(CAPTCHA_CREATED_AT, Instant.now());
        session.setAttribute(CAPTCHA_FAILURES, 0);
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        captcha.write(response.getOutputStream());
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDto loginDto, HttpSession session,
                                              HttpServletRequest request, HttpServletResponse response) {
        if (loginDto == null || !StringUtils.hasText(loginDto.getUsername())
                || !StringUtils.hasText(loginDto.getPassword())) {
            return Result.error(400, "账号和密码不能为空");
        }
        String account = loginDto.getUsername().trim();
        String clientIp = clientIpResolver.resolve(request);
        loginAttemptService.assertAllowed(account, clientIp);

        Result<Void> captchaError = validateCaptcha(loginDto.getCaptcha(), session);
        if (captchaError != null) {
            return Result.error(captchaError.getCode(), captchaError.getMsg());
        }

        User user = findByLoginAccount(account);
        if (user == null || !passwordMatches(loginDto.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(account, clientIp);
            return Result.error(400, "账号或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            loginAttemptService.recordFailure(account, clientIp);
            return Result.error(403, "账号已停用，请联系管理员");
        }

        if (!isBcrypt(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(loginDto.getPassword()));
            userService.updateById(user);
        }
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(0);
        }
        loginAttemptService.recordSuccess(account, clientIp);
        userService.attachRoles(List.of(user));
        request.setAttribute("currentUser", user);

        String token = jwtUtils.generateToken(new JwtUtils.UserTokenClaims(
                user.getId(), user.getUsername(), user.getTokenVersion()));
        RefreshTokenService.RefreshSession refreshSession = refreshTokenService.issue(user, request);
        setRefreshCookie(response, refreshSession.rawToken());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresInMillis", jwtUtils.getExpireTime());
        data.put("user", user);
        data.put("permissionCodes", userService.getPermissionCodesByUserId(user.getId()));
        return Result.success(data);
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@CookieValue(name = "fruit_mall_refresh", required = false) String refreshToken,
                                               HttpServletRequest request, HttpServletResponse response) {
        RefreshTokenService.RefreshSession session = refreshTokenService.rotate(refreshToken, request);
        User user = session.user();
        userService.attachRoles(List.of(user));
        String accessToken = jwtUtils.generateToken(new JwtUtils.UserTokenClaims(
                user.getId(), user.getUsername(), user.getTokenVersion()));
        setRefreshCookie(response, session.rawToken());
        Map<String, Object> data = new HashMap<>();
        data.put("token", accessToken);
        data.put("expiresInMillis", jwtUtils.getExpireTime());
        data.put("user", user);
        data.put("permissionCodes", userService.getPermissionCodesByUserId(user.getId()));
        return Result.success(data);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestAttribute("currentUser") User currentUser) {
        userService.attachRoles(List.of(currentUser));
        Map<String, Object> data = new HashMap<>();
        data.put("user", currentUser);
        data.put("permissionCodes", userService.getPermissionCodesByUserId(currentUser.getId()));
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestAttribute("currentUser") User currentUser, HttpServletResponse response) {
        refreshTokenService.revokeAll(currentUser.getId());
        currentUser.setTokenVersion(normalizedVersion(currentUser) + 1);
        userService.updateById(currentUser);
        clearRefreshCookie(response);
        return Result.success("退出成功");
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody CustomerRegisterDTO registerDTO, HttpSession session) {
        Result<Void> captchaError = validateCaptcha(registerDTO == null ? null : registerDTO.getCaptcha(), session);
        if (captchaError != null) {
            return Result.error(captchaError.getCode(), captchaError.getMsg());
        }
        return Result.success(userService.registerCustomer(registerDTO));
    }

    private User findByLoginAccount(String account) {
        User user = userService.getByUsername(account);
        if (user != null) {
            return user;
        }
        return userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, account)
                .last("LIMIT 1"));
    }

    private Result<Void> validateCaptcha(String input, HttpSession session) {
        String correct = (String) session.getAttribute(CAPTCHA_CODE);
        Instant createdAt = (Instant) session.getAttribute(CAPTCHA_CREATED_AT);
        if (!StringUtils.hasText(input)) {
            return Result.error(400, "请输入验证码");
        }
        if (correct == null || createdAt == null || Instant.now().isAfter(createdAt.plus(CAPTCHA_TTL))) {
            clearCaptcha(session);
            return Result.error(400, "验证码已过期，请点击图片刷新");
        }
        if (!input.equalsIgnoreCase(correct)) {
            Integer failures = (Integer) session.getAttribute(CAPTCHA_FAILURES);
            failures = failures == null ? 1 : failures + 1;
            session.setAttribute(CAPTCHA_FAILURES, failures);
            if (failures >= 5) {
                clearCaptcha(session);
                return Result.error(400, "验证码错误次数过多，请刷新后重试");
            }
            return Result.error(400, "验证码错误");
        }
        clearCaptcha(session);
        return null;
    }

    private void clearCaptcha(HttpSession session) {
        session.removeAttribute(CAPTCHA_CODE);
        session.removeAttribute(CAPTCHA_CREATED_AT);
        session.removeAttribute(CAPTCHA_FAILURES);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        return isBcrypt(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : storedPassword.equals(rawPassword);
    }

    private boolean isBcrypt(String password) {
        return password != null && (password.startsWith("$2a$")
                || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private int normalizedVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, token)
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Lax")
                .path("/auth").maxAge(Duration.ofDays(refreshDays)).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Lax")
                .path("/auth").maxAge(Duration.ZERO).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
