package cn.ko_ai_code.com.koaicode.ai.guardrail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感信息脱敏重写规则，识别并替换 Prompt 中的个人隐私数据。
 * <p>
 * 覆盖的敏感信息类型包括：中国大陆身份证号、手机号、电子邮箱、
 * 银行卡号、API 密钥/Token、IPv4 地址。脱敏后的占位符采用中文标记，
 * 便于下游调用方识别被修改的位置。
 * <p>
 * 优先级设为 0，确保脱敏操作在其他改写规则之前执行。
 */
public class SensitiveInfoRewriteRule implements PromptRewriteRule {

    /**
     * 敏感信息匹配模式与对应的脱敏占位符，按匹配顺序排列。
     * 使用 LinkedHashMap 保证遍历顺序稳定。
     */
    private static final Map<Pattern, String> PATTERNS = new LinkedHashMap<>();

    static {
        // IPv4 地址（私网地址也一并脱敏，避免泄露内网拓扑）
        PATTERNS.put(
                Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"),
                "[IP地址已脱敏]"
        );
    }

    @Override
    public String name() {
        return "SensitiveInfoRewriteRule";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean matches(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return false;
        }
        for (Pattern pattern : PATTERNS.keySet()) {
            if (pattern.matcher(prompt).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String rewrite(String prompt, List<PromptRewriteResult.ModificationRecord> modifications) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }
        String result = prompt;
        for (Map.Entry<Pattern, String> entry : PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(result);
            if (matcher.find()) {
                // 收集所有匹配项后再替换，避免索引偏移
                List<int[]> matches = new ArrayList<>();
                matcher.reset();
                while (matcher.find()) {
                    matches.add(new int[]{matcher.start(), matcher.end()});
                }
                // 从后往前替换，保证索引不变
                String replacement = entry.getValue();
                for (int i = matches.size() - 1; i >= 0; i--) {
                    int[] span = matches.get(i);
                    String originalText = result.substring(span[0], span[1]);
                    result = result.substring(0, span[0]) + replacement + result.substring(span[1]);
                    modifications.add(PromptRewriteResult.ModificationRecord.builder()
                            .type("REDACT")
                            .originalSegment(originalText)
                            .rewrittenSegment(replacement)
                            .reason("检测到敏感信息（" + typeLabel(replacement) + "），已自动脱敏处理")
                            .build());
                }
            }
        }
        return result;
    }

    private static String typeLabel(String placeholder) {
        if (placeholder.contains("身份证")) return "身份证号";
        if (placeholder.contains("手机")) return "手机号";
        if (placeholder.contains("邮箱")) return "电子邮箱";
        if (placeholder.contains("密钥") || placeholder.contains("Token")) return "密钥/令牌";
        if (placeholder.contains("银行卡")) return "银行卡号";
        if (placeholder.contains("IP")) return "IP地址";
        return "敏感信息";
    }
}
