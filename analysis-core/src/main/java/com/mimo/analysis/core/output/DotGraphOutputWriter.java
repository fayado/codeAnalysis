package com.mimo.analysis.core.output;

import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.model.ClassReference;
import com.mimo.analysis.core.model.ClassInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class DotGraphOutputWriter implements OutputWriter {

    private final int maxNodes;
    private final boolean clusterByPackage;

    public DotGraphOutputWriter(int maxNodes, boolean clusterByPackage) {
        this.maxNodes = maxNodes;
        this.clusterByPackage = clusterByPackage;
    }

    @Override
    public void write(AnalysisReport report, Writer output) throws IOException {
        PrintWriter pw = new PrintWriter(output);

        // Select top nodes by reference count
        Map<String, Integer> nodeWeights = new HashMap<>();
        for (ClassReference ref : report.getAllReferences()) {
            nodeWeights.merge(ref.getFromClass(), 1, Integer::sum);
            nodeWeights.merge(ref.getToClass(), 1, Integer::sum);
        }

        List<String> topNodes = nodeWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxNodes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Set<String> topNodeSet = new HashSet<>(topNodes);

        pw.println("digraph ClassDependencies {");
        pw.println("    rankdir=LR;");
        pw.println("    node [shape=box, style=filled, fillcolor=lightblue, fontname=\"Helvetica\", fontsize=10];");
        pw.println("    edge [color=gray, arrowsize=0.8];");
        pw.println();

        if (clusterByPackage) {
            // Group nodes by package
            Map<String, List<String>> packageGroups = new LinkedHashMap<>();
            for (String node : topNodes) {
                ClassInfo info = report.getClass(node);
                String pkg = info != null ? info.getPackageName() : "(unknown)";
                packageGroups.computeIfAbsent(pkg, k -> new ArrayList<>()).add(node);
            }

            int clusterId = 0;
            for (Map.Entry<String, List<String>> entry : packageGroups.entrySet()) {
                pw.println("    subgraph cluster_" + clusterId + " {");
                pw.println("        label=\"" + entry.getKey() + "\";");
                pw.println("        style=dashed;");
                pw.println("        color=gray;");
                for (String node : entry.getValue()) {
                    pw.println("        \"" + node + "\" [label=\"" + shortenName(node) + "\"];");
                }
                pw.println("    }");
                clusterId++;
            }
        } else {
            for (String node : topNodes) {
                pw.println("    \"" + node + "\" [label=\"" + shortenName(node) + "\"];");
            }
        }

        pw.println();

        // Edges between top nodes
        Set<String> addedEdges = new HashSet<>();
        for (ClassReference ref : report.getAllReferences()) {
            if (topNodeSet.contains(ref.getFromClass()) && topNodeSet.contains(ref.getToClass())) {
                String edgeKey = ref.getFromClass() + "->" + ref.getToClass();
                if (addedEdges.add(edgeKey)) {
                    pw.println("    \"" + ref.getFromClass() + "\" -> \"" + ref.getToClass()
                            + "\" [label=\"" + ref.getType().name() + "\"];");
                }
            }
        }

        pw.println("}");
        pw.flush();
    }

    private String shortenName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }
}
