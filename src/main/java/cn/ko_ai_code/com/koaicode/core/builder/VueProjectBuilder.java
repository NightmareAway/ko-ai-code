package cn.ko_ai_code.com.koaicode.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import cn.ko_ai_code.com.koaicode.model.dto.build.BuildStatusEvent;
import cn.ko_ai_code.com.koaicode.model.enums.BuildStatusEnum;
import cn.ko_ai_code.com.koaicode.service.BuildStatusSseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class VueProjectBuilder {

    @Resource
    private BuildStatusSseService buildStatusSseService;
    /**
     * 推送构建状态
     *
     * @param appId    应用ID
     * @param status   构建状态
     * @param message  消息
     * @param progress 进度
     */
    private void pushStatus(Long appId, BuildStatusEnum status, String message, Integer progress) {
        if (buildStatusSseService != null && appId != null) {
            BuildStatusEvent event = BuildStatusEvent.of(appId, status, message, progress);
            buildStatusSseService.pushBuildStatus(event);
        }
    }

    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

    /**
     * 执行 npm install 命令
     *
     * @param projectDir 项目目录
     * @param appId      应用ID
     */
    private boolean executeNpmInstall(File projectDir, Long appId) {
        log.info("执行 npm install...");
        pushStatus(appId, BuildStatusEnum.INSTALLING, "正在安装依赖包...", 20);
        String command = String.format("%s install", buildCommand("npm"));
        boolean success = executeCommand(projectDir, command, 300); // 5分钟超时
        if (!success) {
            pushStatus(appId, BuildStatusEnum.FAILED, "依赖安装失败", 20);
        }
        return success;
    }

    /**
     * 执行 npm run build 命令
     *
     * @param projectDir 项目目录
     * @param appId      应用ID
     */
    private boolean executeNpmBuild(File projectDir, Long appId) {
        log.info("执行 npm run build...");
        pushStatus(appId, BuildStatusEnum.BUILDING, "正在构建项目...", 60);
        String command = String.format("%s run build", buildCommand("npm"));
        boolean success = executeCommand(projectDir, command, 180); // 3分钟超时
        if (!success) {
            pushStatus(appId, BuildStatusEnum.FAILED, "项目构建失败", 60);
        }
        return success;
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
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @param appId       应用ID
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath, Long appId) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            pushStatus(appId, BuildStatusEnum.FAILED, "项目目录不存在", 0);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            pushStatus(appId, BuildStatusEnum.FAILED, "package.json 文件不存在", 0);
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        pushStatus(appId, BuildStatusEnum.PENDING, "准备开始构建...", 10);
        
        // 执行 npm install
        if (!executeNpmInstall(projectDir, appId)) {
            log.error("npm install 执行失败");
            return false;
        }
        pushStatus(appId, BuildStatusEnum.INSTALLING, "依赖安装完成", 50);
        
        // 执行 npm run build
        if (!executeNpmBuild(projectDir, appId)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            pushStatus(appId, BuildStatusEnum.FAILED, "构建完成但 dist 目录未生成", 90);
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        pushStatus(appId, BuildStatusEnum.SUCCESS, "项目构建成功", 100);
        // 完成构建状态流
        if (buildStatusSseService != null && appId != null) {
            buildStatusSseService.completeBuildStatus(appId);
        }
        return true;
    }

    /**
     * 构建 Vue 项目（兼容旧接口）
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        return buildProject(projectPath, null);
    }


    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     * @param appId       应用ID
     */
    public void buildProjectAsync(String projectPath, Long appId) {
        // 在单独的线程中执行构建，避免阻塞主流程
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
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        buildProjectAsync(projectPath, null);
    }
}
