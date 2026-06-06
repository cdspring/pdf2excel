package com.pdf2excel;

import java.io.PrintStream;
import java.util.Arrays;

public class Pdf2ExcelCliApplication {

    public static void main(String[] args) throws Exception {
        System.exit(run(args, System.out));
    }

    public static int run(String[] args, PrintStream out) throws Exception {
        if (args == null || args.length == 0 || isHelp(args[0])) {
            printUsage(out);
            return 0;
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        if ("convert".equalsIgnoreCase(command)) {
            return ConvertRunner.run(commandArgs, out);
        }
        if ("validate".equalsIgnoreCase(command)) {
            return ValidationRunner.run(commandArgs, out);
        }

        out.println("未知命令: " + command);
        printUsage(out);
        return 1;
    }

    private static boolean isHelp(String arg) {
        return "-h".equalsIgnoreCase(arg) || "--help".equalsIgnoreCase(arg);
    }

    private static void printUsage(PrintStream out) {
        out.println("pdf2excel: PDF 账单批量转换与校验工具");
        out.println("用法:");
        out.println("  java -jar pdf2excel-1.0.0.jar convert <pdf目录> [输出目录]");
        out.println("  java -jar pdf2excel-1.0.0.jar validate <pdf目录> [报告文件]");
        out.println("  java -jar pdf2excel-1.0.0.jar --help");
    }
}
