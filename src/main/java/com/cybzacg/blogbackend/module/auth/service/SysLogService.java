package com.cybzacg.blogbackend.module.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysLog;

/**
 * 系统日志服务接口。
 *
 * <p>定义系统日志相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysLogService extends IService<SysLog> {

    /**
     * 使用独立事务保存日志，避免主业务回滚时日志丢失。
     */
    void saveLog(SysLog sysLog);
}
