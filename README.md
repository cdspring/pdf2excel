# pdf2excel

PDF 账单批量转 Excel 工具。

## 基线

- 当前改进前基线: `baseline-before-improvement`

## 构建与测试

要求 Java 8+ 和 Maven。

```bash
mvn test
mvn package
```

## 命令行

转换目录下所有 PDF 为 Excel:

```bash
java -jar target/pdf2excel-1.0.0.jar convert <pdf目录> [输出目录]
```

校验目录下所有 PDF 并生成报告:

```bash
java -jar target/pdf2excel-1.0.0.jar validate <pdf目录> [报告文件]
```

帮助:

```bash
java -jar target/pdf2excel-1.0.0.jar --help
java -jar target/pdf2excel-1.0.0.jar convert --help
java -jar target/pdf2excel-1.0.0.jar validate --help
```

## 结果说明

- `0`: 成功
- `1`: 运行失败或未找到输入文件
- `2`: 校验出现错误但仍完成处理

校验报告会区分无警告通过和带警告通过。带警告通过表示 PDF 已完成解析和导出，但存在需复核的业务口径差异或原始账单异常。

## 交付检查

发布前执行 `RELEASE_CHECKLIST.md` 中的构建、Jar 冒烟和逐个 PDF 校验步骤。
