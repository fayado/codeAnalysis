package com.mimo.analysis.core.model;

public class ClassInfo {

    private final String className;
    private final String packageName;
    private final String simpleName;
    private final String jarSource;
    private final ClassOrigin origin;
    private final boolean isInterface;
    private final boolean isAbstract;
    private final boolean isAnnotation;
    private final boolean isEnum;
    private final boolean isSpringComponent;

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

    public String getClassName() { return className; }
    public String getPackageName() { return packageName; }
    public String getSimpleName() { return simpleName; }
    public String getJarSource() { return jarSource; }
    public ClassOrigin getOrigin() { return origin; }
    public boolean isInterface() { return isInterface; }
    public boolean isAbstract() { return isAbstract; }
    public boolean isAnnotation() { return isAnnotation; }
    public boolean isEnum() { return isEnum; }
    public boolean isSpringComponent() { return isSpringComponent; }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassInfo classInfo = (ClassInfo) o;
        return className.equals(classInfo.className);
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public String toString() {
        return className + " [" + origin + "]";
    }

    public static class Builder {
        private final String className;
        private String packageName;
        private String simpleName;
        private String jarSource = "";
        private ClassOrigin origin = ClassOrigin.STATIC;
        private boolean isInterface;
        private boolean isAbstract;
        private boolean isAnnotation;
        private boolean isEnum;
        private boolean isSpringComponent;

        public Builder(String className) {
            this.className = className;
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                this.packageName = className.substring(0, lastDot);
                this.simpleName = className.substring(lastDot + 1);
            } else {
                this.packageName = "(default)";
                this.simpleName = className;
            }
        }

        public Builder packageName(String packageName) { this.packageName = packageName; return this; }
        public Builder simpleName(String simpleName) { this.simpleName = simpleName; return this; }
        public Builder jarSource(String jarSource) { this.jarSource = jarSource; return this; }
        public Builder origin(ClassOrigin origin) { this.origin = origin; return this; }
        public Builder isInterface(boolean isInterface) { this.isInterface = isInterface; return this; }
        public Builder isAbstract(boolean isAbstract) { this.isAbstract = isAbstract; return this; }
        public Builder isAnnotation(boolean isAnnotation) { this.isAnnotation = isAnnotation; return this; }
        public Builder isEnum(boolean isEnum) { this.isEnum = isEnum; return this; }
        public Builder isSpringComponent(boolean isSpringComponent) { this.isSpringComponent = isSpringComponent; return this; }

        public ClassInfo build() {
            return new ClassInfo(this);
        }
    }
}
