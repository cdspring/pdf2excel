package com.pdf2excel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliRunnerTest {

    @Test
    void cliApplication_helpReturnsZero() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = Pdf2ExcelCliApplication.run(new String[]{"--help"}, new PrintStream(out));

        assertEquals(0, code);
        assertTrue(out.toString().contains("pdf2excel"));
    }

    @Test
    void cliApplication_unknownCommandReturnsOne() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = Pdf2ExcelCliApplication.run(new String[]{"unknown"}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("未知命令"));
    }

    @Test
    void cliApplication_dispatchesConvertCommand(@TempDir Path tempDir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = Pdf2ExcelCliApplication.run(
                new String[]{"convert", tempDir.toString()}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("未找到 PDF 文件"));
    }

    @Test
    void cliApplication_dispatchesValidateCommand(@TempDir Path tempDir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = Pdf2ExcelCliApplication.run(
                new String[]{"validate", tempDir.toString()}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("未找到 PDF 文件"));
    }

    @Test
    void convertRunner_helpReturnsZero() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ConvertRunner.run(new String[]{"--help"}, new PrintStream(out));

        assertEquals(0, code);
        assertTrue(out.toString().contains("ConvertRunner"));
    }

    @Test
    void convertRunner_emptyDirectoryReturnsOne(@TempDir Path tempDir) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ConvertRunner.run(new String[]{tempDir.toString()}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("未找到 PDF 文件"));
    }

    @Test
    void convertRunner_missingInputDirectoryReturnsOne(@TempDir Path tempDir) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ConvertRunner.run(
                new String[]{tempDir.resolve("missing").toString()}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("输入目录不存在或不是目录"));
    }

    @Test
    void convertRunner_tooManyArgsReturnsOne() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ConvertRunner.run(new String[]{"a", "b", "c"}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("参数过多"));
    }

    @Test
    void validationRunner_helpReturnsZero() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ValidationRunner.run(new String[]{"-h"}, new PrintStream(out));

        assertEquals(0, code);
        assertTrue(out.toString().contains("ValidationRunner"));
    }

    @Test
    void validationRunner_emptyDirectoryReturnsOne(@TempDir Path tempDir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ValidationRunner.run(new String[]{tempDir.toString()}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("未找到 PDF 文件"));
    }

    @Test
    void validationRunner_missingInputDirectoryReturnsOne(@TempDir Path tempDir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ValidationRunner.run(
                new String[]{tempDir.resolve("missing").toString()}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("输入目录不存在或不是目录"));
    }

    @Test
    void validationRunner_tooManyArgsReturnsOne() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = ValidationRunner.run(new String[]{"a", "b", "c"}, new PrintStream(out));

        assertEquals(1, code);
        assertTrue(out.toString().contains("参数过多"));
    }
}
