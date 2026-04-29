package com.mimo.analysis.cli;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.filter.DefaultClassFilter;
import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.output.*;
import com.mimo.analysis.staticanalysis.JarAnalyzer;

import java.io.*;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            CliArguments.printUsage();
            System.exit(0);
        }

        try {
            CliArguments cliArgs = CliArguments.parse(args);

            if (cliArgs.getMode() == null) {
                System.err.println("Error: No analysis mode specified.");
                CliArguments.printUsage();
                System.exit(1);
            }

            ClassFilter filter = new DefaultClassFilter(
                    cliArgs.getAdditionalExclusions(),
                    cliArgs.getAdditionalInclusions()
            );

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

            writeOutput(report, cliArgs);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

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

            merged.merge(fileReport);

            if (args.isVerbose()) {
                System.err.println("[INFO] Analyzed: " + inputFile.getName()
                        + " -> " + fileReport.getTotalClassCount() + " classes");
            }
        }

        return merged;
    }

    private static AnalysisReport runAgentAnalysis(CliArguments args, ClassFilter filter) throws IOException {
        if (args.getAgentDataFile() == null) {
            System.err.println("Error: No agent data file specified. Use --agent <file>.");
            System.exit(1);
        }

        return ResultCombiner.combine(new AnalysisReport("static"), args.getAgentDataFile(), filter);
    }

    private static AnalysisReport runCombineAnalysis(CliArguments args, ClassFilter filter) throws IOException {
        if (args.getStaticReportFile() == null) {
            System.err.println("Error: No static report file specified. Use --static <file>.");
            System.exit(1);
        }
        if (args.getAgentDataFile() == null) {
            System.err.println("Error: No agent data file specified. Use --agent <file>.");
            System.exit(1);
        }

        // For combine mode, we need to re-run static analysis if the static report
        // is a JAR/WAR, or parse it if it's JSON
        AnalysisReport staticReport;
        String staticName = args.getStaticReportFile().getName().toLowerCase();
        if (staticName.endsWith(".jar") || staticName.endsWith(".war")) {
            JarAnalyzer analyzer = new JarAnalyzer(filter);
            if (staticName.endsWith(".war")) {
                staticReport = analyzer.analyzeWar(args.getStaticReportFile());
            } else {
                staticReport = analyzer.analyzeJar(args.getStaticReportFile());
            }
        } else {
            // Assume it's a text file with class names (one per line)
            staticReport = parseStaticReport(args.getStaticReportFile());
        }

        return ResultCombiner.combine(staticReport, args.getAgentDataFile(), filter);
    }

    private static AnalysisReport parseStaticReport(File file) throws IOException {
        AnalysisReport report = new AnalysisReport("static");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Parse format: className [ORIGIN] [jarSource]
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

    private static void writeOutput(AnalysisReport report, CliArguments args) throws IOException {
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

        Writer output;
        if (args.getOutputFile() != null) {
            output = new BufferedWriter(new FileWriter(args.getOutputFile()));
        } else {
            output = new BufferedWriter(new OutputStreamWriter(System.out));
        }

        try {
            writer.write(report, output);
        } finally {
            if (args.getOutputFile() != null) {
                output.close();
            }
        }
    }
}
