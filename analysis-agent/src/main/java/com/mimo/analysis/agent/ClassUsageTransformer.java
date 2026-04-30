package com.mimo.analysis.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * 类使用情况字节码转换器，通过 Java Instrumentation API 拦截类加载事件。
 *
 * 该类实现了 {@link ClassFileTransformer} 接口，在 JVM 加载每个类时被回调。
 * 它不会修改类的字节码（返回 null 表示不做转换），只是被动地记录被加载的类名。
 *
 * 工作原理：
 * - JVM 在加载每个类时会调用所有已注册的 ClassFileTransformer
 * - 该转换器在回调中将类名记录到 {@link AgentDataCollector}
 * - 返回 null 表示不修改原始字节码，类正常加载
 *
 * 注意：className 使用 JVM 内部格式（以 '/' 分隔，如 "com/example/MyClass"），
 * 在 AgentDataCollector 中会转换为标准点分隔格式。
 */
public class ClassUsageTransformer implements ClassFileTransformer {

    /**
     * 拦截类加载事件，记录被加载的类名。
     *
     * @param loader              加载该类的 ClassLoader
     * @param className           类的内部名（使用 '/' 分隔），如 "com/example/MyClass"
     * @param classBeingRedefined 如果是类重定义（retransform），则为原始类；否则为 null
     * @param protectionDomain    类的保护域
     * @param classfileBuffer     类的原始字节码
     * @return null 表示不修改字节码，类正常加载
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className != null) {
            // 记录被加载的类名（使用 '/' 分隔的 JVM 内部格式）
            AgentDataCollector.recordClass(className);
        }
        // 返回 null 表示不转换字节码
        return null;
    }
}
