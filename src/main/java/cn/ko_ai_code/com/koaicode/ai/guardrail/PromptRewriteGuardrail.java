package cn.ko_ai_code.com.koaicode.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 基于护轨（Guardrail）机制的 Prompt 自动重写输入护轨。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li><b>风险检测与自动重写</b>：检测注入攻击、越狱尝试、敏感信息等风险内容，
 *       不直接拒绝，而是移除或脱敏后放行。</li>
 *   <li><b>Prompt 质量优化</b>：修正模糊表述、补充上下文约束、调整不合理要求，
 *       将超出模型能力范围的请求转化为可合理完成的任务。</li>
 *   <li><b>透明性审计</b>：重写后的 Prompt 附带修改说明，原始 Prompt 保留备查。</li>
 * </ul>
 *
 * <h3>与 PromptSafetyInputGuardrail 的关系</h3>
 * <p>本类与 {@link PromptSafetyInputGuardrail} 协同工作：</p>
 * <ul>
 *   <li>PromptRewriteGuardrail 负责"改写后放行"——优先通过重写使 Prompt 合规</li>
 *   <li>PromptSafetyInputGuardrail 负责"检测后拒绝"——对无法改写的恶意输入硬阻断</li>
 * </ul>
 * <p>推荐将本护轨放在安全护轨之前执行，形成"先改写、后校验"的双层防护。</p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * // 方式一：Spring 托管（推荐）
 * &#64;Resource
 * private PromptRewriteGuardrail rewriteGuardrail;
 *
 * // 方式二：独立实例化（使用默认规则）
 * InputGuardrail guardrail = PromptRewriteGuardrail.withDefaults();
 * InputGuardrailResult result = guardrail.validate(userMessage);
 * if (result.isSuccess()) {
 *     String safePrompt = PromptRewriteGuardrail.getLastResult().getRewrittenPrompt();
 * }
 * </pre>
 *
 * <h3>性能考虑</h3>
 * <p>单次重写耗时控制在 {@link PromptRewriteProperties#getMaxRewriteTimeMs()} 以内，
 * 超时后跳过剩余非关键规则。默认上限 50ms，对请求延迟影响可控。</p>
 */
@Slf4j
public class PromptRewriteGuardrail implements InputGuardrail {

    // ==================== 注入攻击检测模式（与 PromptSafetyInputGuardrail 保持一致） ====================

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:"),
            Pattern.compile("(?i)(?:jailbreak|越狱|破解|crack)\\s+(?:prompt|指令|模型)"),
            Pattern.compile("(?i)(?:ignore|disregard|override|覆盖)\\s+(?:safety|安全)\\s+(?:rules?|guidelines?|限制)")
    );

    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "破解", "hack", "绕过", "bypass", "越狱", "jailbreak",
            "忽略之前的指令", "ignore previous instructions", "ignore above"
    );

    // ==================== ThreadLocal 结果传递 ====================

    /**
     * 线程级别的最后一次重写结果，供调用方在 validate() 返回后获取改写后的 Prompt。
     * 每次 validate() 调用开始时自动清理旧值。
     */
    private static final ThreadLocal<PromptRewriteResult> LAST_RESULT = new ThreadLocal<>();

    // ==================== 实例字段 ====================

    private final List<PromptRewriteRule> rules;
    private final PromptRewriteProperties properties;
    private final StringRedisTemplate redisTemplate;

    // ==================== 构造器 ====================

    /**
     * Spring 托管构造器：注入规则列表、配置属性和 Redis 模板（可选）。
     * <p>
     * 规则按 {@link PromptRewriteRule#priority()} 升序排列后执行。
     *
     * @param rules         可插拔的重写规则列表（可为空，使用默认规则集）
     * @param properties    重写配置属性
     * @param redisTemplate Redis 客户端（可为 null，此时审计日志仅输出到 SLF4J）
     */
    public PromptRewriteGuardrail(List<PromptRewriteRule> rules,
                                  PromptRewriteProperties properties,
                                  StringRedisTemplate redisTemplate) {
        this.properties = properties != null ? properties : new PromptRewriteProperties();
        this.redisTemplate = redisTemplate;
        List<PromptRewriteRule> ruleList = new ArrayList<>(rules != null ? rules : List.of());
        if (this.properties.isRedactPii() && ruleList.stream().noneMatch(r -> r instanceof SensitiveInfoRewriteRule)) {
            ruleList.add(new SensitiveInfoRewriteRule());
        }
        ruleList.sort(Comparator.comparingInt(PromptRewriteRule::priority));
        this.rules = List.copyOf(ruleList);
    }

    /**
     * 使用默认规则集创建实例（适用于非 Spring 场景）。
     * 默认包含 {@link SensitiveInfoRewriteRule} 脱敏规则。
     */
    public static PromptRewriteGuardrail withDefaults() {
        return new PromptRewriteGuardrail(List.of(new SensitiveInfoRewriteRule()), new PromptRewriteProperties(), null);
    }

    /**
     * 获取当前线程最后一次重写的结果。
     *
     * @return 重写结果，若未发生重写则返回仅含原始 Prompt 的结果对象（modified=false）
     */
    public static PromptRewriteResult getLastResult() {
        PromptRewriteResult result = LAST_RESULT.get();
        return result;
    }

    // ==================== InputGuardrail 接口实现 ====================

    /**
     * 对用户输入执行安全检测与自动重写。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>清理上一次的 ThreadLocal 结果</li>
     *   <li>空输入 / 超长输入检查</li>
     *   <li>注入攻击模式匹配 → 尝试改写或拒绝</li>
     *   <li>敏感词检测 → 改写脱敏</li>
     *   <li>执行所有已注册的 {@link PromptRewriteRule} 规则</li>
     *   <li>Prompt 质量优化（可选）</li>
     *   <li>结果存入 ThreadLocal 并记录审计日志</li>
     * </ol>
     *
     * @param userMessage 用户输入消息
     * @return success（改写后放行）或 fatal（无法改写，拒绝请求）
     */
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        LAST_RESULT.remove();
        long startTime = System.currentTimeMillis();

        String original = userMessage.singleText();
        if (original == null) {
            original = "";
        }

        // 空输入检查
        if (original.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }

        // 超长截断
        String working = original;
        boolean truncated = false;
        if (working.length() > properties.getMaxPromptLength()) {
            working = working.substring(0, properties.getMaxPromptLength());
            truncated = true;
            log.warn("Prompt 长度 {} 超过上限 {}，已截断", original.length(), properties.getMaxPromptLength());
        }

        if (!properties.isEnabled()) {
            return storeAndReturn(original, original, false, List.of());
        }

        List<PromptRewriteResult.ModificationRecord> modifications = new ArrayList<>();
        long deadline = startTime + properties.getMaxRewriteTimeMs();

        // ---- 阶段一：注入攻击检测 ----
        InjectionCheckResult injectionResult = checkInjection(working);
        if (injectionResult.detected) {
            if (properties.getInjectionDefenseMode() == PromptRewriteProperties.InjectionDefenseMode.REJECT) {
                modifications.add(PromptRewriteResult.ModificationRecord.builder()
                        .type("REJECT")
                        .originalSegment(injectionResult.matchedText)
                        .rewrittenSegment("")
                        .reason("检测到注入攻击模式 [" + injectionResult.matchedText + "]，已配置为直接拒绝")
                        .build());
                storeResult(original, working, modifications);
                return fatal("检测到恶意输入，请求被拒绝");
            }
            // REWRITE 模式：移除注入指令，保留用户的实质问题
            working = injectionResult.cleanedPrompt;
            modifications.add(PromptRewriteResult.ModificationRecord.builder()
                    .type("REFINE")
                    .originalSegment(injectionResult.matchedText)
                    .rewrittenSegment("[已移除注入指令]")
                    .reason("检测到注入攻击模式，已移除恶意指令并保留有效提问内容")
                    .build());
        }

        // ---- 阶段二：敏感词改写 ----
        working = rewriteSensitiveKeywords(working, modifications);

        // ---- 阶段三：执行可插拔规则 ----
        for (PromptRewriteRule rule : rules) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("Prompt 重写超时（{}ms），跳过剩余规则", properties.getMaxRewriteTimeMs());
                break;
            }
            if (rule.matches(working)) {
                String before = working;
                working = rule.rewrite(working, modifications);
                if (!before.equals(working)) {
                    log.debug("规则 [{}] 已应用，Prompt 长度: {} -> {}", rule.name(), before.length(), working.length());
                }
            }
        }

        // ---- 阶段四：质量优化 ----
        if (properties.isOptimizeQuality() && System.currentTimeMillis() <= deadline) {
            working = optimizeQuality(working, modifications);
        }

        // ---- 后处理 ----
        if (truncated) {
            modifications.add(PromptRewriteResult.ModificationRecord.builder()
                    .type("REFINE")
                    .originalSegment("(原始 Prompt 尾部)")
                    .rewrittenSegment("(已截断)")
                    .reason("Prompt 超过长度限制 " + properties.getMaxPromptLength() + " 字符，超长部分已移除")
                    .build());
        }

        boolean modified = !working.equals(original);
        if (modified && properties.isAuditEnabled()) {
            writeAuditLog(original, working, modifications);
        }

        log.info("Prompt 重写完成，原始长度={}，重写后长度={}，修改数={}，耗时={}ms",
                original.length(), working.length(), modifications.size(),
                System.currentTimeMillis() - startTime);

        return storeAndReturn(original, working, modified, modifications);
    }

    // ==================== 注入检测 ====================

    private InjectionCheckResult checkInjection(String prompt) {
        for (Pattern pattern : INJECTION_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(prompt);
            if (matcher.find()) {
                String matched = matcher.group();
                // 尝试移除注入指令部分，保留用户实质提问
                String cleaned = prompt.replaceAll(pattern.pattern(), "").replaceAll("\\s{2,}", " ").trim();
                if (cleaned.isEmpty()) {
                    cleaned = "[用户输入仅包含注入指令，已完全移除]";
                }
                return new InjectionCheckResult(true, matched, cleaned);
            }
        }
        return new InjectionCheckResult(false, null, prompt);
    }

    private static class InjectionCheckResult {
        final boolean detected;
        final String matchedText;
        final String cleanedPrompt;

        InjectionCheckResult(boolean detected, String matchedText, String cleanedPrompt) {
            this.detected = detected;
            this.matchedText = matchedText;
            this.cleanedPrompt = cleanedPrompt;
        }
    }

    // ==================== 敏感词处理 ====================

    private String rewriteSensitiveKeywords(String prompt,
                                            List<PromptRewriteResult.ModificationRecord> modifications) {
        String result = prompt;
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (result.toLowerCase().contains(keyword.toLowerCase())) {
                result = result.replaceAll("(?i)" + Pattern.quote(keyword), "[已移除敏感词]");
                modifications.add(PromptRewriteResult.ModificationRecord.builder()
                        .type("REDACT")
                        .originalSegment(keyword)
                        .rewrittenSegment("[已移除敏感词]")
                        .reason("检测到敏感关键词，已自动移除")
                        .build());
            }
        }
        return result;
    }

    // ==================== 质量优化 ====================

    /**
     * 对 Prompt 进行轻量质量优化，不引入外部模型调用以保证性能。
     * <p>优化项包括：</p>
     * <ul>
     *   <li>修正过度模糊的表述，引导用户提供必要上下文</li>
     *   <li>标注超出大模型能力范围的不合理请求</li>
     *   <li>规范化空白字符</li>
     * </ul>
     */
    private String optimizeQuality(String prompt,
                                   List<PromptRewriteResult.ModificationRecord> modifications) {
        String result = prompt;

        // 检测"预测未来"类不合理请求
        Pattern futurePrediction = Pattern.compile("(预测|告诉我|判断).{0,10}(未来|将来|明天|下周|明年|走势|涨跌)");
        java.util.regex.Matcher futureMatcher = futurePrediction.matcher(result);
        if (futureMatcher.find()) {
            String original = futureMatcher.group();
            result = futureMatcher.replaceFirst("基于现有数据和趋势，分析" + futureMatcher.group(2) + "的可能情况");
            modifications.add(PromptRewriteResult.ModificationRecord.builder()
                    .type("REFINE")
                    .originalSegment(original)
                    .rewrittenSegment("基于现有数据和趋势，分析..." + futureMatcher.group(2) + "的可能情况")
                    .reason("原请求涉及对未来事件的确定性预测，已调整为基于现有信息的趋势分析任务")
                    .build());
        }

        // 检测"执行实时操作"类不合理请求
        Pattern realtimeAction = Pattern.compile("(帮我|替我|马上|立刻|现在就).{0,5}(购买|下单|转账|支付|发邮件|打电话)");
        java.util.regex.Matcher actionMatcher = realtimeAction.matcher(result);
        if (actionMatcher.find()) {
            String original = actionMatcher.group();
            String action = actionMatcher.group(2);
            result = actionMatcher.replaceFirst("请提供关于如何" + action + "的操作指导或代码示例");
            modifications.add(PromptRewriteResult.ModificationRecord.builder()
                    .type("REFINE")
                    .originalSegment(original)
                    .rewrittenSegment("请提供关于如何" + action + "的操作指导或代码示例")
                    .reason("原请求要求模型执行实时操作（" + action + "），已调整为提供操作指导")
                    .build());
        }

        // 规范化空白字符
        String normalized = result.replaceAll("[ \\t]{2,}", " ").replaceAll("\\n{3,}", "\n\n").trim();
        if (!normalized.equals(result)) {
            modifications.add(PromptRewriteResult.ModificationRecord.builder()
                    .type("REFINE")
                    .originalSegment("(多余空白字符)")
                    .rewrittenSegment("(已规范化)")
                    .reason("移除多余空白字符，提升 Prompt 可读性")
                    .build());
            result = normalized;
        }

        return result;
    }

    // ==================== 结果存储与审计 ====================

    private InputGuardrailResult storeAndReturn(String original, String rewritten,
                                                 boolean modified,
                                                 List<PromptRewriteResult.ModificationRecord> modifications) {
        PromptRewriteResult result = PromptRewriteResult.builder()
                .originalPrompt(original)
                .rewrittenPrompt(rewritten)
                .modified(modified)
                .modifications(modifications)
                .timestamp(System.currentTimeMillis())
                .build();
        LAST_RESULT.set(result);
        return success();
    }

    private void storeResult(String original, String rewritten,
                             List<PromptRewriteResult.ModificationRecord> modifications) {
        LAST_RESULT.set(PromptRewriteResult.builder()
                .originalPrompt(original)
                .rewrittenPrompt(rewritten)
                .modified(!original.equals(rewritten))
                .modifications(modifications)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * 将重写审计记录写入 Redis，使用 StringRedisTemplate 的 TTL 机制。
     * 若 Redis 不可用则降级为日志记录，不影响主流程。
     */
    private void writeAuditLog(String original, String rewritten,
                               List<PromptRewriteResult.ModificationRecord> modifications) {
        if (redisTemplate == null) {
            log.debug("Redis 不可用，审计记录仅输出到日志。修改数={}", modifications.size());
            return;
        }
        try {
            String auditKey = "guardrail:rewrite:audit:" + System.currentTimeMillis() + ":" + Thread.currentThread().getId();
            String auditValue = String.format("original=%s|rewritten=%s|modifications=%d",
                    original.length() > 200 ? original.substring(0, 200) + "..." : original,
                    rewritten.length() > 200 ? rewritten.substring(0, 200) + "..." : rewritten,
                    modifications.size());
            redisTemplate.opsForValue().set(auditKey, auditValue,
                    properties.getAuditTtlSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("审计记录写入 Redis 失败，已降级为日志", e);
        }
    }
}
