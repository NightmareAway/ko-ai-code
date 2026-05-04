package cn.ko_ai_code.com.koaicode.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.ko_ai_code.com.koaicode.langgraph4j.model.dto.ImageResource;
import cn.ko_ai_code.com.koaicode.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String UNDRAW_BASE_URL = "https://undraw.co";
    private static volatile String cachedBuildId;
    private static volatile long cachedBuildIdTime;

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        String buildId = getBuildId();
        if (buildId == null) {
            log.error("无法获取 undraw.co 的 buildId，跳过插画搜索");
            return imageList;
        }
        int searchCount = 12;
        String apiUrl = String.format("%s/_next/data/%s/search/%s.json?term=%s",
                UNDRAW_BASE_URL, buildId, query, query);

        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.warn("Undraw API 响应异常，状态码: {}, 将刷新 buildId 缓存", response.getStatus());
                cachedBuildId = null;
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pageProps = result.getJSONObject("pageProps");
            if (pageProps == null) {
                return imageList;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, initialResults.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = initialResults.getJSONObject(i);
                String title = illustration.getStr("title", "插画");
                String media = illustration.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("搜索插画失败：{}", e.getMessage(), e);
        }
        return imageList;
    }

    private String getBuildId() {
        if (cachedBuildId != null && System.currentTimeMillis() - cachedBuildIdTime < 3_600_000) {
            return cachedBuildId;
        }
        synchronized (UndrawIllustrationTool.class) {
            if (cachedBuildId != null && System.currentTimeMillis() - cachedBuildIdTime < 3_600_000) {
                return cachedBuildId;
            }
            try (HttpResponse response = HttpRequest.get(UNDRAW_BASE_URL).timeout(10000).execute()) {
                if (!response.isOk()) {
                    log.error("获取 undraw.co 首页失败，状态码: {}", response.getStatus());
                    return cachedBuildId;
                }
                String body = response.body();
                int idx = body.indexOf("\"buildId\":\"");
                if (idx > 0) {
                    int start = idx + 11;
                    int end = body.indexOf("\"", start);
                    if (end > start) {
                        cachedBuildId = body.substring(start, end);
                        cachedBuildIdTime = System.currentTimeMillis();
                        log.info("undraw.co 当前 buildId: {}", cachedBuildId);
                        return cachedBuildId;
                    }
                }
                log.error("未能从 undraw.co 首页解析出 buildId");
            } catch (Exception e) {
                log.error("获取 undraw.co buildId 失败: {}", e.getMessage(), e);
            }
        }
        return cachedBuildId;
    }
}
