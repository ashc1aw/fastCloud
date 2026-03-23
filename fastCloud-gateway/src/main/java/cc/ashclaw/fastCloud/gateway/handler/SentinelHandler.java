package cc.ashclaw.fastCloud.gateway.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel限流降级处理器 - WebMVC版本
 *
 * <p>该处理器通过Spring MVC的HandlerInterceptor机制实现Sentinel限流功能。
 * 在请求处理前通过SphU.entry()尝试获取资源，如果被限流或降级则返回友好的错误响应。
 *
 * <p>主要功能：
 * <ul>
 *   <li>基于URI的限流：自动将请求URI作为Sentinel资源名</li>
 *   <li>多维度限流处理：FlowException(流量控制)、DegradeException(降级)</li>
 *   <li>系统自适应限流：SystemBlockException(系统负载过高)</li>
 * </ul>
 *
 * <p>处理流程：
 * <pre>
 * 请求 → preHandle() → SphU.entry() → 通过：继续处理
 *                          ↓
 *                    抛出BlockException → handleBlockException() → 返回限流响应
 *                          ↓
 *                    afterCompletion() → 记录异常
 * </pre>
 *
 * <p>响应格式：
 * <pre>
 * {
 *   "code": 403,
 *   "msg": "请求过于频繁，请稍后重试",
 *   "data": null
 * }
 * </pre>
 *
 * <p>异常类型与提示信息对应关系：
 * <ul>
 *   <li>FlowException: "请求过于频繁，请稍后重试"</li>
 *   <li>DegradeException: "服务已降级，请稍后重试"</li>
 *   <li>SystemBlockException: "系统负载过高，请稍后重试"</li>
 * </ul>
 *
 * @author ashclaw
 * @since JDK 25
 * @see HandlerInterceptor
 * @see BlockException
 */
@Slf4j
@Component
public class SentinelHandler implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 前置处理方法
     *
     * <p>该方法在请求处理前执行，尝试通过Sentinel检查资源是否可访问。
     * 使用try-with-resources确保Entry正确释放。
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象
     * @return true表示请求可以继续处理，false表示请求被限流拦截
     * @throws Exception 如果处理时发生异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String resource = request.getRequestURI();

        try (Entry entry = SphU.entry(resource, EntryType.IN)) {
            return true;
        } catch (BlockException e) {
            handleBlockException(response, e, resource);
            return false;
        }
    }

    /**
     * 处理Sentinel拦截异常
     *
     * <p>根据异常类型返回不同的友好提示信息，并设置HTTP响应状态码为403 Forbidden。
     *
     * <p>异常类型与提示信息对应关系：
     * <ul>
     *   <li>FlowException: "请求过于频繁，请稍后重试"</li>
     *   <li>DegradeException: "服务已降级，请稍后重试"</li>
     *   <li>SystemBlockException: "系统负载过高，请稍后重试"</li>
     * </ul>
     *
     * @param response HTTP响应对象
     * @param e Sentinel拦截异常
     * @param resource 资源名称
     * @throws IOException 如果写入响应时发生IO异常
     */
    private void handleBlockException(HttpServletResponse response, BlockException e, String resource) throws IOException {
        log.warn("Sentinel拦截 - 资源: {}, 异常类型: {}, 异常信息: {}",
                resource, e.getClass().getSimpleName(), e.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("data", null);

        if (e instanceof FlowException) {
            result.put("msg", "请求过于频繁，请稍后重试");
        } else if (e instanceof DegradeException) {
            result.put("msg", "服务已降级，请稍后重试");
        } else if (e instanceof SystemBlockException) {
            result.put("msg", "系统负载过高，请稍后重试");
        } else {
            result.put("msg", StringUtils.hasText(e.getMessage()) ? e.getMessage() : "请求被限流");
        }

        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }

    /**
     * 请求完成后处理
     *
     * <p>该方法在请求处理完成后调用，用于记录非BlockException的异常。
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理器对象
     * @param ex 抛出的异常（如果有）
     * @throws Exception 如果处理时发生异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (ex != null && !(ex instanceof BlockException)) {
            Tracer.trace(ex);
            log.error("请求处理异常 - URI: {}, 异常: {}", request.getRequestURI(), ex.getMessage());
        }
    }
}