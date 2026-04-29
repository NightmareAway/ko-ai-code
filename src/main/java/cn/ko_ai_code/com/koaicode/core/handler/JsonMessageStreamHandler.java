package cn.ko_ai_code.com.koaicode.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.ko_ai_code.com.koaicode.ai.model.message.*;
import cn.ko_ai_code.com.koaicode.ai.tools.BaseTool;
import cn.ko_ai_code.com.koaicode.ai.tools.ToolManager;
import cn.ko_ai_code.com.koaicode.constant.AppConstant;
import cn.ko_ai_code.com.koaicode.core.builder.VueProjectBuilder;
import cn.ko_ai_code.com.koaicode.entity.User;
import cn.ko_ai_code.com.koaicode.exception.BusinessException;
import cn.ko_ai_code.com.koaicode.exception.ErrorCode;
import cn.ko_ai_code.com.koaicode.model.enums.ChatHistoryMessageTypeEnum;
import cn.ko_ai_code.com.koaicode.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ToolManager toolManager;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 收集工具返回内容

        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 异步构造 Vue 项目，传递 appId 用于构建状态推送
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath, appId);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // Vue 模式下模型可能返回纯文本片段，不能强制按 JSON 解析
        if (StrUtil.isBlank(chunk)) {
            return "";
        }
        String normalized = chunk.trim();
        if (!normalized.startsWith("{")) {
            chatHistoryStringBuilder.append(chunk);
            return chunk;
        }

        StreamMessage streamMessage;
        try {
            streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        } catch (Exception e) {
            log.warn("收到非标准 JSON 流块，按普通文本处理: {}", e.getMessage());
            chatHistoryStringBuilder.append(chunk);
            return chunk;
        }
        if (streamMessage == null || StrUtil.isBlank(streamMessage.getType())) {
            String data = extractDataFromUnknownJson(chunk);
            chatHistoryStringBuilder.append(data);
            return data;
        }
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            String data = extractDataFromUnknownJson(chunk);
            chatHistoryStringBuilder.append(data);
            return data;
        }
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并返回工具信息
                    seenToolIds.add(toolId);
                    // 根据工具名称获取工具实例
                    BaseTool tool = toolManager.getTool(toolName);
                    // 返回格式化的工具调用信息
                    return tool.generateToolRequestResponse();
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                JSONObject jsonObject;
                try {
                    jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                } catch (Exception e) {
                    log.warn("工具执行参数不是合法 JSON，忽略该工具结果: {}", e.getMessage());
                    return "";
                }
                // 根据工具名称获取工具实例并生成相应的结果格式
                BaseTool tool = toolManager.getTool(toolName);
                String result = tool.generateToolExecutedResult(jsonObject);
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }

        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "ai没有生成对应的枚举类");
    }

    private String extractDataFromUnknownJson(String chunk) {
        try {
            JSONObject obj = JSONUtil.parseObj(chunk);
            String data = obj.getStr("data");
            if (StrUtil.isNotBlank(data)) {
                return data;
            }
            String d = obj.getStr("d");
            if (StrUtil.isNotBlank(d)) {
                return d;
            }
        } catch (Exception e) {
            log.warn("未知 JSON 流块提取 data 失败: {}", e.getMessage());
        }
        return chunk;
    }
}