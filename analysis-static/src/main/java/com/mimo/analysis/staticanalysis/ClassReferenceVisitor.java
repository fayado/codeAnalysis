package com.mimo.analysis.staticanalysis;

import com.mimo.analysis.core.filter.ClassFilter;
import com.mimo.analysis.core.model.*;
import com.mimo.analysis.core.util.ClassNameUtils;
import org.objectweb.asm.*;

import java.util.*;

public class ClassReferenceVisitor extends ClassVisitor {

    private final ClassFilter filter;
    private final String jarSource;
    private String currentClassName;
    private ClassInfo classInfo;
    private final List<ClassReference> references = new ArrayList<>();
    private final Set<String> uniqueReferences = new HashSet<>();

    // Spring component annotations
    private static final Set<String> SPRING_COMPONENT_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Repository",
            "org.springframework.stereotype.Controller",
            "org.springframework.context.annotation.Configuration",
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.web.bind.annotation.Controller"
    ));

    public ClassReferenceVisitor(ClassFilter filter, String jarSource) {
        super(Opcodes.ASM9);
        this.filter = filter;
        this.jarSource = jarSource;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        currentClassName = ClassNameUtils.toDotNotation(name);

        boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0 && !isInterface;
        boolean isAnnotation = (access & Opcodes.ACC_ANNOTATION) != 0;
        boolean isEnum = (access & Opcodes.ACC_ENUM) != 0;

        classInfo = new ClassInfo.Builder(currentClassName)
                .jarSource(jarSource)
                .origin(ClassOrigin.STATIC)
                .isInterface(isInterface)
                .isAbstract(isAbstract)
                .isAnnotation(isAnnotation)
                .isEnum(isEnum)
                .build();

        // Superclass reference
        if (superName != null && !"java/lang/Object".equals(superName)) {
            addReference(superName, ReferenceType.EXTENDS);
        }

        // Interface references
        if (interfaces != null) {
            for (String iface : interfaces) {
                addReference(iface, ReferenceType.IMPLEMENTS);
            }
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        addTypeReference(descriptor, ReferenceType.ANNOTATION);

        // Check if this is a Spring component annotation
        String annotationName = ClassNameUtils.normalizeDescriptor(descriptor);
        if (SPRING_COMPONENT_ANNOTATIONS.contains(annotationName) && classInfo != null) {
            classInfo = classInfo.toBuilder().isSpringComponent(true).build();
        }

        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if (value instanceof Type) {
                    Type type = (Type) value;
                    if (type.getSort() == Type.OBJECT) {
                        addReference(type.getInternalName(), ReferenceType.ANNOTATION);
                    }
                }
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                addTypeReference(descriptor, ReferenceType.ANNOTATION);
            }
        };
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        addTypeReference(descriptor, ReferenceType.FIELD_TYPE);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // Return type
        addTypeReferenceFromMethodDesc(descriptor);

        // Parameter types
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        for (Type argType : argumentTypes) {
            addTypeRef(argType, ReferenceType.METHOD_PARAM);
        }

        // Exception types
        if (exceptions != null) {
            for (String exc : exceptions) {
                addReference(exc, ReferenceType.METHOD_PARAM);
            }
        }

        return new MethodReferenceVisitor(this);
    }

    public void addReference(String internalName, ReferenceType type) {
        String dotName = ClassNameUtils.toDotNotation(internalName);
        if (!filter.shouldExclude(dotName) && !dotName.equals(currentClassName)) {
            String key = currentClassName + "|" + dotName + "|" + type;
            if (uniqueReferences.add(key)) {
                references.add(new ClassReference(currentClassName, dotName, type));
            }
        }
    }

    public void addTypeReference(String descriptor, ReferenceType type) {
        if (descriptor == null) return;
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            String internalName = descriptor.substring(1, descriptor.length() - 1);
            addReference(internalName, type);
        } else if (descriptor.startsWith("[")) {
            // Array type - extract element type
            String d = descriptor;
            while (d.startsWith("[")) d = d.substring(1);
            if (d.startsWith("L") && d.endsWith(";")) {
                addReference(d.substring(1, d.length() - 1), ReferenceType.ARRAY_ELEMENT);
            }
        }
    }

    private void addTypeReferenceFromMethodDesc(String descriptor) {
        Type returnType = Type.getReturnType(descriptor);
        addTypeRef(returnType, ReferenceType.METHOD_RETURN);
    }

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

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public List<ClassReference> getReferences() {
        return Collections.unmodifiableList(references);
    }
}
