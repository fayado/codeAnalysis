package com.mimo.analysis.core.output;

import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.model.ClassInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 纯文本格式输出器，将分析报告以简洁的文本格式输出。
 *
 * 输出格式示例：
 * <pre>
 * # Class Usage Analysis Report
 * # Generated: 2024-01-01T00:00:00Z
 * # Mode: combine
 * # Total classes: 150
 * # Total references: 320
 * #
 * # Format: <class-name> [<origin>] [<jar-source>]
 * com.example.service.UserService [BOTH] [app.jar]
 * com.example.model.User [STATIC] [app.jar]
 * </pre>
 *
 * 输出特点：
 * - 以 '#' 开头的行为报告头信息（生成时间、分析模式、统计数据等）
 * - 每行一个类，格式为：类全限定名 [来源] [JAR来源]
 * - 类按全限定名字母序排列
 */
public class TextOutputWriter implements OutputWriter {

    /**
     * 将分析报告以纯文本格式写入输出流。
     *
     * @param report 分析报告
     * @param output 输出目标
     * @throws IOException 写入错误时抛出
     */
    @Override
    public void write(AnalysisReport report, Writer output) throws IOException {
        PrintWriter pw = new PrintWriter(output);

        // 输出报告头信息
        pw.println("# Class Usage Analysis Report");
        pw.println("# Generated: " + report.getGeneratedAt());
        pw.println("# Mode: " + report.getAnalysisMode());
        pw.println("# Total classes: " + report.getTotalClassCount());
        pw.println("# Total references: " + report.getTotalReferenceCount());
        pw.println("#");
        pw.println("# Format: <class-name> [<origin>] [<jar-source>]");

        // 按类名排序后输出所有类信息
        List<ClassInfo> sorted = new ArrayList<>(report.getAllClasses());
        sorted.sort(Comparator.comparing(ClassInfo::getClassName));

        for (ClassInfo info : sorted) {
            pw.println(info.getClassName() + " [" + info.getOrigin() + "] [" + info.getJarSource() + "]");
        }

        pw.flush();
    }
}
