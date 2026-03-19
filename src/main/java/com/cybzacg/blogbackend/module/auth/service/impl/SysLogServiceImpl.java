package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.mapper.SysLogMapper;
import com.cybzacg.blogbackend.module.auth.service.SysLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统日志服务实现。
 */
@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    /**
     * 单独开启新事务保存日志，确保日志记录尽量不受主事务影响。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLog(SysLog sysLog) {
        save(sysLog);
    }
}
