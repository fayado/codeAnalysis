package com.mimo.analysis.core.output;

import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.model.ClassInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TextOutputWriter implements OutputWriter {

    @Override
    public void write(AnalysisReport report, Writer output) throws IOException {
        PrintWriter pw = new PrintWriter(output);

        pw.println("# Class Usage Analysis Report");
        pw.println("# Generated: " + report.getGeneratedAt());
        pw.println("# Mode: " + report.getAnalysisMode());
        pw.println("# Total classes: " + report.getTotalClassCount());
        pw.println("# Total references: " + report.getTotalReferenceCount());
        pw.println("#");
        pw.println("# Format: <class-name> [<origin>] [<jar-source>]");

        List<ClassInfo> sorted = new ArrayList<>(report.getAllClasses());
        sorted.sort(Comparator.comparing(ClassInfo::getClassName));

        for (ClassInfo info : sorted) {
            pw.println(info.getClassName() + " [" + info.getOrigin() + "] [" + info.getJarSource() + "]");
        }

        pw.flush();
    }
}
