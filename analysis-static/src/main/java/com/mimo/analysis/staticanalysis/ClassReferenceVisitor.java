package com.mimo.analysis.staticanalysis;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.model.*;
import com.mimo.analysis.core.util.ClassNameUtils;
import org.objectweb.asm.*;

import java.util.*;

/**
 * 类引用访问器，基于 ASM 的 ClassVisitor 实现，从字节码中提取类的元数据和引用关系。
 *
 * 该类是静态字节码分析的核心组件，负责：
 * 1. 提取类的基本信息（全限定名、类型特征等），构建 {@link ClassInfo} 对象
 * 2. 识别类的继承关系（extends）和接口实现关系（implements）
 * 3. 提取类级别的注解引用，并检测 Spring 组件注解（@Service、@Controller 等）
 * 4. 提取字段声明中的类型引用
 * 5. 提取方法签名中的参数类型和返回值类型引用
 * 6. 委托 {@link MethodReferenceVisitor} 提取方法体内的指令级引用（new、invoke、cast 等）
 *
 * 对于提取到的每个引用，会先通过 {@link ClassFilter} 过滤掉不需要的类，
 * 并使用唯一键（来源类|目标类|引用类型）进行去重，避免重复引用。
 */
public class ClassReferenceVisitor extends ClassVisitor {

    /** 类过滤器，用于排除不需要分析的类 */
    private final ClassFilter filter;

    /** 类所在的 JAR 文件来源名称 */
    private final String jarSource;

    /** 当前正在访问的类的全限定名 */
    private String currentClassName;

    /** 当前类的元数据信息 */
    private ClassInfo classInfo;

    /** 收集到的所有引用关系列表 */
    private final List<ClassReference> references = new ArrayList<>();

    /** 引用关系的唯一标识键集合，用于去重 */
    private final Set<String> uniqueReferences = new HashSet<>();

    /**
     * Spring 组件注解全限定名集合。
     * 如果类上存在这些注解，则标记为 Spring 组件（isSpringComponent = true）。
     */
    private static final Set<String> SPRING_COMPONENT_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "org.springframework.stereotype.Component",       // @Component 通用组件
            "org.springframework.stereotype.Service",         // @Service 业务层组件
            "org.springframework.stereotype.Repository",      // @Repository 数据访问层组件
            "org.springframework.stereotype.Controller",      // @Controller 控制器组件
            "org.springframework.context.annotation.Configuration",  // @Configuration 配置类
            "org.springframework.web.bind.annotation.RestController", // @RestController REST 控制器
            "org.springframework.web.bind.annotation.Controller"      // @Controller Web 控制器
    ));

    /**
     * 构造类引用访问器实例。
     *
     * @param filter   类过滤器，用于排除不需要分析的类
     * @param jarSource 类所在的 JAR 文件来源名称
     */
    public ClassReferenceVisitor(ClassFilter filter, String jarSource) {
        super(Opcodes.ASM9);
        this.filter = filter;
        this.jarSource = jarSource;
    }

    /**
     * 访问类的基本信息，提取类的元数据和继承/实现关系。
     *
     * 该方法在 ASM 解析每个 .class 文件时被调用，负责：
     * 1. 将 JVM 内部格式的类名转换为标准点分隔格式
     * 2. 从访问标志中提取类的类型特征（接口、抽象类、注解、枚举）
     * 3. 构建 ClassInfo 对象
     * 4. 记录父类引用（extends）和接口引用（implements）
     *
     * @param version    类文件版本号
     * @param access     类的访问标志（public、abstract 等）
     * @param name       类的内部名（JVM 格式，使用 '/' 分隔）
     * @param signature  类的泛型签名（如果有的话）
     * @param superName  父类的内部名
     * @param interfaces 实现的接口内部名数组
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // 转换为标准点分隔格式
        currentClassName = ClassNameUtils.toDotNotation(name);

        // 从访问标志中提取类型特征
        boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0 && !isInterface;
        boolean isAnnotation = (access & Opcodes.ACC_ANNOTATION) != 0;
        boolean isEnum = (access & Opcodes.ACC_ENUM) != 0;

        // 构建类信息对象
        classInfo = new ClassInfo.Builder(currentClassName)
                .jarSource(jarSource)
                .origin(ClassOrigin.STATIC)
                .isInterface(isInterface)
                .isAbstract(isAbstract)
                .isAnnotation(isAnnotation)
                .isEnum(isEnum)
                .build();

        // 记录父类引用（排除 java.lang.Object，它是所有类的隐式父类）
        if (superName != null && !"java/lang/Object".equals(superName)) {
            addReference(superName, ReferenceType.EXTENDS);
        }

        // 记录接口实现引用
        if (interfaces != null) {
            for (String iface : interfaces) {
                addReference(iface, ReferenceType.IMPLEMENTS);
            }
        }
    }

    /**
     * 访问类上的注解，提取注解类型引用并检测 Spring 组件注解。
     *
     * 返回一个 AnnotationVisitor 用于进一步访问注解的属性值，
     * 如果注解属性值是 Type 类型（如 @RequestMapping 的属性），
     * 也会将其记录为引用。
     *
     * @param descriptor 注解类型的描述符
     * @param visible    注解在运行时是否可见
     * @return 注解属性访问器，用于访问注解的属性值
     */
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        // 记录注解类型引用
        addTypeReference(descriptor, ReferenceType.ANNOTATION);

        // 检查是否为 Spring 组件注解
        String annotationName = ClassNameUtils.normalizeDescriptor(descriptor);
        if (SPRING_COMPONENT_ANNOTATIONS.contains(annotationName) && classInfo != null) {
            classInfo = classInfo.toBuilder().isSpringComponent(true).build();
        }

        // 返回注解属性访问器，用于访问注解的属性值
        return new AnnotationVisitor(Opcodes.ASM9) {
            /**
             * 访问注解的普通属性值。
             * 如果属性值是 Type 类型（类引用），则记录为注解引用。
             */
            @Override
            public void visit(String name, Object value) {
                if (value instanceof Type) {
                    Type type = (Type) value;
                    if (type.getSort() == Type.OBJECT) {
                        addReference(type.getInternalName(), ReferenceType.ANNOTATION);
                    }
                }
            }

            /**
             * 访问注解的枚举属性值。
             * 将枚举类型记录为注解引用。
             */
            @Override
            public void visitEnum(String name, String descriptor, String value) {
                addTypeReference(descriptor, ReferenceType.ANNOTATION);
            }
        };
    }

    /**
     * 访问类的字段声明，提取字段类型引用。
     *
     * 例如：private UserService userService;
     * 会记录当前类对 UserService 的 FIELD_TYPE 引用。
     *
     * @param access     字段的访问标志
     * @param name       字段名
     * @param descriptor 字段类型的描述符
     * @param signature  字段的泛型签名
     * @param value      字段的默认值（如果有）
     * @return null，不需要进一步访问字段属性
     */
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        addTypeReference(descriptor, ReferenceType.FIELD_TYPE);
        return null;
    }

    /**
     * 访问类的方法声明，提取方法签名中的类型引用。
     *
     * 提取以下引用：
     * - 方法返回值类型
     * - 方法参数类型列表
     * - 方法声明的异常类型
     *
     * 返回 MethodReferenceVisitor 用于进一步访问方法体内的指令级引用。
     *
     * @param access     方法的访问标志
     * @param name       方法名
     * @param descriptor 方法描述符（包含参数类型和返回值类型）
     * @param signature  方法的泛型签名
     * @param exceptions 方法声明抛出的异常类型数组
     * @return 方法体访问器，用于提取方法体内的引用
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // 提取返回值类型引用
        addTypeReferenceFromMethodDesc(descriptor);

        // 提取参数类型引用
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        for (Type argType : argumentTypes) {
            addTypeRef(argType, ReferenceType.METHOD_PARAM);
        }

        // 提取异常类型引用
        if (exceptions != null) {
            for (String exc : exceptions) {
                addReference(exc, ReferenceType.METHOD_PARAM);
            }
        }

        // 返回方法体访问器，用于提取方法体内的指令级引用
        return new MethodReferenceVisitor(this);
    }

    /**
     * 添加一条类引用关系。
     *
     * 处理流程：
     * 1. 将 JVM 内部名转换为标准点分隔格式
     * 2. 检查目标类是否被过滤器排除
     * 3. 检查是否为自引用（类引用自身）
     * 4. 使用唯一键去重后添加到引用列表
     *
     * @param internalName 目标类的 JVM 内部名
     * @param type         引用类型
     */
    public void addReference(String internalName, ReferenceType type) {
        String dotName = ClassNameUtils.toDotNotation(internalName);
        // 过滤被排除的类和自引用
        if (!filter.shouldExclude(dotName) && !dotName.equals(currentClassName)) {
            String key = currentClassName + "|" + dotName + "|" + type;
            if (uniqueReferences.add(key)) {
                references.add(new ClassReference(currentClassName, dotName, type));
            }
        }
    }

    /**
     * 从类型描述符中提取类引用。
     *
     * 处理两种情况：
     * - 对象类型描述符（Lcom/example/User;）：直接提取
     * - 数组类型描述符（[Lcom/example/User;）：提取数组元素类型
     *
     * @param descriptor 类型描述符
     * @param type       引用类型
     */
    public void addTypeReference(String descriptor, ReferenceType type) {
        if (descriptor == null) return;
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            // 对象类型：去除 L 前缀和 ; 后缀
            String internalName = descriptor.substring(1, descriptor.length() - 1);
            addReference(internalName, type);
        } else if (descriptor.startsWith("[")) {
            // 数组类型：去除数组维度前缀，提取元素类型
            String d = descriptor;
            while (d.startsWith("[")) d = d.substring(1);
            if (d.startsWith("L") && d.endsWith(";")) {
                addReference(d.substring(1, d.length() - 1), ReferenceType.ARRAY_ELEMENT);
            }
        }
    }

    /**
     * 从方法描述符中提取返回值类型引用。
     *
     * @param descriptor 方法描述符
     */
    private void addTypeReferenceFromMethodDesc(String descriptor) {
        Type returnType = Type.getReturnType(descriptor);
        addTypeRef(returnType, ReferenceType.METHOD_RETURN);
    }

    /**
     * 根据 ASM Type 对象添加类型引用。
     * 处理对象类型和数组类型（提取数组元素类型）。
     *
     * @param type    ASM Type 对象
     * @param refType 引用类型
     */
    private void addTypeRef(Type type, ReferenceType refType) {
        if (type.getSort() == Type.OBJECT) {
            addReference(type.getInternalName(), refType);
        } else if (type.getSort() == Type.ARRAY) {
            Type elementType = type.getElementType();
            if (elementType.getSort() == Type.OBJECT) {
                addReference(elementType.getInternalName(), ReferenceType.ARRAY_ELEMENT);
            }
        }
    }

    /**
     * 获取当前类的元数据信息。
     *
     * @return ClassInfo 对象，如果尚未访问类则返回 null
     */
    public ClassInfo getClassInfo() {
        return classInfo;
    }

    /**
     * 获取收集到的所有引用关系的不可修改列表。
     *
     * @return 引用关系列表
     */
    public List<ClassReference> getReferences() {
        return Collections.unmodifiableList(references);
    }
}
