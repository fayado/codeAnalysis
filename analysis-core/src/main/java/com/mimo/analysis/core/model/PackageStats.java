package com.mimo.analysis.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包统计信息模型，记录某个包下所有类的统计数据。
 *
 * 用于按包维度汇总分析结果，包括：
 * - 类总数（含普通类、接口、抽象类、注解、枚举）
 * - Spring 组件数量
 * - 包下所有类名列表
 *
 * 通过 {@link #addClass(ClassInfo)} 方法逐个累加类信息，
 * 通过 {@link #getClassPercentage(double)} 方法计算该包类数占总数的百分比。
 */
public class PackageStats {

    /** 包名，例如：com.example.service */
    private final String packageName;

    /** 包下的类总数 */
    private int classCount;

    /** 包下的接口数量 */
    private int interfaceCount;

    /** 包下的抽象类数量 */
    private int abstractClassCount;

    /** 包下的注解类型数量 */
    private int annotationCount;

    /** 包下的枚举类型数量 */
    private int enumCount;

    /** 包下的 Spring 组件数量（@Service、@Controller、@Component 等） */
    private int springComponentCount;

    /** 包下所有类的全限定名列表 */
    private final List<String> classNames = new ArrayList<>();

    /**
     * 构造指定包名的统计信息实例。
     *
     * @param packageName 包名
     */
    public PackageStats(String packageName) {
        this.packageName = packageName;
    }

    /**
     * 向该包统计中添加一个类的信息，累加对应的计数器。
     *
     * @param info 要添加的类信息
     */
    public void addClass(ClassInfo info) {
        classNames.add(info.getClassName());
        classCount++;
        if (info.isInterface()) interfaceCount++;
        if (info.isAbstract()) abstractClassCount++;
        if (info.isAnnotation()) annotationCount++;
        if (info.isEnum()) enumCount++;
        if (info.isSpringComponent()) springComponentCount++;
    }

    /** 获取包名 */
    public String getPackageName() { return packageName; }

    /** 获取类总数 */
    public int getClassCount() { return classCount; }

    /** 获取接口数量 */
    public int getInterfaceCount() { return interfaceCount; }

    /** 获取抽象类数量 */
    public int getAbstractClassCount() { return abstractClassCount; }

    /** 获取注解类型数量 */
    public int getAnnotationCount() { return annotationCount; }

    /** 获取枚举类型数量 */
    public int getEnumCount() { return enumCount; }

    /** 获取 Spring 组件数量 */
    public int getSpringComponentCount() { return springComponentCount; }

    /** 获取包下所有类名的不可修改列表 */
    public List<String> getClassNames() { return Collections.unmodifiableList(classNames); }

    /**
     * 计算该包的类数占总类数的百分比。
     *
     * @param totalClasses 总类数
     * @return 百分比值（0.0 ~ 100.0），总类数为 0 时返回 0.0
     */
    public double getClassPercentage(double totalClasses) {
        return totalClasses > 0 ? (classCount / totalClasses) * 100.0 : 0.0;
    }
}
