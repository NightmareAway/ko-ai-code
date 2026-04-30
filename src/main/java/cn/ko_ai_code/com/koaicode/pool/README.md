# WebDriverPool 对象池

## 配置参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 核心池大小 (POOL_SIZE) | 3 | 固定创建 3 个 WebDriver 实例 |
| 等待队列 | LinkedBlockingQueue (无界) | 超出核心数时请求排队阻塞等待 |
| 窗口尺寸 | 1600x900 | headless Chrome 窗口大小 |
| 页面加载超时 | 30s | driver.manage().timeouts().pageLoadTimeout |
| 隐式等待 | 10s | driver.manage().timeouts().implicitlyWait |

## 使用方式

```java
// 1. 注入对象池
@Resource
private WebDriverPool webDriverPool;

// 2. 借出 WebDriver（若无空闲实例则阻塞等待）
WebDriver driver = webDriverPool.borrow();

try {
    // 使用 driver 执行操作
    driver.get("https://example.com");
    // ...
} finally {
    // 3. 必须归还，否则其他线程永远阻塞
    webDriverPool.returnDriver(driver);
}
```

## 生命周期

- **初始化**: `@PostConstruct` 自动创建 3 个实例
- **销毁**: `@PreDestroy` 自动关闭全部实例并释放资源
- **可中断**: `borrow()` 抛出 `InterruptedException`，调用方可优雅退出

## 线程安全

`LinkedBlockingQueue` 保证 `borrow()` / `returnDriver()` 的线程安全。多线程并发使用时遵循 FIFO 公平调度——先等待的线程先获得归还的实例。
