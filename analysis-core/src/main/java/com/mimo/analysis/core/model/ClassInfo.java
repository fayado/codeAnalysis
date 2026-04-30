package com.mimo.analysis.core.model;

/**
 * 类信息模型，记录单个 Java 类的详细元数据。
 *
 * 包含类的全限定名、包名、简单名、来源 JAR、发现来源（Agent/静态/两者），
 * 以及类的类型特征（是否为接口、抽象类、注解、枚举、Spring 组件等）。
 * 使用 Builder 模式构建，确保对象创建的灵活性和不可变性。
 *
 * 示例用法：
 * <pre>
 * ClassInfo info = new ClassInfo.Builder("com.example.UserService")
 *         .origin(ClassOrigin.BOTH)
 *         .isInterface(false)
 *         .isSpringComponent(true)
 *         .build();
 * </pre>
 */
public class ClassInfo {

    /** 类的全限定名，例如：com.example.service.UserService */
    private final String className;

    /** 类所在的包名，例如：com.example.service */
    private final String packageName;

    /** 类的简单名（不含包名），例如：UserService */
    private final String simpleName;

    /** 类所在的 JAR 文件来源，用于追溯类的出处 */
    private final String jarSource;

    /** 类的发现来源：Agent 运行时采集、静态分析或两者兼有 */
    private final ClassOrigin origin;

    /** 是否为接口 */
    private final boolean isInterface;

    /** 是否为抽象类 */
    private final boolean isAbstract;

    /** 是否为注解类型 */
    private final boolean isAnnotation;

    /** 是否为枚举类型 */
    private final boolean isEnum;

    /** 是否为 Spring 组件（如 @Service、@Controller、@Component 等） */
    private final boolean isSpringComponent;

    /**
     * 私有构造函数，通过 Builder 模式创建实例。
     *
     * @param builder 构建器实例，包含所有属性值
     */
    private ClassInfo(Builder builder) {
        this.className = builder.className;
        this.packageName = builder.packageName;
        this.simpleName = builder.simpleName;
        this.jarSource = builder.jarSource;
        this.origin = builder.origin;
        this.isInterface = builder.isInterface;
        this.isAbstract = builder.isAbstract;
        this.isAnnotation = builder.isAnnotation;
        this.isEnum = builder.isEnum;
        this.isSpringComponent = builder.isSpringComponent;
    }

    /** 获取类的全限定名 */
    public String getClassName() { return className; }

    /** 获取类所在的包名 */
    public String getPackageName() { return packageName; }

    /** 获取类的简单名（不含包名） */
    public String getSimpleName() { return simpleName; }

    /** 获取类所在的 JAR 文件来源 */
    public String getJarSource() { return jarSource; }

    /** 获取类的发现来源（Agent/静态/两者） */
    public ClassOrigin getOrigin() { return origin; }

    /** 判断是否为接口 */
    public boolean isInterface() { return isInterface; }

    /** 判断是否为抽象类 */
    public boolean isAbstract() { return isAbstract; }

    /** 判断是否为注解类型 */
    public boolean isAnnotation() { return isAnnotation; }

    /** 判断是否为枚举类型 */
    public boolean isEnum() { return isEnum; }

    /** 判断是否为 Spring 组件 */
    public boolean isSpringComponent() { return isSpringComponent; }

    /**
     * 基于当前对象创建一个新的 Builder，用于修改部分属性后重建对象。
     * 常用于合并数据时修改来源为 {@link ClassOrigin#BOTH}。
     *
     * @return 包含当前所有属性值的新 Builder 实例
     */
    public Builder toBuilder() {
        return new Builder(className)
                .packageName(packageName)
                .simpleName(simpleName)
                .jarSource(jarSource)
                .origin(origin)
                .isInterface(isInterface)
                .isAbstract(isAbstract)
                .isAnnotation(isAnnotation)
                .isEnum(isEnum)
                .isSpringComponent(isSpringComponent);
    }

    /**
     * 相等性判断，基于类的全限定名。
     * 同一个类即使来源不同，只要全限定名相同即视为相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassInfo classInfo = (ClassInfo) o;
        return className.equals(classInfo.className);
    }

    /** 哈希码计算，基于类的全限定名，与 equals 保持一致 */
    @Override
    public int hashCode() {
        return className.hashCode();
    }

    /** 字符串表示，格式为：全限定名 [来源] */
    @Override
    public String toString() {
        return className + " [" + origin + "]";
    }

    /**
     * ClassInfo 的构建器，采用 Builder 模式创建不可变的 ClassInfo 实例。
     *
     * 构造时只需提供类的全限定名，包名和简单名会自动从全限定名中解析。
     * 其他属性可通过链式调用设置，默认值为：
     * - jarSource: 空字符串
     * - origin: STATIC（静态分析）
     * - 各类型标志（isInterface 等）: false
     */
    public static class Builder {
        /** 类的全限定名（必填） */
        private final String className;

        /** 类所在的包名，从全限定名自动解析 */
        private String packageName;

        /** 类的简单名，从全限定名自动解析 */
        private String simpleName;

        /** 类所在的 JAR 文件来源，默认为空字符串 */
        private String jarSource = "";

        /** 类的发现来源，默认为静态分析 */
        private ClassOrigin origin = ClassOrigin.STATIC;

        /** 是否为接口，默认为 false */
        private boolean isInterface;

        /** 是否为抽象类，默认为 false */
        private boolean isAbstract;

        /** 是否为注解类型，默认为 false */
        private boolean isAnnotation;

        /** 是否为枚举类型，默认为 false */
        private boolean isEnum;

        /** 是否为 Spring 组件，默认为 false */
        private boolean isSpringComponent;

        /**
         * 构造 Builder 实例，自动从全限定名中解析包名和简单名。
         *
         * @param className 类的全限定名，例如：com.example.UserService
         */
        public Builder(String className) {
            this.className = className;
            // 从全限定名中解析包名和简单名
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                this.packageName = className.substring(0, lastDot);
                this.simpleName = className.substring(lastDot + 1);
            } else {
                // 没有包名的情况（默认包）
                this.packageName = "(default)";
                this.simpleName = className;
            }
        }

        /** 设置包名 */
        public Builder packageName(String packageName) { this.packageName = packageName; return this; }

        /** 设置简单名 */
        public Builder simpleName(String simpleName) { this.simpleName = simpleName; return this; }

        /** 设置 JAR 文件来源 */
        public Builder jarSource(String jarSource) { this.jarSource = jarSource; return this; }

        /** 设置发现来源 */
        public Builder origin(ClassOrigin origin) { this.origin = origin; return this; }

        /** 设置是否为接口 */
        public Builder isInterface(boolean isInterface) { this.isInterface = isInterface; return this; }

        /** 设置是否为抽象类 */
        public Builder isAbstract(boolean isAbstract) { this.isAbstract = isAbstract; return this; }

        /** 设置是否为注解类型 */
        public Builder isAnnotation(boolean isAnnotation) { this.isAnnotation = isAnnotation; return this; }

        /** 设置是否为枚举类型 */
        public Builder isEnum(boolean isEnum) { this.isEnum = isEnum; return this; }

        /** 设置是否为 Spring 组件 */
        public Builder isSpringComponent(boolean isSpringComponent) { this.isSpringComponent = isSpringComponent; return this; }

        /**
         * 构建 ClassInfo 实例。
         *
         * @return 根据当前 Builder 属性创建的 ClassInfo 对象
         */
        public ClassInfo build() {
            return new ClassInfo(this);
        }
    }
}
