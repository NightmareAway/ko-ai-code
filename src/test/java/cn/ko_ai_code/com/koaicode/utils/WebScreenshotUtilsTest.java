package cn.ko_ai_code.com.koaicode.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class WebScreenshotUtilsTest {

    @Resource
    private WebScreenshotUtils webScreenshotUtils;

    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://www.baidu.com";
        String webPageScreenshot = webScreenshotUtils.saveWebPageScreenshot(testUrl);
        Assertions.assertNotNull(webPageScreenshot);
    }
}