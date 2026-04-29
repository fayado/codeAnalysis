package com.mimo.analysis.staticanalysis;

import com.mimo.analysis.core.model.ReferenceType;
import com.mimo.analysis.core.util.ClassNameUtils;
import org.objectweb.asm.*;

public class MethodReferenceVisitor extends MethodVisitor {

    private final ClassReferenceVisitor parent;

    public MethodReferenceVisitor(ClassReferenceVisitor parent) {
        super(Opcodes.ASM9);
        this.parent = parent;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        String dotName = ClassNameUtils.toDotNotation(type);
        switch (opcode) {
            case Opcodes.NEW:
                parent.addReference(type, ReferenceType.NEW);
                break;
            case Opcodes.CHECKCAST:
                parent.addReference(type, ReferenceType.CAST);
                break;
            case Opcodes.INSTANCEOF:
                parent.addReference(type, ReferenceType.INSTANCEOF);
                break;
            case Opcodes.ANEWARRAY:
                parent.addReference(type, ReferenceType.ARRAY_ELEMENT);
                break;
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        parent.addReference(owner, ReferenceType.INVOKE);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        parent.addReference(owner, ReferenceType.FIELD_TYPE);
        parent.addTypeReference(descriptor, ReferenceType.FIELD_TYPE);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof Type) {
            Type type = (Type) value;
            if (type.getSort() == Type.OBJECT) {
                parent.addReference(type.getInternalName(), ReferenceType.INVOKE);
            } else if (type.getSort() == Type.ARRAY) {
                String elementType = type.getElementType().getInternalName();
                parent.addReference(elementType, ReferenceType.ARRAY_ELEMENT);
            }
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        parent.addTypeReference(descriptor, ReferenceType.ANNOTATION);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        parent.addTypeReference(descriptor, ReferenceType.ANNOTATION);
        return super.visitParameterAnnotation(parameter, descriptor, visible);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (type != null) {
            parent.addReference(type, ReferenceType.INVOKE);
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        String d = descriptor;
        while (d.startsWith("[")) d = d.substring(1);
        if (d.startsWith("L") && d.endsWith(";")) {
            parent.addReference(d.substring(1, d.length() - 1), ReferenceType.ARRAY_ELEMENT);
        }
    }
}
