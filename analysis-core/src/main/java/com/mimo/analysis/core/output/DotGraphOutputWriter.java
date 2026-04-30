package com.mimo.analysis.core.output;

import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.model.ClassReference;
import com.mimo.analysis.core.model.ClassInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Graphviz DOT 格式输出器，生成类依赖关系的可视化图描述文件。
 *
 * 输出的 DOT 文件可以使用 Graphviz 工具（如 dot、neato）或在线工具
 * （如 dreampuf.github.io/GraphvizOnline）渲染为 PNG、SVG 等图片格式。
 *
 * 功能特性：
 * - 根据引用次数自动筛选 Top N 节点，避免图表过于复杂
 * - 支持按包名分组显示（clusterByPackage 选项），生成子图聚类
 * - 节点使用简单类名（不含包名）作为标签，保持图表简洁
 * - 边上标注引用类型（EXTENDS、FIELD_TYPE 等）
 * - 自动去除重复边（同一对类之间的多条引用合并为一条）
 */
public class DotGraphOutputWriter implements OutputWriter {

    /** 图中显示的最大节点数 */
    private final int maxNodes;

    /** 是否按包名将节点分组到子图中 */
    private final boolean clusterByPackage;

    /**
     * 构造 DOT 图输出器。
     *
     * @param maxNodes         图中显示的最大节点数，引用次数最多的 Top N 节点会被选中
     * @param clusterByPackage 是否按包名将节点分组到子图聚类中
     */
    public DotGraphOutputWriter(int maxNodes, boolean clusterByPackage) {
        this.maxNodes = maxNodes;
        this.clusterByPackage = clusterByPackage;
    }

    /**
     * 将分析报告以 DOT 图格式写入输出流。
     *
     * 处理流程：
     * 1. 统计每个节点（类）的引用权重（入引用 + 出引用次数）
     * 2. 选取权重最高的 Top N 节点
     * 3. 输出 DOT 图头和全局样式配置
     * 4. 按是否启用聚类分组输出节点定义
     * 5. 输出 Top N 节点之间的引用边（去重）
     *
     * @param report 分析报告
     * @param output 输出目标
     * @throws IOException 写入错误时抛出
     */
    @Override
    public void write(AnalysisReport report, Writer output) throws IOException {
        PrintWriter pw = new PrintWriter(output);

        // 统计每个节点的引用权重（入引用 + 出引用次数之和）
        Map<String, Integer> nodeWeights = new HashMap<>();
        for (ClassReference ref : report.getAllReferences()) {
            nodeWeights.merge(ref.getFromClass(), 1, Integer::sum);
            nodeWeights.merge(ref.getToClass(), 1, Integer::sum);
        }

        // 选取权重最高的 Top N 节点
        List<String> topNodes = nodeWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxNodes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Set<String> topNodeSet = new HashSet<>(topNodes);

        // 输出 DOT 图头和全局样式
        pw.println("digraph ClassDependencies {");
        pw.println("    rankdir=LR;");  // 从左到右布局
        pw.println("    node [shape=box, style=filled, fillcolor=lightblue, fontname=\"Helvetica\", fontsize=10];");
        pw.println("    edge [color=gray, arrowsize=0.8];");
        pw.println();

        if (clusterByPackage) {
            // 按包名分组，生成子图聚类
            Map<String, List<String>> packageGroups = new LinkedHashMap<>();
            for (String node : topNodes) {
                ClassInfo info = report.getClass(node);
                String pkg = info != null ? info.getPackageName() : "(unknown)";
                packageGroups.computeIfAbsent(pkg, k -> new ArrayList<>()).add(node);
            }

            // 输出每个包的子图聚类
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
            // 不分组，直接输出所有节点
            for (String node : topNodes) {
                pw.println("    \"" + node + "\" [label=\"" + shortenName(node) + "\"];");
            }
        }

        pw.println();

        // 输出 Top N 节点之间的引用边（去重）
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

    /**
     * 将全限定类名缩短为简单名（不含包名）。
     * 用于节点标签，保持图表简洁。
     *
     * @param className 全限定类名
     * @return 简单类名
     */
    private String shortenName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }
}
