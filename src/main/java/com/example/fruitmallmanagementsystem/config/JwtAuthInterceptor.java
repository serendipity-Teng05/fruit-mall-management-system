package com.example.fruitmallmanagementsystem.config;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.UserService;
import com.example.fruitmallmanagementsystem.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(UserService userService, JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(response, "登录状态无效，请重新登录");
        }
        String token = authHeader.substring(7);
        String username = jwtUtils.getUsernameFromToken(token);
        Integer tokenVersion = jwtUtils.getTokenVersion(token);
        if (username == null || tokenVersion == null) {
            return unauthorized(response, "登录已过期，请重新登录");
        }
        User user = userService.getByUsername(username);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            return unauthorized(response, "账号不存在或已停用");
        }
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (!Objects.equals(tokenVersion, currentVersion)) {
            return unauthorized(response, "登录已失效，请重新登录");
        }
        request.setAttribute("currentUser", user);
        return true;
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, message)));
        return false;
    }
}
