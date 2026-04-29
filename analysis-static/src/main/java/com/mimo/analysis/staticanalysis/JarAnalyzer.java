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

public class JarAnalyzer {

    private final ClassFilter filter;

    public JarAnalyzer(ClassFilter filter) {
        this.filter = filter;
    }

    public AnalysisReport analyzeJar(File jarFile) throws IOException {
        AnalysisReport report = new AnalysisReport("static");
        String jarName = jarFile.getName();

        if (filter.shouldExcludeJar(jarName)) {
            return report;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            // Check if this is a Spring Boot fat JAR
            boolean isSpringBootJar = false;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("BOOT-INF/")) {
                    isSpringBootJar = true;
                    break;
                }
            }

            if (isSpringBootJar) {
                return analyzeSpringBootJar(jar);
            }

            // Regular JAR
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

    private AnalysisReport analyzeSpringBootJar(JarFile jar) throws IOException {
        AnalysisReport report = new AnalysisReport("static");

        // Analyze classes from BOOT-INF/classes/
        // Analyze JARs from BOOT-INF/lib/
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

        // Analyze application classes
        for (JarEntry entry : classEntries) {
            try (InputStream is = jar.getInputStream(entry)) {
                analyzeClass(is, jar.getName(), report);
            }
        }

        // Analyze library JARs
        for (JarEntry entry : libJarEntries) {
            String libJarName = entry.getName();
            String fileName = libJarName.substring(libJarName.lastIndexOf('/') + 1);

            if (filter.shouldExcludeJar(fileName)) {
                continue;
            }

            File tempJar = extractToTempFile(jar, entry);
            try {
                AnalysisReport libReport = analyzeJar(tempJar);
                report.merge(libReport);
            } finally {
                tempJar.delete();
            }
        }

        return report;
    }

    public AnalysisReport analyzeWar(File warFile) throws IOException {
        AnalysisReport report = new AnalysisReport("static");

        try (JarFile war = new JarFile(warFile)) {
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

            for (JarEntry entry : classEntries) {
                try (InputStream is = war.getInputStream(entry)) {
                    analyzeClass(is, warFile.getName(), report);
                }
            }

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

    public AnalysisReport analyzeDirectory(File directory) throws IOException {
        AnalysisReport report = new AnalysisReport("static");

        File[] files = directory.listFiles();
        if (files == null) return report;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                AnalysisReport jarReport = analyzeJar(file);
                report.merge(jarReport);
            } else if (file.isDirectory()) {
                AnalysisReport dirReport = analyzeDirectory(file);
                report.merge(dirReport);
            }
        }

        return report;
    }

    public void analyzeClass(InputStream classStream, String sourceName, AnalysisReport report) throws IOException {
        ClassReader reader = new ClassReader(classStream);
        ClassReferenceVisitor visitor = new ClassReferenceVisitor(filter, sourceName);

        reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        ClassInfo classInfo = visitor.getClassInfo();
        if (classInfo != null && filter.shouldInclude(classInfo.getClassName())) {
            report.addClass(classInfo);
            report.addReferences(visitor.getReferences());
        }
    }

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
