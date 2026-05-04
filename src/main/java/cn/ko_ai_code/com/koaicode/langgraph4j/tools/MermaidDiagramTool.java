package cn.ko_ai_code.com.koaicode.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.ko_ai_code.com.koaicode.exception.BusinessException;
import cn.ko_ai_code.com.koaicode.exception.ErrorCode;
import cn.ko_ai_code.com.koaicode.langgraph4j.model.dto.ImageResource;
import cn.ko_ai_code.com.koaicode.langgraph4j.model.enums.ImageCategoryEnum;
import cn.ko_ai_code.com.koaicode.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;
    
    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // 上传到COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 清理临时文件
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) {
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        try {
            FileUtil.writeUtf8String(mermaidCode, tempInputFile);
            String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
            ProcessBuilder pb = new ProcessBuilder(
                    command, "-i", tempInputFile.getAbsolutePath(),
                    "-o", tempOutputFile.getAbsolutePath(),
                    "-b", "transparent");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            Thread reader = new Thread(() -> {
                try {
                    output.append(IoUtil.read(process.getInputStream(), Charset.defaultCharset()));
                } catch (Exception ignored) {
                }
            }, "mmdc-reader");
            reader.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "Mermaid CLI 执行超时（30秒），输出: " + output);
            }
            reader.join(5000);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "Mermaid CLI 执行失败，退出码: " + exitCode + "，输出: " + output);
            }
            if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "Mermaid CLI 执行完成但未生成有效输出文件");
            }
            FileUtil.del(tempInputFile);
            return tempOutputFile;
        } catch (BusinessException e) {
            FileUtil.del(tempInputFile);
            FileUtil.del(tempOutputFile);
            throw e;
        } catch (Exception e) {
            FileUtil.del(tempInputFile);
            FileUtil.del(tempOutputFile);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Mermaid CLI 执行异常: " + e.getMessage());
        }
    }
}
