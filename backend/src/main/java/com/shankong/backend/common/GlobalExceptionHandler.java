package com.shankong.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import com.shankong.backend.exception.BusinessException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * 作用：Controller 里抛出来的异常，不要让 Spring 用它默认那套
 * {"timestamp":...,"status":500,"trace":"一长串堆栈..."} 返回给前端，
 * 而是统一转成项目自己的 Result 结构。
 *
 * 原来这里只有一个 @ExceptionHandler(Exception.class) 的方法，
 * 参数却写成了 MethodArgumentNotValidException —— 前者匹配所有异常，
 * 后者要求参数必须是那一种异常，两者对不上，Spring 没法解析这个参数，
 * 结果就是：参数校验失败接不住，空指针之类的异常更接不住，
 * 前端看到的还是 Spring 默认的 500 大堆栈。
 * 正确做法是拆成两个方法：一个精确匹配校验异常，一个兜底。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验失败：@Valid 后面的 @NotBlank / @Size / @Email / @Pattern 没通过。
     * 把每条 message 拼起来返回，前端就能直接弹出来给用户看。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败: {}", message);

        return Result.error(ResultCode.PARAM_ERROR.getCode(),
                ResultCode.PARAM_ERROR.getMsg(),
                message);
    }

    /**
     * 请求体根本不是合法 JSON：少引号、多逗号、Content-Type 不是 application/json……
     *
     * 这属于"参数错误"，不是"系统错误"。不单独接住的话它会掉进最下面的兜底，
     * 返回 code=500「系统错误」—— 排查时会被彻底带偏：以为后端崩了，
     * 其实只是 Apifox 里 body 拼错了（实测就是这么复现出 500 的）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());

        return Result.error(ResultCode.PARAM_ERROR.getCode(),
                ResultCode.PARAM_ERROR.getMsg(),
                "请求体不是合法的 JSON，请检查格式，并确认 Content-Type 是 application/json");
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        //如果业务异常携带额外的数据
        if(e.getData() != null) {
            log.error("业务异常: {} {} {}", e.getCode(), e.getMessage(), e.getData());
            return Result.error(e.getCode(), e.getMessage(), e.getData());
        }
        log.error("业务异常: {} {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage(), null);
    }

    /**
     * 兜底：所有没被上面接住的异常（空指针、SQL 错误、类型转换失败……）。
     *
     * 完整堆栈只写进控制台日志给自己排查，返回给前端的只有一句"系统错误"，
     * 不要把 trace 暴露出去（那里面可能带表名、SQL、文件路径）。
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(),
                ResultCode.SYSTEM_ERROR.getMsg(),
                null);
    }
}
