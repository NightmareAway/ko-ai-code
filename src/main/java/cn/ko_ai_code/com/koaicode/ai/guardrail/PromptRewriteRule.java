package cn.ko_ai_code.com.koaicode.ai.guardrail;

import java.util.List;

/**
 * Prompt 重写规则接口，定义单一职责的改写策略。
 * <p>
 * 每条规则负责检测特定类型的风险或质量问题，并对 Prompt 进行针对性改写。
 * 通过 {@link #priority()} 控制规则执行顺序，数值越小越先执行。
 * 规则应设计为无状态，确保并发安全。
 */
public interface PromptRewriteRule {

    /**
     * 规则名称，用于日志和审计追溯
     */
    String name();

    /**
     * 判断当前规则是否需要对输入 Prompt 执行改写
     *
     * @param prompt 用户原始输入
     * @return true 表示需要应用此规则
     */
    boolean matches(String prompt);

    /**
     * 对 Prompt 执行改写，并将变更记录追加到 modifications 列表。
     * <p>
     * 即使未匹配到任何内容，也应原样返回 prompt。
     *
     * @param prompt        待改写的 Prompt 文本
     * @param modifications 修改记录收集列表，规则将变更明细追加到此列表
     * @return 改写后的 Prompt 文本
     */
    String rewrite(String prompt, List<PromptRewriteResult.ModificationRecord> modifications);

    /**
     * 规则优先级，数值越小越先执行，默认 100。
     * <p>
     * 脱敏类规则建议 0~49，质量优化类规则建议 50~99，结构调整类规则建议 100+。
     */
    default int priority() {
        return 100;
    }
}
