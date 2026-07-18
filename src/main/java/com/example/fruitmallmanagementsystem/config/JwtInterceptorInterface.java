package com.example.fruitmallmanagementsystem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface JwtInterceptorInterface {
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
}
