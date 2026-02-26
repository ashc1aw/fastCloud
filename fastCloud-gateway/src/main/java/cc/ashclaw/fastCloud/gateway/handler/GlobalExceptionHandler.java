package cc.ashclaw.fastCloud.gateway.handler;

import cc.ashclaw.common4j.core.util.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Order(-1)
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<String> handleException(HttpServletRequest request, Exception ex) {
        HttpStatusCode httpStatus;
        String message;

        if (ex instanceof ResponseStatusException responseStatusException) {
            httpStatus = responseStatusException.getStatusCode();
            message = responseStatusException.getReason();
            if (StringUtil.isEmpty(message)) {
                message = responseStatusException.getMessage();
            }

            if (httpStatus == HttpStatus.NOT_FOUND) {
                message = "服务未找到";
            }
        } else if (ex instanceof TimeoutException) {
            httpStatus = HttpStatus.GATEWAY_TIMEOUT;
            message = "服务超时";
        } else if (ex instanceof ConnectException) {
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
            message = "服务连接失败";
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "内部服务器错误";
        }

        log.error("[网关异常] 路径: {}, 类型: {}, 状态: {}, 消息: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), httpStatus, message, ex);

        return ResponseEntity.status(httpStatus).body(message);
    }
}
