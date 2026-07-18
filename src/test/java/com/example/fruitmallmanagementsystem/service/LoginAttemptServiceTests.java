package com.example.fruitmallmanagementsystem.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTests {
    @Test
    void locksAccountAndIpAfterFiveFailuresAndResetsOnSuccess() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) service.recordFailure("demo", "127.0.0.1");
        assertThatThrownBy(() -> service.assertAllowed("demo", "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("登录失败次数过多");

        service.recordSuccess("demo", "127.0.0.1");
        service.assertAllowed("demo", "127.0.0.1");
    }
}
