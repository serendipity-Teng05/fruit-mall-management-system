package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.SysLog;
import com.example.fruitmallmanagementsystem.mapper.SysLogMapper;
import com.example.fruitmallmanagementsystem.service.SysLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    public List<SysLog> getLogsByUserId(Long userId) {
        return list(new LambdaQueryWrapper<SysLog>()
                .eq(SysLog::getUserId, userId)
                .orderByDesc(SysLog::getCreateTime));
    }

    @Override
    public void addSysLog(SysLog log) {
        save(log);
    }

    @Override
    public Page<SysLog> pageList(Integer pageNum, Integer pageSize, Long userId, String module, String action) {
        Page<SysLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(SysLog::getUserId, userId);
        }
        if (StringUtils.hasText(module)) {
            wrapper.like(SysLog::getModule, module.trim());
        }
        if (StringUtils.hasText(action)) {
            wrapper.like(SysLog::getAction, action.trim());
        }

        wrapper.orderByDesc(SysLog::getCreateTime);
        return page(page, wrapper);
    }

}
