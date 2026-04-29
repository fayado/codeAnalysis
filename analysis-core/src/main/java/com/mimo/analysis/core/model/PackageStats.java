package com.mimo.analysis.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackageStats {

    private final String packageName;
    private int classCount;
    private int interfaceCount;
    private int abstractClassCount;
    private int annotationCount;
    private int enumCount;
    private int springComponentCount;
    private final List<String> classNames = new ArrayList<>();

    public PackageStats(String packageName) {
        this.packageName = packageName;
    }

    public void addClass(ClassInfo info) {
        classNames.add(info.getClassName());
        classCount++;
        if (info.isInterface()) interfaceCount++;
        if (info.isAbstract()) abstractClassCount++;
        if (info.isAnnotation()) annotationCount++;
        if (info.isEnum()) enumCount++;
        if (info.isSpringComponent()) springComponentCount++;
    }

    public String getPackageName() { return packageName; }
    public int getClassCount() { return classCount; }
    public int getInterfaceCount() { return interfaceCount; }
    public int getAbstractClassCount() { return abstractClassCount; }
    public int getAnnotationCount() { return annotationCount; }
    public int getEnumCount() { return enumCount; }
    public int getSpringComponentCount() { return springComponentCount; }
    public List<String> getClassNames() { return Collections.unmodifiableList(classNames); }

    public double getClassPercentage(double totalClasses) {
        return totalClasses > 0 ? (classCount / totalClasses) * 100.0 : 0.0;
    }
}
