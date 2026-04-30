package com.mimo.analysis.core.model;

/**
 * 类引用类型枚举，描述两个类之间存在的引用关系类型。
 *
 * 在字节码分析过程中，ASM 会识别出类之间的多种依赖关系，
 * 如继承、实现接口、字段类型引用、方法参数引用等。
 * 该枚举用于区分这些不同的引用关系，便于生成更精确的依赖分析报告。
 */
public enum ReferenceType {

    /**
     * 继承关系：class A extends B，表示 A 类继承自 B 类。
     */
    EXTENDS,

    /**
     * 实现接口关系：class A implements B，表示 A 类实现了 B 接口。
     */
    IMPLEMENTS,

    /**
     * 字段类型引用：类中声明的字段使用了某个类型。
     * 例如：private UserService userService;
     */
    FIELD_TYPE,

    /**
     * 方法参数引用：方法的参数列表中使用了某个类型。
     * 例如：public void save(User user)
     */
    METHOD_PARAM,

    /**
     * 方法返回值引用：方法的返回类型使用了某个类型。
     * 例如：public User findById(Long id)
     */
    METHOD_RETURN,

    /**
     * 注解引用：类、方法或字段上使用了某个注解。
     * 例如：@Autowired、@Service 等。
     */
    ANNOTATION,

    /**
     * 方法调用引用：在代码中调用了某个类的方法。
     * 例如：userService.findAll()
     */
    INVOKE,

    /**
     * instanceof 检查引用：使用 instanceof 运算符检查某个类型。
     * 例如：if (obj instanceof User)
     */
    INSTANCEOF,

    /**
     * 实例化引用：使用 new 关键字创建了某个类的实例。
     * 例如：new User()
     */
    NEW,

    /**
     * 类型转换引用：将对象强制转换为某个类型。
     * 例如：(User) obj
     */
    CAST,

    /**
     * 数组元素类型引用：数组声明中使用了某个类型作为元素类型。
     * 例如：User[] users
     */
    ARRAY_ELEMENT
}
