package com.mimo.analysis.cli;

/**
 * 分析模式枚举，定义 CLI 工具支持的三种运行模式。
 *
 * 用户通过命令行第一个参数指定分析模式，不同模式对应不同的处理流程和输入要求。
 */
public enum AnalysisMode {

    /**
     * 静态分析模式：通过 ASM 字节码分析 JAR/WAR 文件中的类依赖关系。
     * 输入：JAR 或 WAR 文件（通过 -i 参数指定）
     * 输出：分析报告（支持 text/json/dot 格式）
     */
    STATIC,

    /**
     * Agent 结果解析模式：解析 Java Agent 运行时采集的类加载数据。
     * 输入：Agent 输出文件（通过 --agent 参数指定）
     * 输出：分析报告
     */
    AGENT,

    /**
     * 合并分析模式：将静态分析结果和 Agent 采集结果合并。
     * 输入：静态分析报告（--static）和 Agent 输出文件（--agent）
     * 输出：合并后的分析报告，同时标记类的来源（静态/Agent/两者兼有）
     */
    COMBINE
}
