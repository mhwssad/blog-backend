package com.cybzacg.blogbackend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记当前控制器或方法不记录系统操作日志。<p>可标注在类或方法上，{@link com.cybzacg.blogbackend.common.aspect.SysLogAspect} 会据此跳过日志采集。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DisableSysLog {
}
