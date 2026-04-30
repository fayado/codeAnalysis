package com.mimo.analysis.core.util;

/**
 * 类名工具类，提供类名格式转换和解析的静态方法。
 *
 * 在字节码分析过程中，ASM 使用 JVM 内部格式（以 '/' 分隔，如 "com/example/User"），
 * 而 Java 代码通常使用标准格式（以 '.' 分隔，如 "com.example.User"）。
 * 该工具类负责在两种格式之间转换，以及从类名中提取包名和简单名。
 *
 * 所有方法均为静态方法，不允许实例化。
 */
public final class ClassNameUtils {

    /** 私有构造函数，防止实例化工具类 */
    private ClassNameUtils() {}

    /**
     * 将 JVM 内部格式的类名转换为标准点分隔格式。
     *
     * JVM 内部使用 '/' 作为包名分隔符，例如 "com/example/service/UserService"。
     * Java 语言使用 '.' 作为包名分隔符，例如 "com.example.service.UserService"。
     *
     * @param internalName JVM 内部格式的类名（使用 '/' 分隔）
     * @return 标准点分隔格式的类名（使用 '.' 分隔）
     */
    public static String toDotNotation(String internalName) {
        return internalName.replace('/', '.');
    }

    /**
     * 从全限定类名中提取包名。
     *
     * 例如：从 "com.example.service.UserService" 中提取 "com.example.service"。
     * 如果类名中没有 '.'（即属于默认包），则返回 "(default)"。
     *
     * @param className 全限定类名
     * @return 包名，如果属于默认包则返回 "(default)"
     */
    public static String extractPackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "(default)";
    }

    /**
     * 从全限定类名中提取简单名（不含包名）。
     *
     * 例如：从 "com.example.service.UserService" 中提取 "UserService"。
     * 如果类名中没有 '.'，则直接返回原类名。
     *
     * @param className 全限定类名
     * @return 类的简单名
     */
    public static String extractSimpleName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }

    /**
     * 将 JVM 类型描述符转换为标准全限定类名。
     *
     * JVM 类型描述符是一种紧凑的类型表示格式，例如：
     * - 对象类型：Ljava/lang/String;
     * - 数组类型：[Ljava/lang/String;（一维数组）、[[Ljava/lang/String;（二维数组）
     * - 基本类型：I（int）、Z（boolean）等
     *
     * 该方法会：
     * 1. 去除数组维度前缀（'[' 字符）
     * 2. 去除对象类型标记（'L' 前缀和 ';' 后缀）
     * 3. 将 '/' 分隔符转换为 '.' 分隔符
     *
     * @param descriptor JVM 类型描述符
     * @return 标准全限定类名
     */
    public static String normalizeDescriptor(String descriptor) {
        String result = descriptor;
        // 去除数组维度前缀
        while (result.startsWith("[")) result = result.substring(1);
        // 去除对象类型标记
        if (result.startsWith("L") && result.endsWith(";")) {
            result = result.substring(1, result.length() - 1);
        }
        // 转换为点分隔格式
        return toDotNotation(result);
    }
}
