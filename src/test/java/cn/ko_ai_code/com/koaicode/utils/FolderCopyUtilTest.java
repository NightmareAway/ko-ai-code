package cn.ko_ai_code.com.koaicode.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FolderCopyUtil 单元测试
 *
 * @author ko
 */
class FolderCopyUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCopyAndRenameFolder() throws IOException {
        Path srcParent = tempDir.resolve("src_root");
        Path srcDir = srcParent.resolve("template");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("index.html"), "<html></html>");
        Files.createDirectories(srcDir.resolve("css"));
        Files.writeString(srcDir.resolve("css").resolve("style.css"), "body {}");

        Path destParent = tempDir.resolve("dest_root");

        FolderCopyUtil.copyFolder(
                srcParent.toString(), "template",
                destParent.toString(), "vue_project_10");

        Path destDir = destParent.resolve("vue_project_10");
        Assertions.assertThat(destDir).isDirectory();
        Assertions.assertThat(destDir.resolve("index.html")).exists().isRegularFile()
                .content().isEqualTo("<html></html>");
        Assertions.assertThat(destDir.resolve("css").resolve("style.css")).exists().isRegularFile()
                .content().isEqualTo("body {}");
    }

    @Test
    void shouldThrowWhenSourceNotExists() {
        Path srcParent = tempDir.resolve("nonexistent");
        Path destParent = tempDir.resolve("dest_root");

        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> FolderCopyUtil.copyFolder(
                        srcParent.toString(), "template",
                        destParent.toString(), "vue_project_10"))
                .withMessageContaining("源路径不存在");
    }

    @Test
    void shouldThrowWhenSourceIsNotDirectory() throws IOException {
        Path srcFile = tempDir.resolve("file.txt");
        Files.writeString(srcFile, "hello");
        Path destParent = tempDir.resolve("dest_root");

        Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> FolderCopyUtil.copyFolder(
                        tempDir.toString(), "file.txt",
                        destParent.toString(), "vue_project_10"))
                .withMessageContaining("源路径不是目录");
    }

    @Test
    void shouldOverwriteWhenDestinationExists() throws IOException {
        Path srcParent = tempDir.resolve("src_root");
        Path srcDir = srcParent.resolve("template");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("data.txt"), "new");

        Path destParent = tempDir.resolve("dest_root");
        Path destDir = destParent.resolve("vue_project_10");
        Files.createDirectories(destDir);
        Files.writeString(destDir.resolve("old.txt"), "old");
        Files.writeString(destDir.resolve("data.txt"), "old");

        FolderCopyUtil.copyFolder(
                srcParent.toString(), "template",
                destParent.toString(), "vue_project_10");

        Assertions.assertThat(destDir).isDirectory();
        Assertions.assertThat(destDir.resolve("data.txt")).exists()
                .content().isEqualTo("new");
        Assertions.assertThat(destDir.resolve("old.txt")).doesNotExist();
    }

    @Test
    void shouldCopyDeepNestedAndEmptyDirectories() throws IOException {
        Path srcParent = tempDir.resolve("src_root");
        Path srcDir = srcParent.resolve("nested");
        Files.createDirectories(srcDir.resolve("a").resolve("b").resolve("c"));
        Files.writeString(srcDir.resolve("a").resolve("b").resolve("c").resolve("deep.txt"), "deep");
        Files.createDirectories(srcDir.resolve("empty_dir"));

        Path destParent = tempDir.resolve("dest_root");

        FolderCopyUtil.copyFolder(
                srcParent.toString(), "nested",
                destParent.toString(), "renamed_nested");

        Path destDir = destParent.resolve("renamed_nested");
        Assertions.assertThat(destDir).isDirectory();
        Assertions.assertThat(destDir.resolve("a").resolve("b").resolve("c").resolve("deep.txt"))
                .exists().isRegularFile().content().isEqualTo("deep");
        Assertions.assertThat(destDir.resolve("empty_dir")).exists().isDirectory();
        Assertions.assertThat(Files.list(destDir.resolve("empty_dir"))).isEmpty();
    }

    @Test
    void copyTest() throws Exception{
        FolderCopyUtil.copyFolder("tmp","template","tmp/code_output","vue_project_10");
    }
}
