package com.mimo.analysis.core.model;

public class ClassReference {

    private final String fromClass;
    private final String toClass;
    private final ReferenceType type;

    public ClassReference(String fromClass, String toClass, ReferenceType type) {
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.type = type;
    }

    public String getFromClass() { return fromClass; }
    public String getToClass() { return toClass; }
    public ReferenceType getType() { return type; }

    public String toKey() {
        return fromClass + "|" + toClass + "|" + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassReference that = (ClassReference) o;
        return fromClass.equals(that.fromClass) && toClass.equals(that.toClass) && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = fromClass.hashCode();
        result = 31 * result + toClass.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return fromClass + " -> " + toClass + " [" + type + "]";
    }
}
