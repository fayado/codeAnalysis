package com.mimo.analysis.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CliArguments {

    private AnalysisMode mode;
    private List<File> inputFiles = new ArrayList<>();
    private File agentDataFile;
    private File staticReportFile;
    private File outputFile;
    private String outputFormat = "text";
    private List<String> additionalExclusions = new ArrayList<>();
    private List<String> additionalInclusions = new ArrayList<>();
    private int dotMaxNodes = 200;
    private boolean verbose;

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
                    // First positional arg is the mode
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

    public AnalysisMode getMode() { return mode; }
    public List<File> getInputFiles() { return inputFiles; }
    public File getAgentDataFile() { return agentDataFile; }
    public File getStaticReportFile() { return staticReportFile; }
    public File getOutputFile() { return outputFile; }
    public String getOutputFormat() { return outputFormat; }
    public List<String> getAdditionalExclusions() { return additionalExclusions; }
    public List<String> getAdditionalInclusions() { return additionalInclusions; }
    public int getDotMaxNodes() { return dotMaxNodes; }
    public boolean isVerbose() { return verbose; }
}
