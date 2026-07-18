package com.example.fruitmallmanagementsystem.service;

import com.example.fruitmallmanagementsystem.config.ClientIpResolver;
import com.example.fruitmallmanagementsystem.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class RefreshTokenService {
    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final ClientIpResolver clientIpResolver;
    private final long refreshDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(JdbcTemplate jdbcTemplate, UserService userService, ClientIpResolver clientIpResolver,
                               @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.clientIpResolver = clientIpResolver;
        this.refreshDays = Math.max(1, Math.min(30, refreshDays));
    }

    @Transactional(rollbackFor = Exception.class)
    public RefreshSession issue(User user, HttpServletRequest request) {
        String rawToken = newRawToken();
        insertToken(rawToken, user, request);
        jdbcTemplate.update("DELETE FROM sys_refresh_token WHERE expires_at < DATE_SUB(NOW(), INTERVAL 7 DAY)");
        return new RefreshSession(rawToken, user);
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public RefreshSession rotate(String rawToken, HttpServletRequest request) {
        if (!StringUtils.hasText(rawToken)) throw new IllegalStateException("登录续期凭证不存在");
        String tokenHash = sha256(rawToken);
        List<TokenRow> rows = jdbcTemplate.query("SELECT id, user_id, token_version, expires_at, revoked_at "
                        + "FROM sys_refresh_token WHERE token_hash = ? FOR UPDATE", (rs, rowNum) -> new TokenRow(
                        rs.getLong("id"), rs.getLong("user_id"), rs.getInt("token_version"),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toInstant()), tokenHash);
        if (rows.isEmpty()) throw new IllegalStateException("登录续期凭证无效");

        TokenRow row = rows.get(0);
        if (row.revokedAt() != null) {
            revokeAll(row.userId());
            invalidateUserSessions(row.userId());
            throw new IllegalStateException("检测到已使用的登录凭证，所有会话已注销");
        }
        if (!row.expiresAt().isAfter(Instant.now())) {
            jdbcTemplate.update("UPDATE sys_refresh_token SET revoked_at = NOW() WHERE id = ?", row.id());
            throw new IllegalStateException("登录续期凭证已过期");
        }

        User user = userService.getById(row.userId());
        int currentVersion = user == null || user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (user == null || Integer.valueOf(0).equals(user.getStatus()) || row.tokenVersion() != currentVersion) {
            revokeAll(row.userId());
            throw new IllegalStateException("账号状态已变化，请重新登录");
        }

        String replacement = newRawToken();
        String replacementHash = sha256(replacement);
        jdbcTemplate.update("UPDATE sys_refresh_token SET revoked_at = NOW(), last_used_at = NOW(), replaced_by_hash = ? "
                + "WHERE id = ? AND revoked_at IS NULL", replacementHash, row.id());
        insertToken(replacement, user, request);
        return new RefreshSession(replacement, user);
    }

    public void revokeAll(Long userId) {
        if (userId != null) {
            jdbcTemplate.update("UPDATE sys_refresh_token SET revoked_at = COALESCE(revoked_at, NOW()) "
                    + "WHERE user_id = ? AND revoked_at IS NULL", userId);
        }
    }

    private void invalidateUserSessions(Long userId) {
        User user = userService.getById(userId);
        if (user != null) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            userService.updateById(user);
        }
    }

    private void insertToken(String rawToken, User user, HttpServletRequest request) {
        Instant expiresAt = Instant.now().plus(refreshDays, ChronoUnit.DAYS);
        String userAgent = request.getHeader("User-Agent");
        jdbcTemplate.update("INSERT INTO sys_refresh_token "
                        + "(token_hash, user_id, token_version, expires_at, ip_hash, user_agent_hash) VALUES (?, ?, ?, ?, ?, ?)",
                sha256(rawToken), user.getId(), user.getTokenVersion() == null ? 0 : user.getTokenVersion(),
                Timestamp.from(expiresAt), sha256(clientIpResolver.resolve(request)),
                StringUtils.hasText(userAgent) ? sha256(userAgent.substring(0, Math.min(500, userAgent.length()))) : null);
    }

    private String newRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", impossible);
        }
    }

    public record RefreshSession(String rawToken, User user) {}
    private record TokenRow(long id, long userId, int tokenVersion, Instant expiresAt, Instant revokedAt) {}
}
