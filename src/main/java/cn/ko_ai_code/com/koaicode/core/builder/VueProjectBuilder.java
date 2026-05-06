package cn.ko_ai_code.com.koaicode.core.builder;

import cn.hutool.core.io.IoUtil;
import cn.ko_ai_code.com.koaicode.model.dto.build.BuildStatusEvent;
import cn.ko_ai_code.com.koaicode.model.enums.BuildStatusEnum;
import cn.ko_ai_code.com.koaicode.service.BuildStatusSseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@Slf4j
@Component
public class VueProjectBuilder {

    @Resource
    private BuildStatusSseService buildStatusSseService;

    /**
     * 推送构建状态到 BuildStatusSseService（独立 SSE 订阅通道）
     */
    private void pushStatus(Long appId, BuildStatusEnum status, String message, Integer progress) {
        if (buildStatusSseService != null && appId != null) {
            BuildStatusEvent event = BuildStatusEvent.of(appId, status, message, progress);
            buildStatusSseService.pushBuildStatus(event);
        }
    }

    /**
     * 执行命令，捕获 stdout/stderr 输出并记录到日志
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (InputStream is = process.getInputStream()) {
                    String text = IoUtil.read(is, Charset.defaultCharset());
                    output.append(text);
                } catch (Exception e) {
                    log.warn("读取进程输出时异常: {}", e.getMessage());
                }
            }, "cmd-reader-" + System.currentTimeMillis());
            readerThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程。已捕获的输出:\n{}", timeoutSeconds, output);
                process.destroyForcibly();
                readerThread.interrupt();
                return false;
            }
            readerThread.join(TimeUnit.SECONDS.toMillis(10));

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                log.debug("命令输出:\n{}", output);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}。完整输出:\n{}", exitCode, output);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 构建 Vue 项目（核心方法，通过 Consumer 回调推送状态）。
     * LangGraph4j 工作流和传统模式均可使用此方法。
     *
     * @param projectPath    项目根目录路径
     * @param statusConsumer 构建状态回调，为 null 时不推送
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath, Consumer<BuildStatusEvent> statusConsumer) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.FAILED, "项目目录不存在", 0));
            return false;
        }
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.FAILED, "package.json 文件不存在", 0));
            return false;
        }

        log.info("开始构建 Vue 项目: {}", projectPath);
        acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.PENDING, "准备开始构建...", 10));

        // npm install
        log.info("执行 npm install...");
        acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.INSTALLING, "正在安装依赖包...", 20));
        String installCmd = String.format("%s install", buildCommand("npm"));
        if (!executeCommand(projectDir, installCmd, 300)) {
            acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.FAILED, "依赖安装失败", 20));
            return false;
        }
        acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.INSTALLING, "依赖安装完成", 50));

        // npm run build
        log.info("执行 npm run build...");
        acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.BUILDING, "正在构建项目...", 60));
        String buildCmd = String.format("%s run build", buildCommand("npm"));
        if (!executeCommand(projectDir, buildCmd, 180)) {
            acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.FAILED, "项目构建失败", 60));
            return false;
        }

        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.FAILED, "构建完成但 dist 目录未生成", 90));
            return false;
        }

        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        acceptStatus(statusConsumer, BuildStatusEvent.of(null, BuildStatusEnum.SUCCESS, "项目构建成功", 100));
        return true;
    }

    /**
     * 构建 Vue 项目（通过 BuildStatusSseService 独立通道推送）。
     *
     * @param projectPath 项目根目录路径
     * @param appId       应用ID（为 null 时不推送状态）
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath, Long appId) {
        Consumer<BuildStatusEvent> consumer = event -> pushStatus(appId, event.getStatus(), event.getMessage(), event.getProgress());
        boolean success = buildProject(projectPath, consumer);
        if (buildStatusSseService != null && appId != null) {
            buildStatusSseService.completeBuildStatus(appId);
        }
        return success;
    }

    /**
     * 构建 Vue 项目（兼容旧接口，不推送任何状态）
     */
    public boolean buildProject(String projectPath) {
        return buildProject(projectPath, (Consumer<BuildStatusEvent>) null);
    }

    private static void acceptStatus(Consumer<BuildStatusEvent> consumer, BuildStatusEvent event) {
        if (consumer != null) {
            consumer.accept(event);
        }
    }

    /**
     * 异步构建项目（不阻塞主流程）
     */
    public void buildProjectAsync(String projectPath, Long appId) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath, appId);
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
                pushStatus(appId, BuildStatusEnum.FAILED, "构建异常: " + e.getMessage(), 0);
                if (buildStatusSseService != null && appId != null) {
                    buildStatusSseService.completeBuildStatus(appId);
                }
            }
        });
    }

    /**
     * 异步构建项目（兼容旧接口）
     */
    public void buildProjectAsync(String projectPath) {
        buildProjectAsync(projectPath, null);
    }
}
