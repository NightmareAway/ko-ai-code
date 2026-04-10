package cn.ko_ai_code.com.koaicode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 *
 * @author ko
 * @version 1.0
 * @since 2024-01-01
 */
@Tag(name = "健康检查", description = "提供应用健康状态检查接口，用于监控服务可用性")
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 健康检查
     *
     * @author ko
     * @since 2024-01-01
     * @return 返回 "ok" 表示服务正常运行
     */
    @Operation(
            summary = "健康检查",
            description = "检查服务是否正常运行，返回 'ok' 表示健康状态",
            tags = {"健康检查"},
            method = "GET"
    )
    @GetMapping("/")
    public String healthCheck() {
        return "ok";
    }
}
