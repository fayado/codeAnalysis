package com.mimo.analysis.core.filter;

/**
 * 类过滤器接口，定义类和 JAR 文件的过滤规则。
 *
 * 在分析过程中，需要排除 JDK 核心类、Tomcat 容器类等不属于项目业务代码的类，
 * 以确保分析结果只包含真正有意义的类依赖关系。
 * 该接口为过滤策略提供统一抽象，支持自定义实现替换默认过滤逻辑。
 *
 * @see DefaultClassFilter 默认实现
 */
public interface ClassFilter {

    /**
     * 判断指定类是否应该被包含在分析结果中。
     *
     * @param className 类的全限定名（使用 '.' 分隔）或内部名（使用 '/' 分隔）
     * @return 如果该类应该被包含则返回 true，否则返回 false
     */
    boolean shouldInclude(String className);

    /**
     * 判断指定类是否应该被排除在分析结果之外。
     * 与 {@link #shouldInclude(String)} 逻辑相反。
     *
     * @param className 类的全限定名或内部名
     * @return 如果该类应该被排除则返回 true，否则返回 false
     */
    boolean shouldExclude(String className);

    /**
     * 判断指定 JAR 文件是否应该被整体排除在分析范围之外。
     * 排除整个 JAR 可以避免分析 JDK 核心库、Tomcat 容器库等无关 JAR，
     * 提升分析效率并减少噪音数据。
     *
     * @param jarName JAR 文件名（不含路径）
     * @return 如果该 JAR 应该被排除则返回 true，否则返回 false
     */
    boolean shouldExcludeJar(String jarName);
}
