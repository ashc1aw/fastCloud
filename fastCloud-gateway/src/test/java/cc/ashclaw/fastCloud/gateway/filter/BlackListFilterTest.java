package cc.ashclaw.fastCloud.gateway.filter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import cc.ashclaw.common4j.web.util.IpUtil;
import cc.ashclaw.fastCloud.gateway.config.properties.BlackListProperties;
import cc.ashclaw.fastCloud.gateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BlackListFilter 单元测试")
class BlackListFilterTest {

    private BlackListFilter blackListFilter;
    private BlackListProperties blackListProperties;
    private GatewayMetrics gatewayMetrics;
    private MeterRegistry meterRegistry;
    private StringWriter responseWriter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        blackListProperties = new BlackListProperties();
        meterRegistry = new SimpleMeterRegistry();
        gatewayMetrics = new GatewayMetrics(meterRegistry);
        blackListFilter = new BlackListFilter(blackListProperties, gatewayMetrics);
        responseWriter = new StringWriter();
    }

    @Test
    @DisplayName("黑名单功能禁用时应该放行请求")
    void shouldAllowRequestWhenBlacklistDisabled() throws Exception {
        blackListProperties.setEnabled(false);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");

        blackListFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("IP在黑名单中时应该拦截请求")
    void shouldBlockRequestWhenIpInBlacklist() throws Exception {
        List<String> ipBlacklist = new ArrayList<>();
        ipBlacklist.add("192.168.1.100");
        blackListProperties.setEnabled(true);
        blackListProperties.setIpBlacklist(ipBlacklist);

        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(IpUtil.getClientIp(request)).thenReturn("192.168.1.100");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        blackListFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("IP不在黑名单中时应该放行请求")
    void shouldAllowRequestWhenIpNotInBlacklist() throws Exception {
        List<String> ipBlacklist = new ArrayList<>();
        ipBlacklist.add("192.168.1.100");
        blackListProperties.setEnabled(true);
        blackListProperties.setIpBlacklist(ipBlacklist);

        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(IpUtil.getClientIp(request)).thenReturn("192.168.1.101");

        blackListFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("路径在黑名单中时应该拦截请求")
    void shouldBlockRequestWhenPathInBlacklist() throws Exception {
        List<String> pathBlacklist = new ArrayList<>();
        pathBlacklist.add("/admin/**");
        blackListProperties.setEnabled(true);
        blackListProperties.setPathBlacklist(pathBlacklist);

        when(request.getRequestURI()).thenReturn("/admin/users");
        when(request.getMethod()).thenReturn("GET");
        when(IpUtil.getClientIp(request)).thenReturn("192.168.1.1");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        blackListFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("路径不在黑名单中时应该放行请求")
    void shouldAllowRequestWhenPathNotInBlacklist() throws Exception {
        List<String> pathBlacklist = new ArrayList<>();
        pathBlacklist.add("/admin/**");
        blackListProperties.setEnabled(true);
        blackListProperties.setPathBlacklist(pathBlacklist);

        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("GET");
        when(IpUtil.getClientIp(request)).thenReturn("192.168.1.1");

        blackListFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("IP黑名单优先级高于路径黑名单")
    void ipBlacklistShouldHaveHigherPriorityThanPathBlacklist() throws Exception {
        List<String> ipBlacklist = new ArrayList<>();
        ipBlacklist.add("192.168.1.100");
        List<String> pathBlacklist = new ArrayList<>();
        pathBlacklist.add("/api/**");

        blackListProperties.setEnabled(true);
        blackListProperties.setIpBlacklist(ipBlacklist);
        blackListProperties.setPathBlacklist(pathBlacklist);

        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("GET");
        when(IpUtil.getClientIp(request)).thenReturn("192.168.1.100");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        blackListFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("过滤器优先级应该返回配置值")
    void shouldReturnConfiguredOrder() {
        blackListProperties.setOrder(-100);
        assertEquals(-100, blackListFilter.getOrder());
    }
}