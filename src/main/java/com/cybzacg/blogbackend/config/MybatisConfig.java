package com.cybzacg.blogbackend.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;

/**
 * MyBatis-Plus 全局配置。<p>注册分页插件、字段自动填充处理器和数据库类型自动识别提供器，并通过 MapperScan 指定 Mapper 扫描路径。</p>
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.cybzacg.blogbackend.mapper")
public class MybatisConfig {

    @Value("${app.db-type:mysql}")
    private String dbType;

    /**
     * 注册 MyBatis-Plus 拦截器，包含分页插件（支持根据配置动态选择数据库类型）。
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 数据权限
//        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new MyDataPermissionHandler()));

        // 分页插件，根据配置动态选择数据库类型
        DbType mpDbType = DbType.MYSQL;
        String type = dbType == null ? "mysql" : dbType.toLowerCase();
        if ("postgres".equals(type) || "postgresql".equals(type)) {
            mpDbType = DbType.POSTGRE_SQL;
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(mpDbType));

        return interceptor;
    }

    /**
     * 注册全局配置，绑定字段自动填充处理器。
     *
     * @return GlobalConfig 实例
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(new MyMetaObjectHandler());
        return globalConfig;
    }

    /**
     * 注册数据库类型自动识别提供器，用于 MyBatis 多数据库 SQL 适配。
     *
     * @return DatabaseIdProvider 实例
     */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("MySQL", "mysql");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }

}
