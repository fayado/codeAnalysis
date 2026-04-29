package com.mimo.analysis.core.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class DefaultClassFilter implements ClassFilter {

    private static final List<String> EXCLUDED_PREFIXES = Arrays.asList(
            "java.",
            "javax.",
            "sun.",
            "com.sun.",
            "jdk.",
            "netscape.",
            "org.xml.sax.",
            "org.w3c.dom.",
            "org.ietf.jgss.",
            "org.apache.catalina.",
            "org.apache.tomcat.",
            "org.apache.coyote.",
            "org.apache.el.",
            "org.apache.jasper.",
            "org.apache.naming.",
            "org.apache.juli."
    );

    private static final List<String> EXCLUDED_SUFFIXES = Arrays.asList(
            "$$EnhancerBySpringCGLIB$$",
            "$$FastClassBySpringCGLIB$$",
            "$HibernateProxy$",
            "_$$_jvst"
    );

    private static final List<Pattern> EXCLUDED_JAR_PATTERNS = Arrays.asList(
            Pattern.compile("rt\\.jar"),
            Pattern.compile("tools\\.jar"),
            Pattern.compile("jce\\.jar"),
            Pattern.compile("charsets\\.jar"),
            Pattern.compile("jfr\\.jar"),
            Pattern.compile("tomcat-embed-.*\\.jar"),
            Pattern.compile("catalina-.*\\.jar"),
            Pattern.compile("coyote-.*\\.jar"),
            Pattern.compile("jasper-.*\\.jar"),
            Pattern.compile("el-api-.*\\.jar"),
            Pattern.compile("jsp-api-.*\\.jar"),
            Pattern.compile("servlet-api-.*\\.jar"),
            Pattern.compile("websocket-.*\\.jar")
    );

    private final List<String> additionalExclusions;
    private final List<String> additionalInclusions;

    public DefaultClassFilter() {
        this.additionalExclusions = Collections.emptyList();
        this.additionalInclusions = Collections.emptyList();
    }

    public DefaultClassFilter(List<String> additionalExclusions, List<String> additionalInclusions) {
        this.additionalExclusions = additionalExclusions != null ? additionalExclusions : Collections.emptyList();
        this.additionalInclusions = additionalInclusions != null ? additionalInclusions : Collections.emptyList();
    }

    @Override
    public boolean shouldInclude(String className) {
        return !shouldExclude(className);
    }

    @Override
    public boolean shouldExclude(String className) {
        if (className == null || className.isEmpty()) return true;

        String normalized = className.replace('/', '.');

        for (String inc : additionalInclusions) {
            if (normalized.startsWith(inc)) return false;
        }

        for (String suffix : EXCLUDED_SUFFIXES) {
            if (normalized.contains(suffix)) return true;
        }

        for (String prefix : EXCLUDED_PREFIXES) {
            if (normalized.startsWith(prefix)) return true;
        }

        for (String exc : additionalExclusions) {
            if (normalized.startsWith(exc)) return true;
        }

        return false;
    }

    @Override
    public boolean shouldExcludeJar(String jarName) {
        if (jarName == null) return true;
        for (Pattern pattern : EXCLUDED_JAR_PATTERNS) {
            if (pattern.matcher(jarName).matches()) return true;
        }
        return false;
    }
}
