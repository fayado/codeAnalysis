package com.mimo.analysis.cli;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.model.ClassInfo;
import com.mimo.analysis.core.model.ClassOrigin;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class ResultCombiner {

    public static AnalysisReport combine(AnalysisReport staticReport, File agentDataFile,
                                          ClassFilter filter) throws IOException {
        Set<String> agentClasses = parseAgentOutput(agentDataFile);
        AnalysisReport combined = new AnalysisReport("combined");

        // Process static classes
        for (ClassInfo staticClass : staticReport.getAllClasses()) {
            String name = staticClass.getClassName();
            if (agentClasses.contains(name)) {
                ClassInfo merged = staticClass.toBuilder()
                        .origin(ClassOrigin.BOTH)
                        .build();
                combined.addClass(merged);
                agentClasses.remove(name);
            } else {
                combined.addClass(staticClass);
            }
        }

        // Process remaining agent-only classes
        for (String agentClassName : agentClasses) {
            if (filter.shouldInclude(agentClassName)) {
                ClassInfo agentOnly = new ClassInfo.Builder(agentClassName)
                        .jarSource("(agent-detected)")
                        .origin(ClassOrigin.AGENT)
                        .build();
                combined.addClass(agentOnly);
            }
        }

        // Copy all references from static report
        combined.addReferences(staticReport.getAllReferences());

        return combined;
    }

    private static Set<String> parseAgentOutput(File agentFile) throws IOException {
        Set<String> classes = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(agentFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Handle possible JSON format
                line = line.replace("\"", "").replace(",", "").trim();
                if (!line.isEmpty() && !line.equals("classes") && !line.equals("]")
                        && !line.equals("{") && !line.equals("}")) {
                    classes.add(line);
                }
            }
        }
        return classes;
    }
}
