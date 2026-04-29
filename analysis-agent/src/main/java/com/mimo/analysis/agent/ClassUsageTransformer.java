package com.mimo.analysis.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ClassUsageTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className != null) {
            // className uses '/' notation, e.g., "com/example/MyClass"
            AgentDataCollector.recordClass(className);
        }
        // Return null to indicate no transformation of bytecode
        return null;
    }
}
