package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.dto.LoginDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.time.Instant;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class AuthControllerIntegrationTests {

    @Autowired
    private AuthController authController;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void corsAllowsLoopbackViteOrigin() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }

    @Test
    void adminCanLoginAndReceiveTokenAndPermissions() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("CAPTCHA_CODE", "TEST");
        session.setAttribute("CAPTCHA_CREATED_AT", Instant.now());

        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("admin");
        loginDto.setPassword("123456");
        loginDto.setCaptcha("test");

        Result<?> result = authController.login(loginDto, session, new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isInstanceOf(Map.class);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertThat(data.get("token")).isInstanceOf(String.class);
        assertThat((String) data.get("token")).isNotBlank();
        assertThat(data.get("permissionCodes")).isInstanceOf(List.class);
        List<String> permissionCodes = ((List<?>) data.get("permissionCodes"))
                .stream()
                .map(String::valueOf)
                .toList();
        assertThat(permissionCodes).contains("PRODUCT_MANAGE", "ORDER_MANAGE");
    }

    @Test
    void accountCanLoginWithRegisteredPhoneNumber() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("CAPTCHA_CODE", "PHONE");
        session.setAttribute("CAPTCHA_CREATED_AT", Instant.now());

        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("13800138000");
        loginDto.setPassword("123456");
        loginDto.setCaptcha("phone");

        Result<?> result = authController.login(loginDto, session, new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result.getData()).get("token")).isInstanceOf(String.class);
    }
}
