# 发布检查清单

## 基线保护

- 改进前基线标签: `baseline-before-improvement`
- 本轮改动不删除历史文件，不清理已跟踪构建产物。

## 构建验证

```bash
mvn test
mvn package -DskipTests
```

## Jar 冒烟验证

```bash
java -jar target/pdf2excel-1.0.0.jar --help
java -jar target/pdf2excel-1.0.0.jar convert --help
java -jar target/pdf2excel-1.0.0.jar validate --help
java -jar target/pdf2excel-1.0.0.jar validate . target/validation-smoke-report.txt
java -jar target/pdf2excel-1.0.0.jar convert . target/smoke-output
```

## 交付判定

- 解析失败或运行失败: 不可交付，必须继续修复。
- 校验错误: 不可交付，必须继续修复或确认原始 PDF 口径。
- 带警告通过: 可交付但需在报告中说明业务原因。
- 无警告通过: 可直接交付。

## 当前样例警告说明

- `2026.3月电信集团户账单明细(1).pdf`: 电费合计差异约 0.46%，为表头本期电费与逐户明细费用口径差异。
- `统一账单  03月电信.pdf`: 电费合计差异约 0.47%，为表头本期电费与逐户明细费用口径差异。
- `金牛小电信账单.pdf`: 抄表读数与电量列存在原始账单数据异常，程序保留警告并按 PDF 原文导出。
