package com.mimo.analysis.agent;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.filter.DefaultClassFilter;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * Java Agent 入口类，负责初始化类使用情况采集的 Agent。
 *
 * 该类提供两个入口方法：
 * - {@link #premain(String, Instrumentation)}：在应用启动前执行（通过 -javaagent 参数指定）
 * - {@link #agentmain(String, Instrumentation)}：在应用运行时动态附加执行
 *
 * Agent 参数格式为逗号分隔的 key=value 对：
 * - output=文件路径：指定输出文件路径（默认：class-usage-agent-output.txt）
 * - exclude=包名前缀：添加额外的排除规则
 * - include=包名前缀：添加额外的包含规则（优先级高于 exclude）
 *
 * 使用示例：
 * <pre>
 * java -javaagent:analysis-agent.jar="output=result.txt,exclude=com.example.internal" -jar app.jar
 * </pre>
 *
 * 初始化流程：
 * 1. 解析 Agent 参数
 * 2. 创建类过滤器（默认过滤器 + 自定义排除/包含规则）
 * 3. 初始化数据收集器
 * 4. 注册类加载转换器
 * 5. 注册 JVM 关闭钩子，在退出时保存结果
 */
public class ClassUsageAgent {

    /**
     * Agent 预启动入口，在应用 main 方法之前执行。
     * 通过 -javaagent JVM 参数指定时调用。
     *
     * @param agentArgs Agent 参数字符串（逗号分隔的 key=value 对）
     * @param inst      Instrumentation 实例，用于注册类加载转换器
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[ClassUsageAgent] Agent starting...");
        initialize(agentArgs, inst);
    }

    /**
     * Agent 动态附加入口，在应用运行时通过 Attach API 动态加载时调用。
     *
     * @param agentArgs Agent 参数字符串（逗号分隔的 key=value 对）
     * @param inst      Instrumentation 实例，用于注册类加载转换器
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[ClassUsageAgent] Agent attached (agentmain)...");
        initialize(agentArgs, inst);
    }

    /**
     * 初始化 Agent 的核心逻辑。
     *
     * 处理流程：
     * 1. 设置默认输出文件路径
     * 2. 解析 Agent 参数（output、exclude、include）
     * 3. 创建带自定义规则的默认类过滤器
     * 4. 初始化数据收集器
     * 5. 注册字节码转换器（支持类重定义）
     * 6. 注册 shutdown hook 保存结果
     *
     * @param agentArgs Agent 参数字符串
     * @param inst      Instrumentation 实例
     */
    private static void initialize(String agentArgs, Instrumentation inst) {
        // 默认输出文件路径
        String outputFile = "class-usage-agent-output.txt";
        List<String> additionalExclusions = new ArrayList<>();
        List<String> additionalInclusions = new ArrayList<>();

        // 解析 Agent 参数：逗号分隔的 key=value 对
        if (agentArgs != null && !agentArgs.isEmpty()) {
            String[] parts = agentArgs.split(",");
            for (String part : parts) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    switch (key) {
                        case "output":
                            outputFile = value;
                            break;
                        case "exclude":
                            additionalExclusions.add(value);
                            break;
                        case "include":
                            additionalInclusions.add(value);
                            break;
                    }
                }
            }
        }

        // 创建类过滤器（默认过滤器 + 自定义规则）
        ClassFilter filter = new DefaultClassFilter(additionalExclusions, additionalInclusions);
        AgentDataCollector.initialize(outputFile, filter);

        // 注册字节码转换器（true 表示支持类重定义）
        inst.addTransformer(new ClassUsageTransformer(), true);

        // 注册 JVM 关闭钩子，在退出时保存采集结果
        Runtime.getRuntime().addShutdownHook(new Thread(AgentDataCollector::saveResults));

        System.out.println("[ClassUsageAgent] Agent initialized. Output: " + outputFile);
    }
}
