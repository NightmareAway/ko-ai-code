package cn.ko_ai_code.com.koaicode.service;

public interface ScreenshotService {
    /**
     * 生成网页截图并上传到对象存储
     * @param webUrl 网页的URL地址
     * @return 返回上传到对象存储后的URL地址
     */
    String generateAndUploadScreenshot(String webUrl);
}
