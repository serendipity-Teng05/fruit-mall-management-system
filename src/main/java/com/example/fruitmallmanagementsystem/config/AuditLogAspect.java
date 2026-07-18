package com.example.fruitmallmanagementsystem.config;

import com.example.fruitmallmanagementsystem.entity.SysLog;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditLogAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private final SysLogService sysLogService;
    private final ClientIpResolver clientIpResolver;

    public AuditLogAspect(SysLogService sysLogService, ClientIpResolver clientIpResolver) {
        this.sysLogService = sysLogService;
        this.clientIpResolver = clientIpResolver;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping))")
    public Object recordMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable failure = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            saveLog(joinPoint, start, failure);
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, long start, Throwable failure) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();
            User user = (User) request.getAttribute("currentUser");

            SysLog audit = new SysLog();
            audit.setUserId(user == null ? null : user.getId());
            audit.setUsername(user == null ? null : user.getUsername());
            audit.setModule(moduleName(joinPoint));
            audit.setAction(joinPoint.getSignature().getName());
            audit.setRemark(failure == null ? "操作成功" : safeMessage(failure));
            audit.setIpAddress(clientIpResolver.resolve(request));
            audit.setHttpMethod(request.getMethod());
            audit.setRequestUri(request.getRequestURI());
            audit.setRequestId(requestId(request));
            audit.setSuccess(failure == null ? 1 : 0);
            audit.setDurationMs(System.currentTimeMillis() - start);
            audit.setCreateTime(LocalDateTime.now());
            sysLogService.addSysLog(audit);
        } catch (Exception auditFailure) {
            log.warn("Unable to persist audit log", auditFailure);
        }
    }

    private String moduleName(ProceedingJoinPoint joinPoint) {
        String name = joinPoint.getTarget().getClass().getSimpleName();
        return name.endsWith("Controller") ? name.substring(0, name.length() - 10) : name;
    }

    private String requestId(HttpServletRequest request) {
        Object id = request.getAttribute(RequestTracingFilter.REQUEST_ID_ATTRIBUTE);
        return id == null ? null : id.toString();
    }

    private String safeMessage(Throwable failure) {
        String message = StringUtils.hasText(failure.getMessage()) ? failure.getMessage() : failure.getClass().getSimpleName();
        return "操作失败：" + message.substring(0, Math.min(220, message.length()));
    }
}
