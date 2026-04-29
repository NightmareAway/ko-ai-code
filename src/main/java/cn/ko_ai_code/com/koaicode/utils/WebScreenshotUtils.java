package cn.ko_ai_code.com.koaicode.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.ko_ai_code.com.koaicode.exception.BusinessException;
import cn.ko_ai_code.com.koaicode.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class WebScreenshotUtils {

    /**
     * Chrome 浏览器的常见安装路径
     */
    private static final String[] CHROME_BINARY_PATHS = {
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Chromium\\Application\\chrome.exe",
            "/usr/bin/google-chrome",
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium",
            "/snap/bin/chromium"
    };

    /**
     * 查找 Chrome 浏览器可执行文件路径
     *
     * @return Chrome 浏览器路径，未找到返回 null
     */
    private static String findChromeBinaryPath() {
        // 1. 尝试从环境变量获取
        String envPath = System.getenv("CHROME_BIN");
        if (StrUtil.isNotBlank(envPath) && Files.exists(Paths.get(envPath))) {
            log.info("从环境变量 CHROME_BIN 找到 Chrome: {}", envPath);
            return envPath;
        }

        envPath = System.getenv("CHROME_PATH");
        if (StrUtil.isNotBlank(envPath) && Files.exists(Paths.get(envPath))) {
            log.info("从环境变量 CHROME_PATH 找到 Chrome: {}", envPath);
            return envPath;
        }

        // 2. 尝试从 PATH 中查找
        try {
            String whichChrome = new String(java.util.Objects.requireNonNull(
                    Runtime.getRuntime().exec(
                            System.getProperty("os.name", "").toLowerCase().contains("win") ?
                                    new String[]{"where", "chrome"} :
                                    new String[]{"which", "google-chrome", "chromium-browser", "chromium", "chrome"}
                    ).getInputStream().readAllBytes()
            )).trim().split("\\r?\\n")[0];
            if (StrUtil.isNotBlank(whichChrome) && Files.exists(Paths.get(whichChrome))) {
                log.info("从 PATH 找到 Chrome: {}", whichChrome);
                return whichChrome;
            }
        } catch (Exception e) {
            // ignore
        }

        // 3. 尝试常见安装路径
        for (String path : CHROME_BINARY_PATHS) {
            if (Files.exists(Paths.get(path))) {
                log.info("从常见路径找到 Chrome: {}", path);
                return path;
            }
        }

        log.warn("未找到 Chrome 浏览器，请确保 Chrome 已安装，或设置环境变量 CHROME_BIN 或 CHROME_PATH");
        return null;
    }

    /**
     * 初始化 Chrome 浏览器驱动（每次调用创建新实例，确保线程安全）
     */
    private WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().setup();

            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();

            // 查找并设置 Chrome 二进制路径（解决 ChromeDriver 找不到 Chrome 的问题）
            String chromeBinaryPath = findChromeBinaryPath();
            if (chromeBinaryPath != null) {
                options.setBinary(chromeBinaryPath);
                log.info("已设置 Chrome 二进制路径: {}", chromeBinaryPath);
            } else {
                log.warn("未设置 Chrome 二进制路径，ChromeDriver 将尝试自动查找");
            }

            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败: " + e.getMessage());
        }
    }

    /**
     * 保存图片到文件
     */
    private void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片
     */
    private void compressImage(String originalImagePath, String compressedImagePath) {
        // 压缩图片质量（0.1 = 10% 质量）
        final float COMPRESSION_QUALITY = 0.5f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 等待页面加载完成
     */
    private void waitForPageLoad(WebDriver driver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 等待 document.readyState 为complete
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }

    /**
     * 生成网页截图
     *
     * @param webUrl 网页URL
     * @return 压缩后的截图文件路径，失败返回null
     */
    public String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return null;
        }

        WebDriver driver = null;
        try {
            // 每个请求创建独立的 WebDriver 实例（线程安全）
            driver = initChromeDriver(1600, 900);

            // 创建临时目录
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);

            // 图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始截图文件路径
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;

            // 访问网页
            driver.get(webUrl);
            // 等待页面加载完成
            waitForPageLoad(driver);
            // 截图
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            // 保存原始图片
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);

            // 压缩图片
            final String COMPRESSION_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESSION_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功: {}", compressedImagePath);

            // 删除原始图片，只保留压缩图片
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            return null;
        } finally {
            // 确保 WebDriver 被正确关闭，避免资源泄漏
            if (driver != null) {
                try {
                    driver.quit();
                    log.info("Chrome WebDriver 已关闭");
                } catch (Exception e) {
                    log.warn("关闭 Chrome WebDriver 时出现异常", e);
                }
            }
        }
    }


}
