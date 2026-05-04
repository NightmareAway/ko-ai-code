package cn.ko_ai_code.com.koaicode.ai.guardrail;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Prompt 重写结果封装，记录原始 Prompt、重写后的 Prompt 以及所有修改明细。
 * <p>
 * 调用方可通过此对象了解哪些内容被修改及原因，原始 Prompt 保留用于审计追溯。
 */
@Data
@Builder
public class PromptRewriteResult implements Serializable {

    /**
     * 用户提交的原始 Prompt
     */
    private String originalPrompt;

    /**
     * 经过重写处理后的安全 Prompt
     */
    private String rewrittenPrompt;

    /**
     * 是否发生了任何修改
     */
    private boolean modified;

    /**
     * 处理时间戳（毫秒）
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /**
     * 修改记录列表，每条记录对应一处变更
     */
    @Builder.Default
    private List<ModificationRecord> modifications = new ArrayList<>();

    /**
     * 单条修改记录，说明哪段原始文本被替换为什么、以及替换原因。
     */
    @Data
    @Builder
    public static class ModificationRecord implements Serializable {

        /**
         * 修改类型标识：REDACT（脱敏）、REFINE（优化表达）、RESTRUCTURE（结构调整）、REJECT（拒绝改写失败）
         */
        private String type;

        /**
         * 被修改的原始文本片段
         */
        private String originalSegment;

        /**
         * 修改后的文本片段
         */
        private String rewrittenSegment;

        /**
         * 修改原因说明，便于调用方理解变更意图
         */
        private String reason;
    }
}
