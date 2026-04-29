package com.mimo.analysis.core.output;

import com.mimo.analysis.core.model.AnalysisReport;

import java.io.IOException;
import java.io.Writer;

public interface OutputWriter {

    void write(AnalysisReport report, Writer output) throws IOException;
}
