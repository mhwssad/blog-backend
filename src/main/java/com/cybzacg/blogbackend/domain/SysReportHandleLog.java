package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报处理日志表。
 */
@Data
@TableName("sys_report_handle_log")
public class SysReportHandleLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 举报单ID */
    private Long reportId;
    /** 变更前状态 */
    private Integer fromStatus;
    /** 变更后状态 */
    private Integer toStatus;
    /** 动作类型：claim/approve/reject/close/reassign */
    private String actionType;
    /** 动作结果/处罚类型 */
    private String actionResult;
    /** 操作人ID */
    private Long operatorUserId;
    /** 处理备注 */
    private String actionRemark;
    private LocalDateTime createdAt;
}
