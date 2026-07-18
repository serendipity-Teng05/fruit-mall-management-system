package com.example.fruitmallmanagementsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fruitmallmanagementsystem.entity.SysLog;

import java.util.List;

public interface SysLogService extends IService<SysLog> {
    List<SysLog> getLogsByUserId(Long userId);

    void addSysLog(SysLog log);

    Page<SysLog> pageList(Integer pageNum, Integer pageSize, Long userId, String module, String action);

}
