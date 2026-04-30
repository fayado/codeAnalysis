package com.mimo.analysis.staticanalysis;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.model.AnalysisReport;
import com.mimo.analysis.core.model.ClassInfo;
import com.mimo.analysis.core.model.ClassReference;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;

/**
 * JAR 文件分析器，负责分析 JAR、WAR 和 Spring Boot fat JAR 中的类依赖关系。
 *
 * 该类是静态字节码分析的入口，支持以下格式的归档文件：
 * - 普通 JAR：直接分析其中的 .class 文件
 * - WAR 文件：分析 WEB-INF/classes/ 下的类和 WEB-INF/lib/ 下的依赖 JAR
 * - Spring Boot fat JAR：分析 BOOT-INF/classes/ 下的应用类和 BOOT-INF/lib/ 下的嵌套 JAR
 *
 * 对于嵌套的 JAR（如 Spring Boot fat JAR 中的 lib JAR），
 * 会先解压到临时文件再分析，分析完成后自动清理临时文件。
 *
 * 使用方式：
 * <pre>
 * ClassFilter filter = new DefaultClassFilter();
 * JarAnalyzer analyzer = new JarAnalyzer(filter);
 * AnalysisReport report = analyzer.analyzeJar(new File("app.jar"));
 * </pre>
 */
public class JarAnalyzer {

    /** 类过滤器，用于排除不需要分析的类和 JAR */
    private final ClassFilter filter;

    /**
     * 构造 JAR 分析器实例。
     *
     * @param filter 类过滤器
     */
    public JarAnalyzer(ClassFilter filter) {
        this.filter = filter;
    }

    /**
     * 分析单个 JAR 文件，提取其中所有类的引用关系。
     *
     * 自动检测是否为 Spring Boot fat JAR（通过 BOOT-INF/ 前缀判断），
     * 如果是则委托给 {@link #analyzeSpringBootJar(JarFile)} 处理。
     *
     * @param jarFile JAR 文件
     * @return 分析报告
     * @throws IOException 读取 JAR 文件失败时抛出
     */
    public AnalysisReport analyzeJar(File jarFile) throws IOException {
        AnalysisReport report = new AnalysisReport("static");
        String jarName = jarFile.getName();

        // 检查 JAR 是否被过滤器排除
        if (filter.shouldExcludeJar(jarName)) {
            return report;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            // 检测是否为 Spring Boot fat JAR
            boolean isSpringBootJar = false;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("BOOT-INF/")) {
                    isSpringBootJar = true;
                    break;
                }
            }

            // Spring Boot fat JAR 走专用分析流程
            if (isSpringBootJar) {
                return analyzeSpringBootJar(jar);
            }

            // 普通 JAR：遍历所有 .class 文件并分析
            entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                if (!entry.getName().endsWith(".class")) continue;

                try (InputStream is = jar.getInputStream(entry)) {
                    analyzeClass(is, jarName, report);
                } catch (Exception e) {
                    System.err.println("[WARN] Failed to analyze: " + entry.getName()
                            + " in " + jarName + ": " + e.getMessage());
                }
            }
        }

        return report;
    }

    /**
     * 分析 Spring Boot fat JAR。
     *
     * Spring Boot fat JAR 的结构：
     * - BOOT-INF/classes/：应用自身的类文件
     * - BOOT-INF/lib/：应用依赖的第三方 JAR
     *
     * 先分析应用类，再逐个解压并分析 lib 目录下的依赖 JAR。
     *
     * @param jar Spring Boot fat JAR 文件
     * @return 分析报告
     * @throws IOException 读取 JAR 文件失败时抛出
     */
    private AnalysisReport analyzeSpringBootJar(JarFile jar) throws IOException {
        AnalysisReport report = new AnalysisReport("static");

        // 分离应用类和依赖 JAR
        List<JarEntry> classEntries = new ArrayList<>();
        List<JarEntry> libJarEntries = new ArrayList<>();

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.startsWith("BOOT-INF/classes/") && name.endsWith(".class")) {
                classEntries.add(entry);
            } else if (name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar")) {
                libJarEntries.add(entry);
            }
        }

        // 分析应用自身的类
        for (JarEntry entry : classEntries) {
            try (InputStream is = jar.getInputStream(entry)) {
                analyzeClass(is, jar.getName(), report);
            }
        }

        // 分析依赖 JAR（解压到临时文件后递归分析）
        for (JarEntry entry : libJarEntries) {
            String libJarName = entry.getName();
            String fileName = libJarName.substring(libJarName.lastIndexOf('/') + 1);

            // 检查依赖 JAR 是否被过滤器排除
            if (filter.shouldExcludeJar(fileName)) {
                continue;
            }

            // 解压到临时文件
            File tempJar = extractToTempFile(jar, entry);
            try {
                // 递归分析依赖 JAR
                AnalysisReport libReport = analyzeJar(tempJar);
                report.merge(libReport);
            } finally {
                // 清理临时文件
                tempJar.delete();
            }
        }

        return report;
    }

    /**
     * 分析 WAR 文件，提取其中所有类的引用关系。
     *
     * WAR 文件的标准结构：
     * - WEB-INF/classes/：应用自身的类文件
     * - WEB-INF/lib/：应用依赖的第三方 JAR
     *
     * @param warFile WAR 文件
     * @return 分析报告
     * @throws IOException 读取 WAR 文件失败时抛出
     */
    public AnalysisReport analyzeWar(File warFile) throws IOException {
        AnalysisReport report = new AnalysisReport("static");

        try (JarFile war = new JarFile(warFile)) {
            // 分离应用类和依赖 JAR
            List<JarEntry> classEntries = new ArrayList<>();
            List<JarEntry> libJarEntries = new ArrayList<>();

            Enumeration<JarEntry> entries = war.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("WEB-INF/classes/") && name.endsWith(".class")) {
                    classEntries.add(entry);
                } else if (name.startsWith("WEB-INF/lib/") && name.endsWith(".jar")) {
                    libJarEntries.add(entry);
                }
            }

            // 分析应用自身的类
            for (JarEntry entry : classEntries) {
                try (InputStream is = war.getInputStream(entry)) {
                    analyzeClass(is, warFile.getName(), report);
                }
            }

            // 分析依赖 JAR
            for (JarEntry entry : libJarEntries) {
                String libJarName = entry.getName();
                String fileName = libJarName.substring(libJarName.lastIndexOf('/') + 1);

                if (filter.shouldExcludeJar(fileName)) {
                    continue;
                }

                File tempJar = extractToTempFile(war, entry);
                try {
                    AnalysisReport libReport = analyzeJar(tempJar);
                    report.merge(libReport);
                } finally {
                    tempJar.delete();
                }
            }
        }

        return report;
    }

    /**
     * 递归分析目录下的所有 JAR 文件。
     *
     * 遍历目录中的所有文件和子目录，对 .jar 文件进行分析，
     * 对子目录进行递归遍历。所有分析结果合并到同一个报告中。
     *
     * @param directory 要分析的目录
     * @return 分析报告
     * @throws IOException 读取文件失败时抛出
     */
    public AnalysisReport analyzeDirectory(File directory) throws IOException {
        AnalysisReport report = new AnalysisReport("static");

        File[] files = directory.listFiles();
        if (files == null) return report;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                // 分析 JAR 文件
                AnalysisReport jarReport = analyzeJar(file);
                report.merge(jarReport);
            } else if (file.isDirectory()) {
                // 递归分析子目录
                AnalysisReport dirReport = analyzeDirectory(file);
                report.merge(dirReport);
            }
        }

        return report;
    }

    /**
     * 分析单个 .class 文件的字节码，提取类信息和引用关系。
     *
     * 使用 ASM 的 ClassReader 读取字节码，通过 ClassReferenceVisitor
     * 访问类的结构信息。跳过调试信息和栈帧以提升分析性能。
     *
     * @param classStream .class 文件的输入流
     * @param sourceName  类所在的来源名称（JAR 文件名或目录路径）
     * @param report      分析报告，分析结果将添加到此报告中
     * @throws IOException 读取字节码失败时抛出
     */
    public void analyzeClass(InputStream classStream, String sourceName, AnalysisReport report) throws IOException {
        ClassReader reader = new ClassReader(classStream);
        ClassReferenceVisitor visitor = new ClassReferenceVisitor(filter, sourceName);

        // 跳过调试信息和栈帧以提升性能
        reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // 如果类通过了过滤器，则添加到报告中
        ClassInfo classInfo = visitor.getClassInfo();
        if (classInfo != null && filter.shouldInclude(classInfo.getClassName())) {
            report.addClass(classInfo);
            report.addReferences(visitor.getReferences());
        }
    }

    /**
     * 将 JAR 中的嵌套条目解压到临时文件。
     *
     * 用于处理 Spring Boot fat JAR 和 WAR 中嵌套的第三方 JAR。
     * 临时文件在 JVM 退出时自动删除。
     *
     * @param jar   父级 JAR 文件
     * @param entry 要解压的 JAR 条目
     * @return 解压后的临时文件
     * @throws IOException 解压失败时抛出
     */
    private File extractToTempFile(JarFile jar, JarEntry entry) throws IOException {
        File tempFile = File.createTempFile("analysis-", ".jar");
        tempFile.deleteOnExit();
        try (InputStream is = jar.getInputStream(entry);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }
}
