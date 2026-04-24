package com.cybzacg.blogbackend.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/** 文章访问授权表。 */
@TableName(value ="blog_article_access")
@Data
public class BlogArticleAccess {
    /** 主键ID */
    private Long id;
    /** 文章ID */
    private Long articleId;
    /** 授权用户ID */
    private Long userId;
    /** 授权类型：1-密码访问，2-指定用户 */
    private Integer accessType;
    /** 授权时间 */
    private Date grantTime;
    /** 过期时间 */
    private Date expireTime;
    /** 授权原因 */
    private String grantReason;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
}
