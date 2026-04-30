package com.mimo.analysis.core.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 默认类过滤器实现，排除 JDK 核心类、Tomcat 容器类和 CGLIB 代理类。
 *
 * 过滤策略：
 * 1. 类名前缀排除：排除 java.*、javax.*、sun.*、jdk.* 等 JDK 核心包，
 *    以及 org.apache.catalina.*、org.apache.tomcat.* 等 Tomcat 容器包
 * 2. 类名后缀排除：排除 Spring CGLIB 代理类（$$EnhancerBySpringCGLIB$$ 等）
 *    和 Hibernate 代理类（$HibernateProxy$ 等）
 * 3. JAR 文件名排除：排除 rt.jar、tools.jar 等 JDK 核心 JAR，
 *    以及 tomcat-embed-*.jar、servlet-api-*.jar 等 Tomcat 相关 JAR
 * 4. 支持自定义额外排除和包含规则（通过构造函数传入）
 *
 * 优先级：自定义包含规则 > 后缀排除 > 前缀排除 > 自定义排除规则
 * 即：如果一个类匹配了自定义包含规则，即使它也匹配排除规则，仍会被包含。
 */
public class DefaultClassFilter implements ClassFilter {

    /**
     * 默认排除的类名前缀列表。
     * 包含 JDK 核心包和 Tomcat 容器包，这些类不属于项目业务代码。
     */
    private static final List<String> EXCLUDED_PREFIXES = Arrays.asList(
            "java.",                 // Java 核心类库
            "javax.",                // Java 扩展类库
            "sun.",                  // Sun 内部实现类
            "com.sun.",              // Sun 公司内部实现类
            "jdk.",                  // JDK 内部工具类
            "netscape.",             // Netscape 兼容类
            "org.xml.sax.",          // XML SAX 解析器
            "org.w3c.dom.",          // W3C DOM 接口
            "org.ietf.jgss.",        // GSS-API 认证框架
            "org.apache.catalina.",  // Tomcat Catalina 容器
            "org.apache.tomcat.",    // Tomcat 核心组件
            "org.apache.coyote.",    // Tomcat Coyote 连接器
            "org.apache.el.",        // Tomcat 表达式语言
            "org.apache.jasper.",    // Tomcat JSP 引擎
            "org.apache.naming.",    // Tomcat JNDI 命名服务
            "org.apache.juli."       // Tomcat 日志组件
    );

    /**
     * 默认排除的类名后缀列表。
     * 包含各种字节码增强工具生成的代理类后缀，这些类是框架自动生成的，
     * 不属于开发者编写的业务代码。
     */
    private static final List<String> EXCLUDED_SUFFIXES = Arrays.asList(
            "$$EnhancerBySpringCGLIB$$",   // Spring CGLIB 动态代理类
            "$$FastClassBySpringCGLIB$$",  // Spring CGLIB FastClass
            "$HibernateProxy$",            // Hibernate 懒加载代理类
            "_$$_jvst"                     // Javassist 字节码增强生成的类
    );

    /**
     * 默认排除的 JAR 文件名正则模式列表。
     * 包含 JDK 核心 JAR 和 Tomcat 相关 JAR，这些 JAR 不属于项目依赖。
     */
    private static final List<Pattern> EXCLUDED_JAR_PATTERNS = Arrays.asList(
            Pattern.compile("rt\\.jar"),              // Java 运行时核心类库
            Pattern.compile("tools\\.jar"),           // Java 开发工具类库
            Pattern.compile("jce\\.jar"),             // Java 加密扩展
            Pattern.compile("charsets\\.jar"),        // 字符集支持
            Pattern.compile("jfr\\.jar"),             // Java Flight Recorder
            Pattern.compile("tomcat-embed-.*\\.jar"), // Tomcat 嵌入式核心
            Pattern.compile("catalina-.*\\.jar"),     // Tomcat Catalina 容器
            Pattern.compile("coyote-.*\\.jar"),       // Tomcat Coyote 连接器
            Pattern.compile("jasper-.*\\.jar"),       // Tomcat JSP 引擎
            Pattern.compile("el-api-.*\\.jar"),       // 表达式语言 API
            Pattern.compile("jsp-api-.*\\.jar"),      // JSP API
            Pattern.compile("servlet-api-.*\\.jar"),  // Servlet API
            Pattern.compile("websocket-.*\\.jar")     // WebSocket API
    );

    /** 用户自定义的额外排除前缀列表 */
    private final List<String> additionalExclusions;

    /** 用户自定义的额外包含前缀列表（优先级高于排除规则） */
    private final List<String> additionalInclusions;

    /**
     * 使用默认配置创建过滤器实例，无自定义排除/包含规则。
     */
    public DefaultClassFilter() {
        this.additionalExclusions = Collections.emptyList();
        this.additionalInclusions = Collections.emptyList();
    }

    /**
     * 使用自定义排除/包含规则创建过滤器实例。
     *
     * @param additionalExclusions 额外的排除前缀列表，例如 ["org.example.internal."]
     * @param additionalInclusions 额外的包含前缀列表，例如 ["org.example.special."]，
     *                            匹配的类即使命中排除规则也会被保留
     */
    public DefaultClassFilter(List<String> additionalExclusions, List<String> additionalInclusions) {
        this.additionalExclusions = additionalExclusions != null ? additionalExclusions : Collections.emptyList();
        this.additionalInclusions = additionalInclusions != null ? additionalInclusions : Collections.emptyList();
    }

    /**
     * 判断指定类是否应该被包含在分析结果中。
     * 逻辑与 {@link #shouldExclude(String)} 相反。
     */
    @Override
    public boolean shouldInclude(String className) {
        return !shouldExclude(className);
    }

    /**
     * 判断指定类是否应该被排除在分析结果之外。
     *
     * 判断流程：
     * 1. null 或空字符串直接排除
     * 2. 将 '/' 分隔的内部名转换为 '.' 分隔的全限定名
     * 3. 检查是否匹配自定义包含规则（匹配则不排除）
     * 4. 检查是否匹配排除后缀（CGLIB 代理类等）
     * 5. 检查是否匹配默认排除前缀（JDK、Tomcat 等）
     * 6. 检查是否匹配自定义排除规则
     */
    @Override
    public boolean shouldExclude(String className) {
        if (className == null || className.isEmpty()) return true;

        // 统一将路径分隔符转换为包名分隔符
        String normalized = className.replace('/', '.');

        // 自定义包含规则优先级最高，匹配则不排除
        for (String inc : additionalInclusions) {
            if (normalized.startsWith(inc)) return false;
        }

        // 检查排除后缀（代理类等）
        for (String suffix : EXCLUDED_SUFFIXES) {
            if (normalized.contains(suffix)) return true;
        }

        // 检查默认排除前缀（JDK、Tomcat 等）
        for (String prefix : EXCLUDED_PREFIXES) {
            if (normalized.startsWith(prefix)) return true;
        }

        // 检查自定义排除规则
        for (String exc : additionalExclusions) {
            if (normalized.startsWith(exc)) return true;
        }

        return false;
    }

    /**
     * 判断指定 JAR 文件是否应该被整体排除在分析范围之外。
     * 通过正则匹配 JAR 文件名，默认排除 JDK 核心 JAR 和 Tomcat 相关 JAR。
     */
    @Override
    public boolean shouldExcludeJar(String jarName) {
        if (jarName == null) return true;
        for (Pattern pattern : EXCLUDED_JAR_PATTERNS) {
            if (pattern.matcher(jarName).matches()) return true;
        }
        return false;
    }
}
