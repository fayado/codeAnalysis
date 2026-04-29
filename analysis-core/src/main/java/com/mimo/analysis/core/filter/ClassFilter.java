package com.mimo.analysis.core.filter;

public interface ClassFilter {

    boolean shouldInclude(String className);

    boolean shouldExclude(String className);

    boolean shouldExcludeJar(String jarName);
}
