package cn.ko_ai_code.com.koaicode.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.ko_ai_code.com.koaicode.langgraph4j.model.dto.ImageResource;
import cn.ko_ai_code.com.koaicode.langgraph4j.model.enums.ImageCategoryEnum;
import cn.ko_ai_code.com.koaicode.manager.CosManager;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.ko_ai_code.com.koaicode.constant.AppConstant.PICTURE_TEMP_DIR;

@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:wan2.7-image}")
    private String imageModel;

    @Resource
    private CosManager cosManager;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // 构建 Logo 设计提示词
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .prompt(logoPrompt)
                    .size("512*512")
                    .n(1) // 生成 1 张足够，因为 AI 不知道哪张最好
                    .build();
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    String dashScopeImageUrl = imageResult.get("url");
                    if (StrUtil.isNotBlank(dashScopeImageUrl)) {
                        String finalUrl = dashScopeImageUrl;
                        // 下载到本地并上传 COS，优先使用 COS 持久 URL
                        String keyName = String.format("/logo/%s.png",
                                RandomUtil.randomString(5));
                        File destFile = FileUtil.file(PICTURE_TEMP_DIR + keyName);
                        try {
                            HttpUtil.downloadFile(dashScopeImageUrl, destFile);
                            String cosUrl = cosManager.uploadFile(keyName, destFile);
                            if (StrUtil.isNotBlank(cosUrl)) {
                                finalUrl = cosUrl;
                                log.info("Logo已上传至COS: {}", cosUrl);
                            } else {
                                log.warn("COS上传返回空URL，回退使用DashScope临时URL");
                            }
                        } catch (Exception e) {
                            log.error("下载/上传Logo文件失败，回退使用DashScope临时URL: {}", e.getMessage());
                        } finally {
                            FileUtil.del(destFile);
                        }
                        logoList.add(ImageResource.builder()
                                .category(ImageCategoryEnum.LOGO)
                                .description(description)
                                .url(finalUrl)
                                .build());
                    }
                }
            }


        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }
}
