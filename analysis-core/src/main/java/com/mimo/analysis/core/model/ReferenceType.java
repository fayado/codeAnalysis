package com.mimo.analysis.core.model;

public enum ReferenceType {
    EXTENDS,
    IMPLEMENTS,
    FIELD_TYPE,
    METHOD_PARAM,
    METHOD_RETURN,
    ANNOTATION,
    INVOKE,
    INSTANCEOF,
    NEW,
    CAST,
    ARRAY_ELEMENT
}
