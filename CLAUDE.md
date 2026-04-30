# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指引。

## 项目概述

分析 Spring Boot 项目中类使用情况的 Java 工具。结合运行时采集（Java Agent）和静态字节码分析（ASM），识别项目中真正被使用的类（包括 Spring、Jackson 等第三方库），排除 JDK 和 Tomcat 相关类。

## 构建命令

```bash
mvn clean package          # 构建所有模块，fat JAR 输出到 analysis-cli/target/
mvn clean package -q       # 静默构建
mvn clean compile          # 仅编译
```

尚未配置测试框架，无测试用例。

## 模块结构

Maven 多模块项目，依赖链：`cli → static → core`，`cli → agent → core`。

- **analysis-core** — 领域模型（`ClassInfo`、`ClassReference`、`AnalysisReport`）、过滤器（`DefaultClassFilter`）、输出器（text/json/dot）
- **analysis-static** — ASM 字节码分析。`JarAnalyzer` 处理 JAR/WAR/Spring Boot fat JAR。`ClassReferenceVisitor` + `MethodReferenceVisitor` 从字节码中提取类引用
- **analysis-agent** — Java Agent（`-javaagent`）。`ClassUsageTransformer` 通过 `Instrumentation` API 拦截类加载，`AgentDataCollector` 收集结果，shutdown hook 写入输出
- **analysis-cli** — CLI 入口（`Main`）。三种模式：`static`（分析 JAR）、`agent`（解析 agent 输出）、`combine`（合并两者）

## 关键设计决策

- JSON 输出使用 **Gson**（非 Jackson），避免循环依赖——Jackson 类本身也在分析范围内
- Java 源码/目标版本为 **1.8**，保证兼容性
- `DefaultClassFilter` 默认排除 `java.*`、`javax.*`、`sun.*`、`org.apache.catalina.*`、`org.apache.tomcat.*`；支持 `--exclude`/`--include` 自定义
- `AnalysisReport.merge()` 按类名去重类，按 `(from, to, type)` 三元组去重引用
- Spring Boot fat JAR 通过 `BOOT-INF/` 前缀检测，嵌套 JAR 解压到临时文件后分析
- 代码必须有详细的注释，且是中文
- 每次修改记得更新README，中文

## 包约定

所有代码位于 `com.mimo.analysis.*` 下：
- `.core.model` — 数据类
- `.core.filter` — 类包含/排除逻辑
- `.core.output` — 报告格式化器
- `.staticanalysis` — ASM 访问器和 JAR 分析器
- `.agent` — Java Agent 类
- `.cli` — CLI 入口和参数解析
