package cn.ko_ai_code.com.koaicode.model.dto.build;

import cn.ko_ai_code.com.koaicode.model.enums.BuildStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 构建状态事件
 * 用于SSE推送构建状态变更
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildStatusEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 构建状态
     */
    private BuildStatusEnum status;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;

    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建构建状态事件
     *
     * @param appId    应用ID
     * @param status   构建状态
     * @param message  消息
     * @param progress 进度
     * @return 构建状态事件
     */
    public static BuildStatusEvent of(Long appId, BuildStatusEnum status, String message, Integer progress) {
        return BuildStatusEvent.builder()
                .appId(appId)
                .status(status)
                .message(message)
                .progress(progress)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
