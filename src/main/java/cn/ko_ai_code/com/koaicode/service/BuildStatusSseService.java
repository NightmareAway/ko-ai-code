package cn.ko_ai_code.com.koaicode.service;

import cn.ko_ai_code.com.koaicode.model.dto.build.BuildStatusEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 构建状态SSE推送服务
 * 提供构建状态的实时推送能力
 */
public interface BuildStatusSseService {

    /**
     * 订阅指定应用的构建状态变更事件
     *
     * @param appId 应用ID
     * @return 构建状态事件流
     */
    Flux<BuildStatusEvent> subscribeBuildStatus(Long appId);

    /**
     * 推送构建状态变更事件
     *
     * @param event 构建状态事件
     */
    void pushBuildStatus(BuildStatusEvent event);

    /**
     * 获取指定应用的Sink，用于推送事件
     *
     * @param appId 应用ID
     * @return Sink对象
     */
    Sinks.Many<BuildStatusEvent> getSink(Long appId);

    /**
     * 完成指定应用的构建状态流（关闭连接）
     *
     * @param appId 应用ID
     */
    void completeBuildStatus(Long appId);
}
