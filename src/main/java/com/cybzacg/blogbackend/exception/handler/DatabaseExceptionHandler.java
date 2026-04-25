package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.*;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;

/**
 * 数据库异常处理器。<p>统一捕获 SQL 异常、Spring Data 异常、事务异常和 MyBatis 异常，根据错误特征映射为语义化的错误码。</p>
 */
@Slf4j
@Order(5)
@RestControllerAdvice
public class DatabaseExceptionHandler extends BaseExceptionHandler {
    /**
     * 处理通用 SQL 执行异常。
     *
     * @param e SQL 异常
     * @return 包含数据库执行错误信息的统一响应
     */
    @ExceptionHandler(SQLException.class)
    public Result<Object> handleSqlException(SQLException e) {
        log.error("SQL异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_EXECUTION_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理 Spring DataAccessException 及其子类。
     *
     * @param e 数据访问异常
     * @return 包含数据访问错误信息的统一响应
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Object> handleDataAccessException(DataAccessException e) {
        log.error("数据库访问异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DATA_ACCESS_ERROR,
                "production".equals(profile) ? null
                        : e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    }

    /**
     * 处理乐观锁冲突异常。
     *
     * @param e 乐观锁异常
     * @return 乐观锁失败错误响应
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Result<Object> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
        logException(e, "乐观锁异常");
        return buildErrorResult(ResultErrorCode.DB_OPTIMISTIC_LOCK_FAILURE);
    }

    /**
     * 处理 SQL 语法错误异常。
     *
     * @param e SQL 语法异常
     * @return 包含语法错误信息的统一响应
     */
    @ExceptionHandler(SQLSyntaxErrorException.class)
    public Result<Object> handleSqlSyntaxError(SQLSyntaxErrorException e) {
        log.error("SQL语法错误 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_SYNTAX_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理完整性约束违反异常，根据错误信息自动区分重复键、外键和主键冲突。
     *
     * @param e 完整性约束异常
     * @return 映射为具体错误码的统一响应
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<Object> handleIntegrityConstraintViolation(SQLIntegrityConstraintViolationException e) {
        log.error("完整性约束异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);

        ResultErrorCode resultCode = ResultErrorCode.DATA_INTEGRITY_VIOLATION;
        String errorMsg = e.getMessage();
        if (errorMsg != null) {
            if (errorMsg.contains("Duplicate entry")) {
                resultCode = ResultErrorCode.DATA_ALREADY_EXISTS;
            } else if (errorMsg.contains("foreign key constraint")) {
                resultCode = ResultErrorCode.DB_FOREIGN_KEY_VIOLATION;
            } else if (errorMsg.contains("PRIMARY")) {
                resultCode = ResultErrorCode.DB_PRIMARY_KEY_CONFLICT;
            } else if (errorMsg.contains("UNIQUE")) {
                resultCode = ResultErrorCode.DB_UNIQUE_CONSTRAINT_VIOLATION;
            }
        }

        return buildErrorResult(resultCode,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理 SQL 执行超时异常。
     *
     * @param e SQL 超时异常
     * @return SQL 超时错误响应
     */
    @ExceptionHandler(SQLTimeoutException.class)
    public Result<Object> handleSqlTimeout(SQLTimeoutException e) {
        log.error("SQL执行超时 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_TIMEOUT,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理数据库连接获取失败异常。
     *
     * @param e 连接获取异常
     * @return 数据库连接失败错误响应
     */
    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public Result<Object> handleCannotGetConnection(CannotGetJdbcConnectionException e) {
        log.error("无法获取数据库连接 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_CONNECTION_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理 SQL 语法错误（MyBatis 封装层）。
     *
     * @param e SQL 语法异常
     * @return SQL 语法错误响应
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<Object> handleBadSqlGrammar(BadSqlGrammarException e) {
        log.error("SQL语法错误 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_SYNTAX_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理数据检索失败异常。
     *
     * @param e 数据检索异常
     * @return 数据检索失败错误响应
     */
    @ExceptionHandler(DataRetrievalFailureException.class)
    public Result<Object> handleDataRetrievalFailure(DataRetrievalFailureException e) {
        log.error("数据检索失败 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_DATA_RETRIEVAL_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理数据完整性违反异常（Spring 封装层），根据错误信息区分重复数据、外键和空值违反。
     *
     * @param e 数据完整性异常
     * @return 映射为具体错误码的统一响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("数据完整性违反 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);

        ResultErrorCode resultCode = ResultErrorCode.DATA_INTEGRITY_VIOLATION;
        String errorMsg = e.getMessage();
        if (errorMsg != null) {
            if (errorMsg.contains("Duplicate") || errorMsg.contains("duplicate")) {
                resultCode = ResultErrorCode.DATA_ALREADY_EXISTS;
            } else if (errorMsg.contains("foreign key") || errorMsg.contains("外键")) {
                resultCode = ResultErrorCode.DB_FOREIGN_KEY_VIOLATION;
            } else if (errorMsg.contains("NOT NULL") || errorMsg.contains("null")) {
                resultCode = ResultErrorCode.DB_NOT_NULL_VIOLATION;
            }
        }

        return buildErrorResult(resultCode,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理悲观锁获取失败和死锁异常。
     *
     * @param e 悲观锁或死锁异常
     * @return 映射为悲观锁或死锁错误码的统一响应
     */
    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public Result<Object> handlePessimisticLockingFailure(Exception e) {
        log.error("悲观锁获取失败 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);

        ResultErrorCode resultCode = ResultErrorCode.DB_PESSIMISTIC_LOCK_FAILURE;
        if (e.getMessage() != null
                && (e.getMessage().contains("Deadlock") || e.getMessage().contains("deadlock"))) {
            resultCode = ResultErrorCode.DB_DEADLOCK_DETECTED;
        }

        return buildErrorResult(resultCode,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理事务创建失败异常。
     *
     * @param e 事务创建异常
     * @return 事务失败错误响应
     */
    @ExceptionHandler(CannotCreateTransactionException.class)
    public Result<Object> handleCannotCreateTransaction(CannotCreateTransactionException e) {
        log.error("无法创建事务 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_TRANSACTION_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理事务系统异常。
     *
     * @param e 事务系统异常
     * @return 事务失败错误响应
     */
    @ExceptionHandler(TransactionSystemException.class)
    public Result<Object> handleTransactionSystem(TransactionSystemException e) {
        log.error("事务系统异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_TRANSACTION_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理 MyBatis 系统异常。
     *
     * @param e MyBatis 系统异常
     * @return 映射错误响应
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public Result<Object> handleMyBatisSystem(MyBatisSystemException e) {
        log.error("MyBatis系统异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_MAPPING_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理无效数据访问资源使用异常。
     *
     * @param e 数据访问资源异常
     * @return 映射错误响应
     */
    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public Result<Object> handleInvalidResourceUsage(InvalidDataAccessResourceUsageException e) {
        log.error("无效的数据访问资源使用 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_INVALID_RESOURCE_USAGE,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理非暂时性数据访问异常。
     *
     * @param e 非暂时性数据访问异常
     * @return 映射错误响应
     */
    @ExceptionHandler(NonTransientDataAccessException.class)
    public Result<Object> handleNonTransientDataAccess(NonTransientDataAccessException e) {
        log.error("非暂时性数据访问异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_NON_TRANSIENT_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    /**
     * 处理暂时性数据访问异常。
     *
     * @param e 暂时性数据访问异常
     * @return 映射错误响应
     */
    @ExceptionHandler(TransientDataAccessException.class)
    public Result<Object> handleTransientDataAccess(TransientDataAccessException e) {
        log.error("暂时性数据访问异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_TRANSIENT_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }
}
