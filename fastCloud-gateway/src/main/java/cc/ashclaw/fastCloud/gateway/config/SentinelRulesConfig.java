package cc.ashclaw.fastCloud.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;

import cc.ashclaw.fastCloud.gateway.config.properties.SentinelRulesProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel规则配置类
 *
 * <p>该类负责在应用启动时初始化Sentinel的限流规则、降级规则和系统规则。
 * 配置通过SentinelRulesProperties类从application.yml读取，支持Nacos动态刷新。
 *
 * <p>主要功能：
 * <ul>
 *   <li>流量控制规则初始化</li>
 *   <li>降级规则初始化</li>
 *   <li>系统规则初始化</li>
 * </ul>
 *
 * <p>注意：规则的动态更新需要配合Nacos配置中心使用。
 *
 * @author ashclaw
 * @since JDK 25
 * @see SentinelRulesProperties
 * @see FlowRule
 * @see DegradeRule
 * @see SystemRule
 */
@Slf4j
@Configuration
public class SentinelRulesConfig {

    private final SentinelRulesProperties sentinelRulesProperties;

    /**
     * 构造函数
     *
     * @param sentinelRulesProperties Sentinel规则配置属性
     */
    public SentinelRulesConfig(SentinelRulesProperties sentinelRulesProperties) {
        this.sentinelRulesProperties = sentinelRulesProperties;
    }

    /**
     * 初始化Sentinel规则
     *
     * <p>在@PostConstruct阶段被Spring调用，依次初始化流量控制规则、降级规则和系统规则。
     */
    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
        initSystemRules();
        log.info("Sentinel限流规则初始化完成");
    }

    /**
     * 初始化流量控制规则
     *
     * <p>从配置属性中读取流量控制规则并注册到Sentinel。
     * 支持QPS限流和并发线程数限流两种模式。
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        for (SentinelRulesProperties.FlowRule rule : sentinelRulesProperties.getFlow()) {
            FlowRule flowRule = new FlowRule();
            flowRule.setResource(rule.getResource());
            flowRule.setCount(rule.getCount());
            flowRule.setGrade(rule.getGrade());
            flowRule.setStrategy(rule.getStrategy());
            flowRule.setControlBehavior(rule.getControlBehavior());
            flowRule.setWarmUpPeriodSec(rule.getWarmUpPeriodSec());
            flowRule.setMaxQueueingTimeMs(rule.getMaxQueueingTimeMs());
            rules.add(flowRule);
        }

        if (!rules.isEmpty()) {
            FlowRuleManager.loadRules(rules);
            log.info("流量控制规则加载完成，规则数：{}", rules.size());
        }
    }

    /**
     * 初始化降级规则
     *
     * <p>从配置属性中读取降级规则并注册到Sentinel。
     * 支持慢调用比例、异常比例和异常数三种降级策略。
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        for (SentinelRulesProperties.DegradeRule rule : sentinelRulesProperties.getDegrade()) {
            DegradeRule degradeRule = new DegradeRule();
            degradeRule.setResource(rule.getResource());
            degradeRule.setCount(rule.getCount());
            degradeRule.setTimeWindow(rule.getTimeWindow());
            degradeRule.setGrade(rule.getGrade());
            degradeRule.setSlowRatioThreshold(rule.getSlowRatioThreshold());
            degradeRule.setMinRequestAmount(rule.getMinRequestAmount());
            degradeRule.setStatIntervalMs(rule.getStatIntervalMs());
            rules.add(degradeRule);
        }

        if (!rules.isEmpty()) {
            DegradeRuleManager.loadRules(rules);
            log.info("降级规则加载完成，规则数：{}", rules.size());
        }
    }

    /**
     * 初始化系统规则
     *
     * <p>从配置属性中读取系统规则并注册到Sentinel。
     * 系统规则会在系统负载过高时触发限流，保护系统稳定性。
     */
    private void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();

        for (SentinelRulesProperties.SystemRule rule : sentinelRulesProperties.getSystem()) {
            SystemRule systemRule = new SystemRule();
            systemRule.setQps(rule.getQps());
            systemRule.setMaxThread(rule.getMaxThread());
            systemRule.setAvgRt((long) rule.getAvgRt());
            systemRule.setHighestSystemLoad(rule.getHighestSystemLoad());
            rules.add(systemRule);
        }

        if (!rules.isEmpty()) {
            SystemRuleManager.loadRules(rules);
            log.info("系统规则加载完成，规则数：{}", rules.size());
        }
    }
}