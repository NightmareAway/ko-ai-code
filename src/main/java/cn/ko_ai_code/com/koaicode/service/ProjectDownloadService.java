package cn.ko_ai_code.com.koaicode.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {

    /**
     * 下载压缩项目（直接通过HttpServletResponse返回）
     *
     * @param projectPath 项目路径
     * @param downloadFileName 下载文件名字
     * @param response 响应体
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
