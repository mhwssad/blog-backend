package com.cybzacg.blogbackend.dto.domain.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报单主表。
 */
@Data
@TableName("sys_report_record")
public class SysReportRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 举报对象类型：article/comment/chat_message */
    private String reportTargetType;
    /** 举报对象ID */
    private Long reportTargetId;
    /** 举报人ID */
    private Long reporterUserId;
    /** 举报原因编码 */
    private String reasonCode;
    /** 举报说明 */
    private String reasonDetail;
    /** 状态：0-待处理，1-处理中，2-已处理，3-已驳回 */
    private Integer status;
    /** 当前处理人ID */
    private Long handlerUserId;
    /** 处理结果 */
    private String resultType;
    /** 处罚类型 */
    private String punishmentType;
    /** 补充证据JSON */
    private String evidenceJson;
    /** 举报时间 */
    private LocalDateTime reportedAt;
    /** 处理完成时间 */
    private LocalDateTime handledAt;
    /** 备注 */
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
