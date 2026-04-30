package com.mimo.analysis.cli;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.filter.DefaultClassFilter;
import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.output.*;
import com.mimo.analysis.staticanalysis.JarAnalyzer;

import java.io.*;
import java.util.Arrays;

/**
 * CLI 主入口类，负责协调整个分析流程。
 *
 * 该类是整个工具的命令行入口，支持三种分析模式：
 * - static：静态字节码分析，分析 JAR/WAR 文件中的类依赖关系
 * - agent：解析 Java Agent 运行时采集的类加载数据
 * - combine：合并静态分析和 Agent 采集的结果
 *
 * 处理流程：
 * 1. 解析命令行参数
 * 2. 根据分析模式执行对应的分析逻辑
 * 3. 将分析结果以指定格式输出
 *
 * 使用示例：
 * <pre>
 * # 静态分析 JAR 文件，输出 JSON 格式
 * java -jar analysis-cli.jar static -i app.jar -o report.json -f json
 *
 * # 解析 Agent 输出
 * java -jar analysis-cli.jar agent --agent output.txt -o report.json -f json
 *
 * # 合并两种分析结果
 * java -jar analysis-cli.jar combine --static report.json --agent output.txt -o combined.json -f json
 * </pre>
 */
public class Main {

    /**
     * 程序主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            CliArguments.printUsage();
            System.exit(0);
        }

        try {
            // 解析命令行参数
            CliArguments cliArgs = CliArguments.parse(args);

            // 检查是否指定了分析模式
            if (cliArgs.getMode() == null) {
                System.err.println("Error: No analysis mode specified.");
                CliArguments.printUsage();
                System.exit(1);
            }

            // 创建类过滤器（默认过滤器 + 用户自定义规则）
            ClassFilter filter = new DefaultClassFilter(
                    cliArgs.getAdditionalExclusions(),
                    cliArgs.getAdditionalInclusions()
            );

            // 根据分析模式执行对应的分析逻辑
            AnalysisReport report;
            switch (cliArgs.getMode()) {
                case STATIC:
                    report = runStaticAnalysis(cliArgs, filter);
                    break;
                case AGENT:
                    report = runAgentAnalysis(cliArgs, filter);
                    break;
                case COMBINE:
                    report = runCombineAnalysis(cliArgs, filter);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown mode: " + cliArgs.getMode());
            }

            // 输出分析结果
            writeOutput(report, cliArgs);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 执行静态字节码分析。
     *
     * 遍历所有输入文件（JAR/WAR/目录），使用 {@link JarAnalyzer} 进行分析，
     * 将所有分析结果合并到同一个报告中。
     *
     * @param args   命令行参数
     * @param filter 类过滤器
     * @return 合并后的分析报告
     * @throws IOException 分析过程中发生 I/O 错误时抛出
     */
    private static AnalysisReport runStaticAnalysis(CliArguments args, ClassFilter filter) throws IOException {
        if (args.getInputFiles().isEmpty()) {
            System.err.println("Error: No input files specified. Use -i <file>.");
            System.exit(1);
        }

        JarAnalyzer analyzer = new JarAnalyzer(filter);
        AnalysisReport merged = new AnalysisReport("static");

        for (File inputFile : args.getInputFiles()) {
            if (!inputFile.exists()) {
                System.err.println("[WARN] File not found: " + inputFile.getPath());
                continue;
            }

            AnalysisReport fileReport;
            String name = inputFile.getName().toLowerCase();

            // 根据文件类型选择分析方法
            if (name.endsWith(".war")) {
                fileReport = analyzer.analyzeWar(inputFile);
            } else if (name.endsWith(".jar")) {
                fileReport = analyzer.analyzeJar(inputFile);
            } else if (inputFile.isDirectory()) {
                fileReport = analyzer.analyzeDirectory(inputFile);
            } else {
                System.err.println("[WARN] Skipping unsupported file type: " + inputFile.getName());
                continue;
            }

            // 合并到总报告中
            merged.merge(fileReport);

            // 详细模式下输出分析进度
            if (args.isVerbose()) {
                System.err.println("[INFO] Analyzed: " + inputFile.getName()
                        + " -> " + fileReport.getTotalClassCount() + " classes");
            }
        }

        return merged;
    }

    /**
     * 执行 Agent 结果解析。
     *
     * 解析 Agent 输出文件，生成包含运行时类加载信息的分析报告。
     *
     * @param args   命令行参数
     * @param filter 类过滤器
     * @return 分析报告
     * @throws IOException 读取 Agent 数据文件失败时抛出
     */
    private static AnalysisReport runAgentAnalysis(CliArguments args, ClassFilter filter) throws IOException {
        if (args.getAgentDataFile() == null) {
            System.err.println("Error: No agent data file specified. Use --agent <file>.");
            System.exit(1);
        }

        return ResultCombiner.combine(new AnalysisReport("static"), args.getAgentDataFile(), filter);
    }

    /**
     * 执行合并分析。
     *
     * 将静态分析结果和 Agent 采集结果合并，生成综合分析报告。
     * 静态分析输入支持 JAR/WAR 文件或之前生成的文本报告。
     *
     * @param args   命令行参数
     * @param filter 类过滤器
     * @return 合并后的分析报告
     * @throws IOException 分析过程中发生 I/O 错误时抛出
     */
    private static AnalysisReport runCombineAnalysis(CliArguments args, ClassFilter filter) throws IOException {
        if (args.getStaticReportFile() == null) {
            System.err.println("Error: No static report file specified. Use --static <file>.");
            System.exit(1);
        }
        if (args.getAgentDataFile() == null) {
            System.err.println("Error: No agent data file specified. Use --agent <file>.");
            System.exit(1);
        }

        // 根据静态分析输入文件类型选择处理方式
        AnalysisReport staticReport;
        String staticName = args.getStaticReportFile().getName().toLowerCase();
        if (staticName.endsWith(".jar") || staticName.endsWith(".war")) {
            // 输入为 JAR/WAR 文件，重新执行静态分析
            JarAnalyzer analyzer = new JarAnalyzer(filter);
            if (staticName.endsWith(".war")) {
                staticReport = analyzer.analyzeWar(args.getStaticReportFile());
            } else {
                staticReport = analyzer.analyzeJar(args.getStaticReportFile());
            }
        } else {
            // 输入为文本报告，解析类名列表
            staticReport = parseStaticReport(args.getStaticReportFile());
        }

        // 合并静态分析和 Agent 结果
        return ResultCombiner.combine(staticReport, args.getAgentDataFile(), filter);
    }

    /**
     * 解析静态分析文本报告文件。
     *
     * 支持的格式：每行一个类名，可选带 [ORIGIN] [jarSource] 后缀。
     * # 开头的行为注释行，空行跳过。
     *
     * @param file 文本报告文件
     * @return 解析后的分析报告
     * @throws IOException 读取文件失败时抛出
     */
    private static AnalysisReport parseStaticReport(File file) throws IOException {
        AnalysisReport report = new AnalysisReport("static");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // 解析格式：className [ORIGIN] [jarSource]
                int bracketIdx = line.indexOf('[');
                String className = bracketIdx > 0 ? line.substring(0, bracketIdx).trim() : line;

                if (!className.isEmpty()) {
                    report.addClass(new com.mimo.analysis.core.model.ClassInfo.Builder(className)
                            .origin(com.mimo.analysis.core.model.ClassOrigin.STATIC)
                            .build());
                }
            }
        }
        return report;
    }

    /**
     * 将分析报告以指定格式输出。
     *
     * 支持的输出格式：
     * - text：纯文本格式（默认）
     * - json：JSON 结构化格式
     * - dot：Graphviz DOT 图格式
     *
     * 输出目标支持文件或标准输出。
     *
     * @param report 分析报告
     * @param args   命令行参数（包含输出格式和输出文件信息）
     * @throws IOException 写入输出失败时抛出
     */
    private static void writeOutput(AnalysisReport report, CliArguments args) throws IOException {
        // 根据输出格式选择对应的写入器
        OutputWriter writer;
        switch (args.getOutputFormat().toLowerCase()) {
            case "json":
                writer = new JsonOutputWriter();
                break;
            case "dot":
                writer = new DotGraphOutputWriter(args.getDotMaxNodes(), true);
                break;
            case "text":
            default:
                writer = new TextOutputWriter();
                break;
        }

        // 确定输出目标（文件或标准输出）
        Writer output;
        if (args.getOutputFile() != null) {
            output = new BufferedWriter(new FileWriter(args.getOutputFile()));
        } else {
            output = new BufferedWriter(new OutputStreamWriter(System.out));
        }

        try {
            writer.write(report, output);
        } finally {
            // 仅关闭文件输出，不关闭标准输出
            if (args.getOutputFile() != null) {
                output.close();
            }
        }
    }
}
