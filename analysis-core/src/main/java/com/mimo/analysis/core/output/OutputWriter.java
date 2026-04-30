package com.mimo.analysis.core.output;

import com.mimo.analysis.core.model.AnalysisReport;

import java.io.IOException;
import java.io.Writer;

/**
 * 输出写入器接口，定义分析报告的输出格式。
 *
 * 支持多种输出格式实现：
 * - {@link TextOutputWriter}：纯文本格式，简洁易读
 * - {@link JsonOutputWriter}：JSON 格式，结构化数据，便于程序解析
 * - {@link DotGraphOutputWriter}：Graphviz DOT 格式，可生成可视化依赖关系图
 *
 * 实现类应确保在写入完成后刷新输出流。
 */
public interface OutputWriter {

    /**
     * 将分析报告写入到指定的输出流中。
     *
     * @param report 分析报告实例
     * @param output 输出目标，调用方负责关闭该 Writer
     * @throws IOException 写入过程中发生 I/O 错误时抛出
     */
    void write(AnalysisReport report, Writer output) throws IOException;
}
