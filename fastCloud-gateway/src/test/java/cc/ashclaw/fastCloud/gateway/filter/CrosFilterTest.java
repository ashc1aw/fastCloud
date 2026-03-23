package cc.ashclaw.fastCloud.gateway.filter;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import cc.ashclaw.fastCloud.gateway.config.properties.CorsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CrosFilter 单元测试")
class CrosFilterTest {

    private CrosFilter crosFilter;
    private CorsProperties corsProperties;
    private StringWriter responseWriter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
        crosFilter = new CrosFilter(corsProperties);
        responseWriter = new StringWriter();
    }

    @Test
    @DisplayName("CORS功能禁用时应该直接放行")
    void shouldPassThroughWhenCorsDisabled() throws Exception {
        corsProperties.setEnabled(false);

        crosFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("OPTIONS预检请求应该直接返回200不继续过滤链")
    void shouldReturn200ForOptionsRequest() throws Exception {
        corsProperties.setEnabled(true);
        when(request.getMethod()).thenReturn("OPTIONS");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        crosFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("非OPTIONS请求应该添加CORS头并继续过滤链")
    void shouldAddCorsHeadersForNonOptionsRequest() throws Exception {
        corsProperties.setEnabled(true);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Origin")).thenReturn("https://example.com");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        crosFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("Access-Control-Allow-Origin", "https://example.com");
        verify(response).setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS,HEAD");
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("允许来源为*时应该使用请求的Origin")
    void shouldUseRequestOriginWhenAllowAll() throws Exception {
        corsProperties.setEnabled(true);
        corsProperties.setAllowedOrigins("*");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn("https://other-domain.com");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        crosFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("Access-Control-Allow-Origin", "https://other-domain.com");
    }

    @Test
    @DisplayName("允许来源为具体域名时应该使用配置的域名")
    void shouldUseConfiguredOrigins() throws Exception {
        corsProperties.setEnabled(true);
        corsProperties.setAllowedOrigins("https://trusted-site.com");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Origin")).thenReturn("https://untrusted-site.com");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        crosFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("Access-Control-Allow-Origin", "https://trusted-site.com");
    }

    @Test
    @DisplayName("不携带凭证时不应设置Access-Control-Allow-Credentials头")
    void shouldNotSetCredentialsHeaderWhenDisabled() throws Exception {
        corsProperties.setEnabled(true);
        corsProperties.setAllowCredentials(false);
        when(request.getMethod()).thenReturn("GET");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        crosFilter.doFilterInternal(request, response, filterChain);

        verify(response, never()).setHeader(eq("Access-Control-Allow-Credentials"), anyString());
    }
}