package cn.ko_ai_code.com.koaicode.pool;

import cn.hutool.core.util.StrUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebDriver 对象池
 * 固定核心大小为 3，使用无界阻塞队列实现借出/归还的循环复用。
 *
 * @author ko
 */
@Slf4j
@Component
public class WebDriverPool {

    private static final int POOL_SIZE = 3;

    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

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

    private final BlockingQueue<WebDriver> pool = new LinkedBlockingQueue<>();
    private final List<WebDriver> allDrivers = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (int i = 0; i < POOL_SIZE; i++) {
            WebDriver driver = createDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            allDrivers.add(driver);
            pool.offer(driver);
        }
        log.info("WebDriver 对象池初始化完成，核心大小: {}", POOL_SIZE);
    }

    /**
     * 从池中借出一个 WebDriver 实例，若无空闲则阻塞等待。
     *
     * @return WebDriver 实例
     * @throws InterruptedException 如果等待被中断
     */
    public WebDriver borrow() throws InterruptedException {
        log.debug("等待获取 WebDriver，当前池中有 {} 个可用实例", pool.size());
        WebDriver driver = pool.take();
        log.debug("已借出 WebDriver 实例，剩余 {} 个可用", pool.size());
        return driver;
    }

    /**
     * 归还 WebDriver 实例到池中，供后续复用。
     *
     * @param driver 要归还的 WebDriver 实例
     */
    public void returnDriver(WebDriver driver) {
        if (driver == null) {
            log.warn("尝试归还 null WebDriver 实例，已忽略");
            return;
        }
        pool.offer(driver);
        log.debug("已归还 WebDriver 实例，当前池中有 {} 个可用", pool.size());
    }

    /**
     * 获取当前池中可用实例数量
     */
    public int availableCount() {
        return pool.size();
    }

    /**
     * 创建新的 WebDriver 实例（protected，供子类测试时覆盖）
     */
    protected WebDriver createDriver(int width, int height) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        String chromeBinaryPath = findChromeBinaryPath();
        if (chromeBinaryPath != null) {
            options.setBinary(chromeBinaryPath);
            log.info("已设置 Chrome 二进制路径: {}", chromeBinaryPath);
        } else {
            log.warn("未设置 Chrome 二进制路径，ChromeDriver 将尝试自动查找");
        }

        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments(String.format("--window-size=%d,%d", width, height));
        options.addArguments("--disable-extensions");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        return driver;
    }

    @PreDestroy
    public void destroy() {
        log.info("正在关闭 WebDriver 对象池，共 {} 个实例", allDrivers.size());
        for (WebDriver driver : allDrivers) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("关闭 WebDriver 实例时出现异常", e);
            }
        }
        allDrivers.clear();
        pool.clear();
        log.info("WebDriver 对象池已关闭");
    }

    /**
     * 查找 Chrome 浏览器可执行文件路径
     *
     * @return Chrome 浏览器路径，未找到返回 null
     */
    private static String findChromeBinaryPath() {
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

        for (String path : CHROME_BINARY_PATHS) {
            if (Files.exists(Paths.get(path))) {
                log.info("从常见路径找到 Chrome: {}", path);
                return path;
            }
        }

        log.warn("未找到 Chrome 浏览器，请确保 Chrome 已安装，或设置环境变量 CHROME_BIN 或 CHROME_PATH");
        return null;
    }
}
