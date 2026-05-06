package cn.ko_ai_code.com.koaicode.langgraph4j.node;

import cn.ko_ai_code.com.koaicode.ai.AiCodeGenTypeRoutingService;
import cn.ko_ai_code.com.koaicode.ai.AiCodeGenTypeRoutingServiceFactory;
import cn.ko_ai_code.com.koaicode.langgraph4j.state.WorkflowContext;
import cn.ko_ai_code.com.koaicode.model.enums.CodeGenTypeEnum;
import cn.ko_ai_code.com.koaicode.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType = context.getGenerationType();
            String codeGenTypeSource = context.getCodeGenTypeSource();

            // 优先使用 App 已配置的 codeGenType，避免重复 AI 路由
            if (generationType != null && "app".equals(codeGenTypeSource)) {
                log.info("codeGenType resolved: {} (source: app, skip AI routing)", generationType.getValue());
                context.setCurrentStep("智能路由");
                return WorkflowContext.saveContext(context);
            }

            // 兜底：通过 AI 动态路由确定代码生成类型
            try {
                AiCodeGenTypeRoutingServiceFactory factory = SpringContextUtil.getBean(AiCodeGenTypeRoutingServiceFactory.class);
                AiCodeGenTypeRoutingService routingService = factory.createAiCodeGenTypeRoutingService();
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                context.setCodeGenTypeSource("ai_route");
                log.info("codeGenType resolved: {} (source: ai_route)", generationType.getValue());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
                context.setCodeGenTypeSource("fallback");
            }

            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}

