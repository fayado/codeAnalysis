package com.mimo.analysis.core.util;

public final class ClassNameUtils {

    private ClassNameUtils() {}

    public static String toDotNotation(String internalName) {
        return internalName.replace('/', '.');
    }

    public static String extractPackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "(default)";
    }

    public static String extractSimpleName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }

    public static String normalizeDescriptor(String descriptor) {
        String result = descriptor;
        while (result.startsWith("[")) result = result.substring(1);
        if (result.startsWith("L") && result.endsWith(";")) {
            result = result.substring(1, result.length() - 1);
        }
        return toDotNotation(result);
    }
}
