package cc.ashclaw.fastCloud.gateway.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import cc.ashclaw.fastCloud.gateway.config.properties.RetryProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 请求重试过滤器 - WebMVC版本
 *
 * <p>该过滤器为下游服务调用提供重试能力，使用RestTemplate执行HTTP请求。
 * 当下游服务返回可重试的错误码或网络异常时，自动进行重试。
 *
 * <p>主要功能：
 * <ul>
 *   <li>支持配置可重试的HTTP状态码（500,502,503,504等）</li>
 *   <li>支持配置可重试的HTTP方法</li>
 *   <li>支持指数退避策略</li>
 *   <li>可配置最大重试次数</li>
 *   <li>记录重试日志便于排查问题</li>
 * </ul>
 *
 * <p>注意：
 * <ul>
 *   <li>该过滤器仅在网关直连下游服务场景下生效</li>
 *   <li>通过路由转发的请求重试需要在路由配置中设置</li>
 *   <li>幂等性请求（GET,PUT,DELETE）可以安全重试</li>
 *   <li>非幂等性请求（POST）重试需谨慎</li>
 * </ul>
 *
 * @author ashclaw
 * @since JDK 25
 */
@Slf4j
@Component
public class RetryFilter implements Ordered {

    private static final int RETRY_ORDER = 50;

    private final RetryProperties retryProperties;
    private final RestTemplate retryRestTemplate;

    /**
     * 构造函数
     *
     * @param retryProperties 重试配置属性
     */
    public RetryFilter(RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.retryRestTemplate = new RestTemplate(factory);
    }

    /**
     * 执行带重试的HTTP请求
     *
     * <p>根据配置的重试策略执行HTTP请求，支持指数退避算法。
     * 当遇到可重试的状态码或网络异常时，会按照配置进行重试。
     *
     * @param request HTTP请求
     * @param body 请求体
     * @return HTTP响应
     * @throws IOException 如果请求失败或重试次数耗尽
     */
    public ClientHttpResponse executeWithRetry(HttpRequest request, byte[] body) throws IOException {
        if (!retryProperties.isEnabled()) {
            return retryRestTemplate.execute(request.getURI(), request.getMethod(), null, (clientHttpRequest) -> {
                clientHttpRequest.getHeaders().putAll(request.getHeaders());
                return clientHttpRequest;
            });
        }

        String uri = request.getURI().toString();
        String method = request.getMethod().name();
        RetryProperties.Backoff backoff = retryProperties.getBackoff();
        long currentInterval = backoff.getInitialInterval();

        for (int attempt = 1; attempt <= retryProperties.getMaxAttempts(); attempt++) {
            try {
                ClientHttpResponse response = retryRestTemplate.execute(
                        request.getURI(),
                        request.getMethod(),
                        null,
                        (clientHttpRequest) -> {
                            clientHttpRequest.getHeaders().putAll(request.getHeaders());
                            return clientHttpRequest;
                        }
                );

                if (response != null && isRetryableStatus(response.getStatusCode().value())) {
                    if (attempt < retryProperties.getMaxAttempts()) {
                        log.warn("请求失败准备重试 - URI: {}, Method: {}, 状态码: {}, 尝试: {}/{}",
                                uri, method, response.getStatusCode().value(), attempt, retryProperties.getMaxAttempts());
                        closeResponse(response);
                        sleep(currentInterval);
                        currentInterval = (long) (currentInterval * backoff.getMultiplier());
                        currentInterval = Math.min(currentInterval, backoff.getMaxInterval());
                        continue;
                    }
                }

                return response;
            } catch (Exception e) {
                if (attempt >= retryProperties.getMaxAttempts()) {
                    log.error("重试次数耗尽仍失败 - URI: {}, Method: {}, 尝试: {}/{}, 错误: {}",
                            uri, method, attempt, retryProperties.getMaxAttempts(), e.getMessage());
                    throw e instanceof IOException ? (IOException) e : new IOException(e);
                }

                log.warn("请求异常准备重试 - URI: {}, Method: {}, 尝试: {}/{}, 错误: {}",
                        uri, method, attempt, retryProperties.getMaxAttempts(), e.getMessage());
                sleep(currentInterval);
                currentInterval = (long) (currentInterval * backoff.getMultiplier());
                currentInterval = Math.min(currentInterval, backoff.getMaxInterval());
            }
        }

        throw new IOException("重试次数耗尽");
    }

    /**
     * 判断HTTP状态码是否可重试
     *
     * @param status HTTP状态码
     * @return true表示可重试，false表示不可重试
     */
    private boolean isRetryableStatus(int status) {
        return retryProperties.getRetryableStatusCodes().contains(status);
    }

    /**
     * 关闭HTTP响应
     *
     * @param response HTTP响应
     */
    private void closeResponse(ClientHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (Exception e) {
            log.debug("关闭响应失败: {}", e.getMessage());
        }
    }

    /**
     * 线程休眠
     *
     * @param millis 休眠时间（毫秒）
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int getOrder() {
        return RETRY_ORDER;
    }
}