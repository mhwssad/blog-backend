package com.cybzacg.blogbackend.exception.handler;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
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
 * 数据库异常处理器
 */
@Slf4j
@Order(5)
@RestControllerAdvice
public class DatabaseExceptionHandler extends BaseExceptionHandler {
    @ExceptionHandler(SQLException.class)
    public Result<Object> handleSqlException(SQLException e) {
        log.error("SQL异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_EXECUTION_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public Result<Object> handleDataAccessException(DataAccessException e) {
        log.error("数据库访问异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DATA_ACCESS_ERROR,
                "production".equals(profile) ? null
                        : e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Result<Object> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
        logException(e, "乐观锁异常");
        return buildErrorResult(ResultErrorCode.DB_OPTIMISTIC_LOCK_FAILURE);
    }

    @ExceptionHandler(SQLSyntaxErrorException.class)
    public Result<Object> handleSqlSyntaxError(SQLSyntaxErrorException e) {
        log.error("SQL语法错误 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_SYNTAX_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

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

    @ExceptionHandler(SQLTimeoutException.class)
    public Result<Object> handleSqlTimeout(SQLTimeoutException e) {
        log.error("SQL执行超时 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_TIMEOUT,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public Result<Object> handleCannotGetConnection(CannotGetJdbcConnectionException e) {
        log.error("无法获取数据库连接 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_CONNECTION_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<Object> handleBadSqlGrammar(BadSqlGrammarException e) {
        log.error("SQL语法错误 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_SQL_SYNTAX_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(DataRetrievalFailureException.class)
    public Result<Object> handleDataRetrievalFailure(DataRetrievalFailureException e) {
        log.error("数据检索失败 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_DATA_RETRIEVAL_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

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

    @ExceptionHandler(CannotCreateTransactionException.class)
    public Result<Object> handleCannotCreateTransaction(CannotCreateTransactionException e) {
        log.error("无法创建事务 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_TRANSACTION_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(TransactionSystemException.class)
    public Result<Object> handleTransactionSystem(TransactionSystemException e) {
        log.error("事务系统异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_TRANSACTION_FAILED,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(MyBatisSystemException.class)
    public Result<Object> handleMyBatisSystem(MyBatisSystemException e) {
        log.error("MyBatis系统异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_MAPPING_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public Result<Object> handleInvalidResourceUsage(InvalidDataAccessResourceUsageException e) {
        log.error("无效的数据访问资源使用 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_INVALID_RESOURCE_USAGE,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(NonTransientDataAccessException.class)
    public Result<Object> handleNonTransientDataAccess(NonTransientDataAccessException e) {
        log.error("非暂时性数据访问异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_NON_TRANSIENT_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }

    @ExceptionHandler(TransientDataAccessException.class)
    public Result<Object> handleTransientDataAccess(TransientDataAccessException e) {
        log.error("暂时性数据访问异常 [TraceID: {}] - {}", getTraceId(), e.getMessage(), e);
        return buildErrorResult(ResultErrorCode.DB_TRANSIENT_ERROR,
                "production".equals(profile) ? null : e.getMessage());
    }
}
