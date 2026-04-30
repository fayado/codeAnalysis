package com.mimo.analysis.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CLI 命令行参数解析器，负责解析用户传入的命令行参数。
 *
 * 支持的参数：
 * - 第一个位置参数：分析模式（static/agent/combine）
 * - -i, --input：输入文件（JAR/WAR），可多次指定
 * - -o, --output：输出文件路径（默认输出到标准输出）
 * - -f, --format：输出格式（text/json/dot，默认 text）
 * - --exclude：额外的包名排除前缀
 * - --include：额外的包名包含前缀（优先级高于 exclude）
 * - --max-nodes：DOT 图的最大节点数（默认 200）
 * - --static：静态分析报告文件（combine 模式使用）
 * - --agent：Agent 输出文件（agent/combine 模式使用）
 * - --verbose：启用详细输出
 * - -h, --help：显示帮助信息
 *
 * 使用示例：
 * <pre>
 * CliArguments args = CliArguments.parse(new String[]{"static", "-i", "app.jar", "-f", "json"});
 * </pre>
 */
public class CliArguments {

    /** 分析模式（static/agent/combine） */
    private AnalysisMode mode;

    /** 输入文件列表（JAR/WAR 文件或目录） */
    private List<File> inputFiles = new ArrayList<>();

    /** Agent 输出数据文件 */
    private File agentDataFile;

    /** 静态分析报告文件 */
    private File staticReportFile;

    /** 输出文件路径，为 null 时输出到标准输出 */
    private File outputFile;

    /** 输出格式（text/json/dot），默认为 text */
    private String outputFormat = "text";

    /** 额外的包名排除前缀列表 */
    private List<String> additionalExclusions = new ArrayList<>();

    /** 额外的包名包含前缀列表（优先级高于排除规则） */
    private List<String> additionalInclusions = new ArrayList<>();

    /** DOT 图的最大节点数，默认 200 */
    private int dotMaxNodes = 200;

    /** 是否启用详细输出模式 */
    private boolean verbose;

    /**
     * 解析命令行参数，返回解析结果。
     *
     * 解析规则：
     * - 以 '-' 开头的为选项参数
     * - 第一个非选项参数为分析模式
     * - 选项参数后紧跟的下一个参数为其值
     *
     * @param args 命令行参数数组
     * @return 解析后的参数对象
     */
    public static CliArguments parse(String[] args) {
        CliArguments result = new CliArguments();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-h":
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
                case "-i":
                case "--input":
                    if (i + 1 < args.length) {
                        result.inputFiles.add(new File(args[++i]));
                    }
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        result.outputFile = new File(args[++i]);
                    }
                    break;
                case "-f":
                case "--format":
                    if (i + 1 < args.length) {
                        result.outputFormat = args[++i];
                    }
                    break;
                case "--exclude":
                    if (i + 1 < args.length) {
                        result.additionalExclusions.add(args[++i]);
                    }
                    break;
                case "--include":
                    if (i + 1 < args.length) {
                        result.additionalInclusions.add(args[++i]);
                    }
                    break;
                case "--max-nodes":
                    if (i + 1 < args.length) {
                        result.dotMaxNodes = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--static":
                    if (i + 1 < args.length) {
                        result.staticReportFile = new File(args[++i]);
                    }
                    break;
                case "--agent":
                    if (i + 1 < args.length) {
                        result.agentDataFile = new File(args[++i]);
                    }
                    break;
                case "--verbose":
                    result.verbose = true;
                    break;
                default:
                    // 第一个非选项参数作为分析模式
                    if (result.mode == null) {
                        try {
                            result.mode = AnalysisMode.valueOf(arg.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            System.err.println("Unknown mode: " + arg);
                            printUsage();
                            System.exit(1);
                        }
                    }
                    break;
            }
        }

        return result;
    }

    /**
     * 打印命令行使用帮助信息。
     */
    public static void printUsage() {
        System.out.println("Usage: analysis-cli <mode> [options]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  static    Analyze JAR/WAR files using bytecode analysis");
        System.out.println("  agent     Parse output from the Java agent");
        System.out.println("  combine   Merge static analysis and agent results");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -i, --input <file>      Input JAR/WAR file(s) (static mode, can repeat)");
        System.out.println("  -o, --output <file>     Output file (default: stdout)");
        System.out.println("  -f, --format <fmt>      Output format: text, json, dot (default: text)");
        System.out.println("  --exclude <pkg>         Additional package prefix to exclude");
        System.out.println("  --include <pkg>         Package prefix to force-include");
        System.out.println("  --max-nodes <n>         Max nodes for DOT graph (default: 200)");
        System.out.println("  --static <file>         Static analysis report JSON (combine mode)");
        System.out.println("  --agent <file>          Agent output file (agent/combine mode)");
        System.out.println("  --verbose               Enable verbose output");
        System.out.println("  -h, --help              Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  analysis-cli static -i app.jar -o report.json -f json");
        System.out.println("  analysis-cli static -i app.war -o deps.dot -f dot --max-nodes 150");
        System.out.println("  analysis-cli agent --agent agent-output.txt -o report.json -f json");
        System.out.println("  analysis-cli combine --static report.json --agent agent-output.txt -o combined.json -f json");
    }

    /** 获取分析模式 */
    public AnalysisMode getMode() { return mode; }

    /** 获取输入文件列表 */
    public List<File> getInputFiles() { return inputFiles; }

    /** 获取 Agent 数据文件 */
    public File getAgentDataFile() { return agentDataFile; }

    /** 获取静态分析报告文件 */
    public File getStaticReportFile() { return staticReportFile; }

    /** 获取输出文件路径 */
    public File getOutputFile() { return outputFile; }

    /** 获取输出格式 */
    public String getOutputFormat() { return outputFormat; }

    /** 获取额外排除前缀列表 */
    public List<String> getAdditionalExclusions() { return additionalExclusions; }

    /** 获取额外包含前缀列表 */
    public List<String> getAdditionalInclusions() { return additionalInclusions; }

    /** 获取 DOT 图最大节点数 */
    public int getDotMaxNodes() { return dotMaxNodes; }

    /** 是否启用详细输出 */
    public boolean isVerbose() { return verbose; }
}
