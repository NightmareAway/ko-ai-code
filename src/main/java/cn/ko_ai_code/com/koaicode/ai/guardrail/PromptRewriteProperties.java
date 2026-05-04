package cn.ko_ai_code.com.koaicode.ai.guardrail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Prompt 重写护轨的配置属性，支持通过 application.yml 动态调整重写策略。
 *
 * <pre>
 * app:
 *   guardrail:
 *     rewrite:
 *       enabled: true
 *       redact-pii: true
 *       optimize-quality: true
 *       audit-enabled: true
 *       audit-ttl-seconds: 86400
 *       max-rewrite-time-ms: 50
 *       injection-defense-mode: REWRITE
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.guardrail.rewrite")
public class PromptRewriteProperties {

    /**
     * 是否启用 Prompt 自动重写功能，默认 true
     */
    private boolean enabled = true;

    /**
     * 是否启用敏感个人信息脱敏（身份证、手机号、邮箱等），默认 true
     */
    private boolean redactPii = true;

    /**
     * 是否启用 Prompt 质量优化（修正模糊表述、补充上下文约束），默认 true
     */
    private boolean optimizeQuality = true;

    /**
     * 是否启用审计记录（将重写记录写入 Redis），默认 true
     */
    private boolean auditEnabled = true;

    /**
     * 审计记录在 Redis 中的 TTL（秒），默认 86400（24 小时）
     */
    private long auditTtlSeconds = 86400;

    /**
     * 单次重写最大耗时（毫秒），超时则跳过剩余非关键规则，默认 50ms
     */
    private int maxRewriteTimeMs = 50;

    /**
     * 注入攻击防御模式：REWRITE（尝试改写后放行）、REJECT（直接拒绝）。
     * 默认 REWRITE，与 PromptSafetyInputGuardrail 的严格拒绝策略形成互补。
     */
    private InjectionDefenseMode injectionDefenseMode = InjectionDefenseMode.REWRITE;

    /**
     * Prompt 最大允许长度（字符数），超过此长度先截断再处理，默认 2000
     */
    private int maxPromptLength = 2000;

    public enum InjectionDefenseMode {
        REWRITE,
        REJECT
    }
}
