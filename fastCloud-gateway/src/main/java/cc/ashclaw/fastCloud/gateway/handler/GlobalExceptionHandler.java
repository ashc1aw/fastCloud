package cc.ashclaw.fastCloud.gateway.handler;

import cc.ashclaw.common4j.core.model.Result;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 - WebMVC版本
 *
 * <p>该处理器负责统一处理网关层抛出的所有异常，返回标准化的错误响应。
 * 通过@RestControllerAdvice实现对所有Controller异常的统一捕获和处理。
 *
 * <p>处理的异常类型：
 * <ul>
 *   <li>NoResourceFoundException: 资源未找到（404）</li>
 *   <li>ResponseStatusException: 响应状态异常</li>
 *   <li>TimeoutException: 服务超时（504）</li>
 *   <li>ConnectException: 服务连接失败（503）</li>
 *   <li>MethodArgumentNotValidException: 参数校验失败（400）</li>
 *   <li>BindException: 参数绑定失败（400）</li>
 *   <li>MethodArgumentTypeMismatchException: 参数类型不匹配（400）</li>
 *   <li>HttpMessageNotReadableException: 请求体格式错误（400）</li>
 *   <li>Exception: 其他未处理异常（500）</li>
 * </ul>
 *
 * <p>响应格式：
 * <pre>
 * {
 *   "code": 状态码,
 *   "msg": "错误信息",
 *   "data": null
 * }
 * </pre>
 *
 * @author ashclaw
 * @since JDK 25
 * @see Result
 */
@Slf4j
@Order(-1)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理资源未找到异常
     *
     * <p>当请求的路径没有对应的Controller处理时触发。
     *
     * @param e 资源未找到异常
     * @param request HTTP请求对象
     * @return 404响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<?>> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("资源未找到: {}", request.getRequestURI());
        String message = "未找到路径 '" + request.getRequestURI() + "' 对应的资源";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(HttpStatus.NOT_FOUND.value(), message));
    }

    /**
     * 处理响应状态异常
     *
     * <p>处理带HTTP状态码的异常，如NotFound、BadRequest等。
     *
     * @param e 响应状态异常
     * @param request HTTP请求对象
     * @return 对应状态的响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<?>> handleResponseStatusException(ResponseStatusException e, HttpServletRequest request) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            log.warn("资源未找到: {}", request.getRequestURI());
            String message = "未找到路径 '" + request.getRequestURI() + "' 对应的资源";
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.error(HttpStatus.NOT_FOUND.value(), message));
        }
        String message = e.getReason() != null ? e.getReason() : e.getMessage();
        log.error("响应状态异常: {}", message, e);
        return ResponseEntity.status(e.getStatusCode())
                .body(Result.error(e.getStatusCode().value(), message));
    }

    /**
     * 处理超时异常
     *
     * <p>当后端服务响应超时时触发。
     *
     * @param e 超时异常
     * @param request HTTP请求对象
     * @return 504响应
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Result<?>> handleTimeout(TimeoutException e, HttpServletRequest request) {
        log.error("服务超时", e);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Result.error(HttpStatus.GATEWAY_TIMEOUT.value(), "服务响应超时"));
    }

    /**
     * 处理连接异常
     *
     * <p>当无法连接到后端服务时触发。
     *
     * @param e 连接异常
     * @param request HTTP请求对象
     * @return 503响应
     */
    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<Result<?>> handleConnectException(ConnectException e, HttpServletRequest request) {
        log.error("服务连接失败", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "目标服务连接失败: " + e.getMessage()));
    }

    /**
     * 处理参数校验异常
     *
     * <p>当使用@Valid注解进行参数校验失败时触发。
     *
     * @param e 参数校验异常
     * @param request HTTP请求对象
     * @return 400响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        String message = "参数校验失败: " + (errors.isEmpty() ? "无效的请求参数" : errors);
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理参数绑定异常
     *
     * <p>当请求参数绑定到方法参数失败时触发。
     *
     * @param e 绑定异常
     * @param request HTTP请求对象
     * @return 400响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<?>> handleBindException(BindException e, HttpServletRequest request) {
        String message = "参数绑定失败: " + e.getMessage();
        log.warn("参数绑定失败", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理参数类型不匹配异常
     *
     * <p>当请求参数类型与方法期望的类型不匹配时触发。
     *
     * @param e 类型不匹配异常
     * @param request HTTP请求对象
     * @return 400响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("参数类型错误: %s 应该为 %s", e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型");
        log.warn("参数类型不匹配: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理请求体不可读异常
     *
     * <p>当请求体JSON格式错误或无法解析时触发。
     *
     * @param e 请求体异常
     * @param request HTTP请求对象
     * @return 400响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String message;
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException ife) {
            message = "JSON 格式错误: " + ife.getOriginalMessage();
        } else {
            message = "请求体格式错误或不可读";
        }
        log.warn("请求体不可读", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理通用异常
     *
     * <p>处理所有其他未专门处理的异常。
     *
     * @param e 异常对象
     * @param request HTTP请求对象
     * @return 500响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("未处理的异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误"));
    }
}