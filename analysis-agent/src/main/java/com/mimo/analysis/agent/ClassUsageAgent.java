package com.mimo.analysis.agent;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.filter.DefaultClassFilter;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

public class ClassUsageAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[ClassUsageAgent] Agent starting...");
        initialize(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[ClassUsageAgent] Agent attached (agentmain)...");
        initialize(agentArgs, inst);
    }

    private static void initialize(String agentArgs, Instrumentation inst) {
        String outputFile = "class-usage-agent-output.txt";
        List<String> additionalExclusions = new ArrayList<>();
        List<String> additionalInclusions = new ArrayList<>();

        // Parse agent arguments: key=value pairs separated by commas
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

        ClassFilter filter = new DefaultClassFilter(additionalExclusions, additionalInclusions);
        AgentDataCollector.initialize(outputFile, filter);

        // Register transformer
        inst.addTransformer(new ClassUsageTransformer(), true);

        // Register shutdown hook to save results
        Runtime.getRuntime().addShutdownHook(new Thread(AgentDataCollector::saveResults));

        System.out.println("[ClassUsageAgent] Agent initialized. Output: " + outputFile);
    }
}
