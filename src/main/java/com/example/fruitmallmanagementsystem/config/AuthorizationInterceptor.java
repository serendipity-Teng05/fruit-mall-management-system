package com.example.fruitmallmanagementsystem.config;

import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AuthorizationInterceptor(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission requirement = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirement == null) {
            requirement = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (requirement == null) {
            return true;
        }

        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            writeForbidden(response, "登录状态无效");
            return false;
        }

        Set<String> permissionCodes = new HashSet<>(userService.getPermissionCodesByUserId(currentUser.getId()));
        List<String> roleCodes = userService.getRoleCodesByUserId(currentUser.getId());
        boolean permissionAllowed = requirement.value().length == 0
                || Arrays.stream(requirement.value()).anyMatch(permissionCodes::contains);
        boolean roleAllowed = requirement.roles().length == 0
                || Arrays.stream(requirement.roles()).anyMatch(roleCodes::contains);

        if (!permissionAllowed || !roleAllowed) {
            writeForbidden(response, "没有权限执行此操作");
            return false;
        }
        return true;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("code", 403, "msg", message)));
    }
}
