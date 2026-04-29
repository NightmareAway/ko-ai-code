package cn.ko_ai_code.com.koaicode.service.impl;

import cn.ko_ai_code.com.koaicode.model.dto.build.BuildStatusEvent;
import cn.ko_ai_code.com.koaicode.service.BuildStatusSseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 构建状态SSE推送服务实现
 * 使用 Sinks.Many 实现多播模式，支持多客户端订阅同一应用的构建状态
 */
@Slf4j
@Service
public class BuildStatusSseServiceImpl implements BuildStatusSseService {

    /**
     * 存储 appId -> Sink 的映射
     * 使用 multicast 模式支持多订阅者
     */
    private final Map<Long, Sinks.Many<BuildStatusEvent>> sinkMap = new ConcurrentHashMap<>();

    @Override
    public Flux<BuildStatusEvent> subscribeBuildStatus(Long appId) {
        Sinks.Many<BuildStatusEvent> sink = getOrCreateSink(appId);
        
        // 返回 Flux，并设置超时自动清理
        return sink.asFlux()
                .timeout(Duration.ofMinutes(30), Flux.empty()) // 30分钟超时
                .doOnSubscribe(subscription -> {
                    log.info("客户端订阅构建状态，appId: {}", appId);
                })
                .doOnCancel(() -> {
                    log.info("客户端取消订阅构建状态，appId: {}", appId);
                })
                .doOnError(error -> {
                    log.error("构建状态订阅发生错误，appId: {}, error: {}", appId, error.getMessage());
                });
    }

    @Override
    public void pushBuildStatus(BuildStatusEvent event) {
        Long appId = event.getAppId();
        Sinks.Many<BuildStatusEvent> sink = getOrCreateSink(appId);
        
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.warn("推送构建状态失败，appId: {}, status: {}, result: {}", 
                    appId, event.getStatus(), result);
        } else {
            log.info("推送构建状态成功，appId: {}, status: {}, message: {}", 
                    appId, event.getStatus(), event.getMessage());
        }
    }

    @Override
    public Sinks.Many<BuildStatusEvent> getSink(Long appId) {
        return getOrCreateSink(appId);
    }

    @Override
    public void completeBuildStatus(Long appId) {
        if (appId == null) {
            log.warn("completeBuildStatus 收到 null appId，忽略");
            return;
        }
        Sinks.Many<BuildStatusEvent> sink = sinkMap.remove(appId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("完成构建状态流，appId: {}", appId);
        }
    }

    /**
     * 获取或创建 Sink
     * 使用 multicast 模式支持多订阅者
     *
     * @param appId 应用ID
     * @return Sink对象
     */
    private Sinks.Many<BuildStatusEvent> getOrCreateSink(Long appId) {
        return sinkMap.computeIfAbsent(appId, id -> {
            log.info("创建新的构建状态Sink，appId: {}", id);
            return Sinks.many().multicast().onBackpressureBuffer();
        });
    }

    /**
     * 清理过期的 Sink（可由定时任务调用）
     */
    public void cleanupExpiredSinks() {
        // 移除已完成的 Sink
        sinkMap.entrySet().removeIf(entry -> {
            Sinks.Many<BuildStatusEvent> sink = entry.getValue();
            // 如果 Sink 已经终止，则移除
            if (sink.currentSubscriberCount() == 0) {
                sink.tryEmitComplete();
                return true;
            }
            return false;
        });
    }
}
