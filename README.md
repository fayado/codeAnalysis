# Class Usage Analysis - Spring Boot 类使用分析工具

分析 Spring Boot 项目中**真正被使用的类**。结合运行时采集（Java Agent）和静态字节码分析（ASM），识别应用代码、Spring 框架、Jackson 等第三方库中实际加载的类，自动排除 JDK 和 Tomcat 等容器类。

## 功能特性

- **静态字节码分析**：基于 ASM 分析 JAR/WAR 文件，提取类之间的引用关系（继承、实现、方法调用、字段引用等）
- **运行时 Agent 采集**：通过 `-javaagent` 附加到 JVM，拦截所有类加载事件，记录运行时真正被加载的类
- **结果合并**：将静态分析和运行时采集结果合并，识别动态加载的类（反射、Spring 组件扫描等）
- **智能过滤**：自动排除 JDK 类（`java.*`、`javax.*`、`sun.*`）和 Tomcat 类（`org.apache.catalina.*`、`org.apache.tomcat.*`），支持自定义排除/包含规则
- **多种输出格式**：文本类名列表、JSON 结构化报告、DOT 依赖关系图（Graphviz）
- **Spring Boot Fat JAR 支持**：自动检测并分析 `BOOT-INF/lib/` 下的嵌套 JAR

## 构建

```bash
# 构建所有模块
mvn clean package

# 静默构建
mvn clean package -q

# 仅编译
mvn clean compile
```

构建产物：
- `analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar` — CLI 工具（fat JAR，含所有依赖）
- `analysis-agent/target/analysis-agent-1.0.0-SNAPSHOT.jar` — Java Agent

## 使用方式

### 1. 静态分析（分析 JAR/WAR 文件）

```bash
# 分析单个 JAR，输出文本格式
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i path/to/app.jar

# 分析 JAR，输出 JSON 报告
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i path/to/app.jar -o report.json -f json

# 分析 WAR 文件，输出 DOT 依赖图
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i path/to/app.war -o deps.dot -f dot

# 分析多个文件
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i app.jar -i lib2.jar -o report.json -f json

# 分析目录下的所有 JAR
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i path/to/lib-dir/ -o report.json -f json

# 自定义排除和包含规则
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i app.jar \
    --exclude com.example.internal \
    --include com.example.internal.publicapi \
    -o report.json -f json

# 限制 DOT 图节点数量（默认 200）
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i app.jar -f dot --max-nodes 100 -o deps.dot

# 详细输出模式
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i app.jar --verbose
```

### 2. 运行时 Agent 采集

```bash
# 基本用法（输出到 class-usage-agent-output.txt）
java -javaagent:analysis-agent/target/analysis-agent-1.0.0-SNAPSHOT.jar -jar your-app.jar

# 指定输出文件
java -javaagent:analysis-agent/target/analysis-agent-1.0.0-SNAPSHOT.jar=output=agent-data.txt -jar your-app.jar

# 指定输出文件 + 自定义排除规则
java -javaagent:analysis-agent/target/analysis-agent-1.0.0-SNAPSHOT.jar=output=agent-data.txt,exclude=com.example.internal -jar your-app.jar
```

Agent 参数格式：`key=value` 对，逗号分隔。支持的参数：
| 参数 | 说明 | 默认值 |
|------|------|--------|
| `output` | 输出文件路径 | `class-usage-agent-output.txt` |
| `exclude` | 额外排除的包前缀 | 无 |
| `include` | 强制包含的包前缀 | 无 |

应用正常运行后关闭，Agent 会在 JVM shutdown hook 中自动写入结果文件。

### 3. 合并静态分析和 Agent 结果

```bash
# 先做静态分析，生成报告
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static -i app.jar -o static-report.json -f json

# 再用 Agent 采集运行时数据
java -javaagent:analysis-agent/target/analysis-agent-1.0.0-SNAPSHOT.jar=output=agent-data.txt -jar app.jar

# 合并两者结果
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar combine \
    --static app.jar --agent agent-data.txt -o combined.json -f json
```

## 输出格式

### 文本格式（`-f text`）

```
# Class Usage Analysis Report
# Generated: 2026-04-29T10:30:00Z
# Mode: static
# Total classes: 1523
# Total references: 4521
#
# Format: <class-name> [<origin>] [<jar-source>]

com.example.MyApplication [STATIC] [app.jar]
org.springframework.boot.SpringApplication [STATIC] [spring-boot-3.1.0.jar]
com.fasterxml.jackson.databind.ObjectMapper [STATIC] [jackson-databind-2.15.0.jar]
```

### JSON 格式（`-f json`）

```json
{
  "metadata": {
    "generatedAt": "2026-04-29T10:30:00Z",
    "analysisMode": "static",
    "totalClasses": 1523,
    "totalReferences": 4521
  },
  "packages": {
    "com.example": {
      "classCount": 45,
      "interfaceCount": 8,
      "springComponentCount": 12,
      "classes": ["com.example.MyApplication", "..."]
    }
  },
  "classes": [
    {
      "name": "com.example.MyApplication",
      "package": "com.example",
      "origin": "STATIC",
      "isSpringComponent": true,
      "inboundReferenceCount": 3,
      "outboundReferenceCount": 5
    }
  ],
  "references": [
    { "from": "com.example.App", "to": "org.springframework.boot.SpringApplication", "type": "INVOKE" }
  ],
  "statistics": {
    "topReferencedClasses": [{ "class": "org.springframework.context.ApplicationContext", "count": 89 }],
    "topPackages": [{ "package": "com.example", "classCount": 45 }]
  }
}
```

### DOT 格式（`-f dot`）

生成 Graphviz DOT 格式的依赖关系图，按包名分组，可直接用 Graphviz 渲染：

```bash
# 渲染为 PNG
dot -Tpng deps.dot -o deps.png

# 渲染为 SVG
dot -Tsvg deps.dot -o deps.svg
```

## 默认过滤规则

### 排除的包前缀

| 分类 | 排除的前缀 |
|------|-----------|
| JDK 核心 | `java.`, `javax.`, `sun.`, `com.sun.`, `jdk.` |
| JDK XML | `org.xml.sax.`, `org.w3c.dom.` |
| Tomcat 容器 | `org.apache.catalina.`, `org.apache.tomcat.`, `org.apache.coyote.`, `org.apache.el.`, `org.apache.jasper.`, `org.apache.naming.`, `org.apache.juli.` |

### 排除的类名后缀（代理类）

- `$$EnhancerBySpringCGLIB$$`
- `$$FastClassBySpringCGLIB$$`
- `$HibernateProxy$`
- `_$$_jvst`

### 排除的 JAR 文件

`rt.jar`, `tools.jar`, `jce.jar`, `charsets.jar`, `jfr.jar`, `tomcat-embed-*.jar`, `catalina-*.jar`, `coyote-*.jar`, `jasper-*.jar`, `el-api-*.jar`, `jsp-api-*.jar`, `servlet-api-*.jar`, `websocket-*.jar`

### 自定义规则

通过 `--exclude` 和 `--include` 参数可以覆盖默认规则。`--include` 优先级高于 `--exclude`，可以强制包含被默认规则排除的包。

## 项目结构

```
codeAnalysis_MIMO/
├── pom.xml                         # 父 POM，统一依赖管理
├── analysis-core/                  # 核心模块
│   └── src/main/java/com/mimo/analysis/core/
│       ├── model/                  # 领域模型：ClassInfo, ClassReference, AnalysisReport 等
│       ├── filter/                 # 类过滤器：ClassFilter 接口, DefaultClassFilter
│       ├── output/                 # 输出器：TextOutputWriter, JsonOutputWriter, DotGraphOutputWriter
│       └── util/                   # 工具类：ClassNameUtils
├── analysis-agent/                 # Java Agent 模块
│   └── src/main/java/com/mimo/analysis/agent/
│       ├── ClassUsageAgent.java    # Agent 入口（premain/agentmain）
│       ├── ClassUsageTransformer.java  # 类加载拦截器
│       └── AgentDataCollector.java # 数据收集与持久化
├── analysis-static/                # 静态分析模块
│   └── src/main/java/com/mimo/analysis/staticanalysis/
│       ├── ClassReferenceVisitor.java    # ASM ClassVisitor，提取类级引用
│       ├── MethodReferenceVisitor.java   # ASM MethodVisitor，提取方法体引用
│       └── JarAnalyzer.java              # JAR/WAR/FatJAR 分析器
└── analysis-cli/                   # CLI 模块
    └── src/main/java/com/mimo/analysis/cli/
        ├── Main.java               # CLI 入口
        ├── CliArguments.java       # 命令行参数解析
        ├── AnalysisMode.java       # 分析模式枚举
        └── ResultCombiner.java     # 静态+Agent 结果合并
```

## 模块依赖

```
analysis-cli  ──→  analysis-static  ──→  analysis-core
             ──→  analysis-agent   ──→  analysis-core
```

## 技术栈

| 依赖 | 版本 | 用途 |
|------|------|------|
| ASM | 9.7 | 字节码分析 |
| Gson | 2.10.1 | JSON 输出（未使用 Jackson，避免循环依赖） |
| Java | 1.8+ | 编译和运行目标版本 |

## CLI 参数速查

```
用法: analysis-cli <mode> [options]

模式:
  static    分析 JAR/WAR 文件（静态字节码分析）
  agent     解析 Java Agent 的输出文件
  combine   合并静态分析和 Agent 结果

选项:
  -i, --input <file>      输入 JAR/WAR 文件（static 模式，可重复）
  -o, --output <file>     输出文件（默认 stdout）
  -f, --format <fmt>      输出格式：text, json, dot（默认 text）
  --exclude <pkg>         额外排除的包前缀
  --include <pkg>         强制包含的包前缀
  --max-nodes <n>         DOT 图最大节点数（默认 200）
  --static <file>         静态分析报告文件（combine 模式）
  --agent <file>          Agent 输出文件（agent/combine 模式）
  --verbose               详细输出模式
  -h, --help              显示帮助
```

## 测试验证

项目包含一个测试用的 Spring Boot 工程 [test-springboot-app/](test-springboot-app/)，用于验证分析工具的正确性。

### 测试工程结构

一个典型的 Spring Boot Web 应用，包含：

| 类 | 说明 |
|---|---|
| `DemoApplication` | Spring Boot 启动类 |
| `UserController` | REST 控制器，提供用户 CRUD 接口 |
| `UserService` | 用户业务服务 |
| `User` | 用户数据模型 |
| `ApiResponse` | 通用 API 响应包装 |
| `WebConfig` | CORS 跨域配置 |

依赖：`spring-boot-starter-web`、`jackson-databind`

### 构建测试工程

```bash
cd test-springboot-app
mvn clean package -q
```

### 运行分析

```bash
cd ..
java -jar analysis-cli/target/analysis-cli-1.0.0-SNAPSHOT.jar static \
    -i test-springboot-app/target/demo-0.0.1-SNAPSHOT.jar -f json -o test-report.json
```

### 验证结果

| 指标 | 数值 | 说明 |
|------|------|------|
| 总类数 | 8,403 | 包含 Spring、Jackson、Logback 等所有依赖 |
| 总引用关系 | 76,852 | 类之间的继承/调用/引用等关系 |
| 涉及包数 | 564 | — |
| 应用类（`com.example.*`） | 6 | 5 个包，全部检出 |
| Spring 框架类 | 6,426 | 417 个包，正确包含 |
| Jackson 类 | 989 | 43 个包，正确包含 |
| JDK 类 | **0** | 已正确排除 |
| Tomcat 类 | **0** | 已正确排除 |

### 应用类检出明细

```
com.example.demo.DemoApplication     [STATIC]  — 启动类
com.example.demo.config.WebConfig    [STATIC]  — 配置类
com.example.demo.controller.UserController [STATIC] — 控制器
com.example.demo.model.ApiResponse   [STATIC]  — 模型
com.example.demo.model.User          [STATIC]  — 模型
com.example.demo.service.UserService [STATIC]  — 服务
```

### 被引用最多的类（Top 10）

| 排名 | 类名 | 引用次数 |
|------|------|---------|
| 1 | `org.springframework.lang.Nullable` | 1,719 |
| 2 | `org.springframework.util.Assert` | 1,089 |
| 3 | `org.apache.commons.logging.Log` | 885 |
| 4 | `com.fasterxml.jackson.databind.JavaType` | 587 |
| 5 | `org.springframework.util.StringUtils` | 566 |
| 6 | `org.springframework.http.HttpHeaders` | 464 |
| 7 | `org.springframework.core.io.Resource` | 441 |
| 8 | `org.springframework.http.MediaType` | 401 |
| 9 | `com.fasterxml.jackson.core.JsonGenerator` | 333 |
| 10 | `org.springframework.core.ResolvableType` | 321 |

### 验证结论

- 应用代码 6 个类全部被正确检出
- Spring 框架类（6,426 个）和 Jackson 类（989 个）被正确包含
- JDK 类和 Tomcat 类被完全排除（计数为 0）
- 引用关系分析准确，`@RestController`、`@Service`、`@Configuration` 等 Spring 组件注解被正确识别
