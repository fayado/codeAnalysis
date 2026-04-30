package com.mimo.analysis.cli;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.model.ClassInfo;
import com.mimo.analysis.core.model.ClassOrigin;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 结果合并器，负责将静态字节码分析结果与 Agent 运行时采集结果合并。
 *
 * 合并逻辑：
 * 1. 解析 Agent 输出文件，获取运行时实际加载的类名集合
 * 2. 遍历静态分析发现的类：
 *    - 如果该类也在 Agent 结果中出现，标记来源为 {@link ClassOrigin#BOTH}
 *    - 否则保持来源为 {@link ClassOrigin#STATIC}
 * 3. 处理仅被 Agent 发现的类（静态分析未发现）：
 *    - 通过过滤器检查后，标记来源为 {@link ClassOrigin#AGENT}
 * 4. 复制静态分析的所有引用关系到合并报告中
 *
 * Agent 输出文件格式支持：
 * - 纯文本格式（每行一个类名，# 开头为注释行）
 * - 简化 JSON 格式（自动清理引号和括号）
 */
public class ResultCombiner {

    /**
     * 合并静态分析结果和 Agent 采集结果。
     *
     * @param staticReport  静态分析报告
     * @param agentDataFile Agent 输出数据文件
     * @param filter        类过滤器
     * @return 合并后的分析报告
     * @throws IOException 读取 Agent 数据文件失败时抛出
     */
    public static AnalysisReport combine(AnalysisReport staticReport, File agentDataFile,
                                          ClassFilter filter) throws IOException {
        // 解析 Agent 输出文件，获取运行时加载的类名集合
        Set<String> agentClasses = parseAgentOutput(agentDataFile);
        AnalysisReport combined = new AnalysisReport("combined");

        // 处理静态分析发现的类
        for (ClassInfo staticClass : staticReport.getAllClasses()) {
            String name = staticClass.getClassName();
            if (agentClasses.contains(name)) {
                // 同时被静态分析和 Agent 发现，标记为 BOTH
                ClassInfo merged = staticClass.toBuilder()
                        .origin(ClassOrigin.BOTH)
                        .build();
                combined.addClass(merged);
                // 从 Agent 集合中移除，剩余的为仅 Agent 发现的类
                agentClasses.remove(name);
            } else {
                // 仅被静态分析发现
                combined.addClass(staticClass);
            }
        }

        // 处理仅被 Agent 发现的类（静态分析未发现的）
        for (String agentClassName : agentClasses) {
            if (filter.shouldInclude(agentClassName)) {
                ClassInfo agentOnly = new ClassInfo.Builder(agentClassName)
                        .jarSource("(agent-detected)")
                        .origin(ClassOrigin.AGENT)
                        .build();
                combined.addClass(agentOnly);
            }
        }

        // 复制静态分析的所有引用关系
        combined.addReferences(staticReport.getAllReferences());

        return combined;
    }

    /**
     * 解析 Agent 输出文件，提取类名集合。
     *
     * 支持两种格式：
     * - 纯文本格式：每行一个类名，# 开头为注释行，空行跳过
     * - 简化 JSON 格式：自动清理引号、逗号、括号等 JSON 语法字符
     *
     * @param agentFile Agent 输出文件
     * @return 类名集合（保持插入顺序）
     * @throws IOException 读取文件失败时抛出
     */
    private static Set<String> parseAgentOutput(File agentFile) throws IOException {
        Set<String> classes = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(agentFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // 处理可能的 JSON 格式：清理语法字符
                line = line.replace("\"", "").replace(",", "").trim();
                // 过滤无效的 JSON 结构字符
                if (!line.isEmpty() && !line.equals("classes") && !line.equals("]")
                        && !line.equals("{") && !line.equals("}")) {
                    classes.add(line);
                }
            }
        }
        return classes;
    }
}
