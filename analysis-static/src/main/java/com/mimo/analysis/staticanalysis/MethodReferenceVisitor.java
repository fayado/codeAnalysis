package com.mimo.analysis.staticanalysis;

import com.mimo.analysis.core.model.ReferenceType;
import com.mimo.analysis.core.util.ClassNameUtils;
import org.objectweb.asm.*;

/**
 * 方法体引用访问器，基于 ASM 的 MethodVisitor 实现，从方法字节码指令中提取类引用。
 *
 * 该类是 {@link ClassReferenceVisitor} 的辅助组件，专门负责分析方法体内的
 * 字节码指令，识别以下类型的引用：
 *
 * - NEW：实例化对象（new 关键字）
 * - CHECKCAST：类型转换（强制类型转换）
 * - INSTANCEOF：类型检查（instanceof 运算符）
 * - ANEWARRAY：创建对象数组
 * - INVOKE：方法调用（invokevirtual、invokeinterface、invokespecial、invokestatic）
 * - FIELD_TYPE：字段访问（getfield、putfield、getstatic、putstatic）
 * - ANNOTATION：方法上的注解引用
 * - ARRAY_ELEMENT：多维数组创建中的元素类型
 *
 * 所有提取到的引用通过委托模式传递给父级 {@link ClassReferenceVisitor} 进行过滤和去重。
 */
public class MethodReferenceVisitor extends MethodVisitor {

    /** 父级类引用访问器，用于委托引用的添加 */
    private final ClassReferenceVisitor parent;

    /**
     * 构造方法体引用访问器。
     *
     * @param parent 父级类引用访问器
     */
    public MethodReferenceVisitor(ClassReferenceVisitor parent) {
        super(Opcodes.ASM9);
        this.parent = parent;
    }

    /**
     * 访问类型相关指令，提取 NEW、CHECKCAST、INSTANCEOF、ANEWARRAY 引用。
     *
     * @param opcode 指令操作码
     * @param type   指令涉及的类型（JVM 内部名）
     */
    @Override
    public void visitTypeInsn(int opcode, String type) {
        switch (opcode) {
            case Opcodes.NEW:        // new 关键字实例化对象
                parent.addReference(type, ReferenceType.NEW);
                break;
            case Opcodes.CHECKCAST:  // 强制类型转换
                parent.addReference(type, ReferenceType.CAST);
                break;
            case Opcodes.INSTANCEOF: // instanceof 类型检查
                parent.addReference(type, ReferenceType.INSTANCEOF);
                break;
            case Opcodes.ANEWARRAY:  // 创建对象数组
                parent.addReference(type, ReferenceType.ARRAY_ELEMENT);
                break;
        }
    }

    /**
     * 访问方法调用指令，提取方法调用引用。
     *
     * 包括 invokevirtual（虚方法调用）、invokeinterface（接口方法调用）、
     * invokespecial（私有/构造方法调用）、invokestatic（静态方法调用）。
     *
     * @param opcode       指令操作码
     * @param owner        方法所属类的内部名
     * @param name         方法名
     * @param descriptor   方法描述符
     * @param isInterface  方法是否属于接口
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        parent.addReference(owner, ReferenceType.INVOKE);
    }

    /**
     * 访问字段操作指令，提取字段访问引用。
     *
     * 包括 getfield（读取实例字段）、putfield（写入实例字段）、
     * getstatic（读取静态字段）、putstatic（写入静态字段）。
     * 同时提取字段类型的引用。
     *
     * @param opcode     指令操作码
     * @param owner      字段所属类的内部名
     * @param name       字段名
     * @param descriptor 字段类型描述符
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        // 记录字段所属类的引用
        parent.addReference(owner, ReferenceType.FIELD_TYPE);
        // 记录字段类型的引用
        parent.addTypeReference(descriptor, ReferenceType.FIELD_TYPE);
    }

    /**
     * 访问 LDC（加载常量）指令，提取类字面量引用。
     *
     * 当代码中使用 Class.forName() 或类字面量（如 User.class）时，
     * 编译器会生成 LDC 指令加载 Type 常量。
     *
     * @param value 加载的常量值
     */
    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof Type) {
            Type type = (Type) value;
            if (type.getSort() == Type.OBJECT) {
                // 对象类型常量（如 User.class）
                parent.addReference(type.getInternalName(), ReferenceType.INVOKE);
            } else if (type.getSort() == Type.ARRAY) {
                // 数组类型常量
                String elementType = type.getElementType().getInternalName();
                parent.addReference(elementType, ReferenceType.ARRAY_ELEMENT);
            }
        }
    }

    /**
     * 访问方法上的注解，提取注解类型引用。
     *
     * @param descriptor 注解类型的描述符
     * @param visible    注解在运行时是否可见
     * @return null，不需要进一步访问注解属性
     */
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        parent.addTypeReference(descriptor, ReferenceType.ANNOTATION);
        return super.visitAnnotation(descriptor, visible);
    }

    /**
     * 访问方法参数上的注解，提取注解类型引用。
     *
     * @param parameter  参数索引
     * @param descriptor 注解类型的描述符
     * @param visible    注解在运行时是否可见
     * @return null，不需要进一步访问注解属性
     */
    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        parent.addTypeReference(descriptor, ReferenceType.ANNOTATION);
        return super.visitParameterAnnotation(parameter, descriptor, visible);
    }

    /**
     * 访问 try-catch 块，提取异常类型的引用。
     *
     * @param start   try 块起始标签
     * @param end     try 块结束标签
     * @param handler catch 块起始标签
     * @param type    异常类型的内部名
     */
    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (type != null) {
            parent.addReference(type, ReferenceType.INVOKE);
        }
    }

    /**
     * 访问多维数组创建指令，提取数组元素类型的引用。
     *
     * 例如：new User[10][20] 会生成 multianewarray 指令，
     * 需要提取 User 类型的引用。
     *
     * @param descriptor   数组类型描述符
     * @param numDimensions 数组维度
     */
    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        // 去除数组维度前缀，提取元素类型
        String d = descriptor;
        while (d.startsWith("[")) d = d.substring(1);
        if (d.startsWith("L") && d.endsWith(";")) {
            parent.addReference(d.substring(1, d.length() - 1), ReferenceType.ARRAY_ELEMENT);
        }
    }
}
