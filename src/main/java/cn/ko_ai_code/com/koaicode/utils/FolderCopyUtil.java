package cn.ko_ai_code.com.koaicode.utils;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 文件夹复制工具类
 * 基于 NIO.2 (Files.walkFileTree) 实现高性能、跨平台的文件夹复制。
 *
 * @author ko
 */
public class FolderCopyUtil {

    /**
     * 将源文件夹完整复制到目标位置并重命名。
     * 源路径 = srcParent/srcName，目标路径 = destParent/destName。
     *
     * @param srcParent  源文件夹的父目录路径
     * @param srcName    源文件夹名称
     * @param destParent 目标文件夹的父目录路径
     * @param destName   目标文件夹名称
     * @throws IllegalArgumentException 如果源路径不存在或不是目录
     * @throws RuntimeException         如果复制过程中发生 IO 错误
     */
    public static void copyFolder(String srcParent, String srcName, String destParent, String destName) {
        Path srcPath = Paths.get(srcParent, srcName);
        Path destPath = Paths.get(destParent, destName);

        if (!Files.exists(srcPath)) {
            throw new IllegalArgumentException("源路径不存在: " + srcPath.toAbsolutePath());
        }
        if (!Files.isDirectory(srcPath)) {
            throw new IllegalArgumentException("源路径不是目录: " + srcPath.toAbsolutePath());
        }

        try {
            if (Files.exists(destPath)) {
                deleteRecursively(destPath);
            }
            Files.createDirectories(destPath.getParent());
            Files.walkFileTree(srcPath, new CopyFileVisitor(srcPath, destPath));
        } catch (IOException e) {
            throw new RuntimeException("文件夹复制失败: " + srcPath.toAbsolutePath() + " -> " + destPath.toAbsolutePath(), e);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            Files.deleteIfExists(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path srcRoot;
        private final Path destRoot;

        CopyFileVisitor(Path srcRoot, Path destRoot) {
            this.srcRoot = srcRoot;
            this.destRoot = destRoot;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetDir = destRoot.resolve(srcRoot.relativize(dir));
            Files.createDirectories(targetDir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path targetFile = destRoot.resolve(srcRoot.relativize(file));
            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
        }
    }
}
