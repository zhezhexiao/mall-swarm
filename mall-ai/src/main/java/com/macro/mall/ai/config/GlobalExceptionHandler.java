package com.macro.mall.ai.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.macro.mall.common.api.CommonResult;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理 — 未登录返回 401
 */
@RestControllerAdvice
@Order(-1)
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CommonResult<?> handleNotLogin(NotLoginException e) {
        return CommonResult.unauthorized("请先登录");
    }
}
