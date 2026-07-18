package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.service.SysLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys-log")
@RequirePermission("SYS_LOG")
public class SysLogController {
    private final SysLogService sysLogService;

    public SysLogController(SysLogService sysLogService) {
        this.sysLogService = sysLogService;
    }

    @GetMapping("/user/{userId}")
    public Result<?> getLogsByUser(@PathVariable Long userId) {
        return Result.success(sysLogService.getLogsByUserId(userId));
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Long userId,
                          @RequestParam(required = false) String module,
                          @RequestParam(required = false) String action,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(sysLogService.pageList(pageNum, pageSize, userId, module, action));
    }
}
