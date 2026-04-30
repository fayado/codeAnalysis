package com.mimo.analysis.core.model;

/**
 * 类引用关系模型，描述两个类之间的依赖关系。
 *
 * 记录了引用的来源类（fromClass）、目标类（toClass）以及引用类型（type）。
 * 例如：UserService 依赖 UserRepository，引用类型为 FIELD_TYPE（字段注入）。
 *
 * 该类是去重的基本单位，通过 {@link #toKey()} 方法生成唯一标识键，
 * 格式为：来源类|目标类|引用类型，用于在 AnalysisReport 中按三元组去重。
 */
public class ClassReference {

    /** 引用的来源类全限定名，即依赖发起方 */
    private final String fromClass;

    /** 引用的目标类全限定名，即被依赖方 */
    private final String toClass;

    /** 引用类型，描述依赖关系的具体形式（继承、实现、字段引用等） */
    private final ReferenceType type;

    /**
     * 构造类引用关系实例。
     *
     * @param fromClass 引用来源类的全限定名
     * @param toClass   引用目标类的全限定名
     * @param type      引用类型
     */
    public ClassReference(String fromClass, String toClass, ReferenceType type) {
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.type = type;
    }

    /** 获取引用来源类的全限定名 */
    public String getFromClass() { return fromClass; }

    /** 获取引用目标类的全限定名 */
    public String getToClass() { return toClass; }

    /** 获取引用类型 */
    public ReferenceType getType() { return type; }

    /**
     * 生成引用关系的唯一标识键。
     * 格式为：来源类|目标类|引用类型，用于去重判断。
     *
     * @return 唯一标识字符串
     */
    public String toKey() {
        return fromClass + "|" + toClass + "|" + type;
    }

    /**
     * 相等性判断，基于来源类、目标类和引用类型三元组。
     * 只有三者完全相同才视为相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassReference that = (ClassReference) o;
        return fromClass.equals(that.fromClass) && toClass.equals(that.toClass) && type == that.type;
    }

    /** 哈希码计算，基于三元组，与 equals 保持一致 */
    @Override
    public int hashCode() {
        int result = fromClass.hashCode();
        result = 31 * result + toClass.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    /** 字符串表示，格式为：来源类 -> 目标类 [引用类型] */
    @Override
    public String toString() {
        return fromClass + " -> " + toClass + " [" + type + "]";
    }
}
