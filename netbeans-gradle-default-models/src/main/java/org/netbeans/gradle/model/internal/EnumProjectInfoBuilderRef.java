package org.netbeans.gradle.model.internal;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.gradle.api.Project;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;

public final class EnumProjectInfoBuilderRef<T> implements ProjectInfoBuilder<T> {
    private static final long serialVersionUID = 1L;

    private final Class<? extends T> modelType;
    private final String wrappedTypeName;
    private final String wrappedConstName;

    private final AtomicReference<ProjectInfoBuilder<?>> wrappedRef;

    public EnumProjectInfoBuilderRef(
            Class<? extends T> modelType,
            String wrappedTypeName) {
        if (modelType == null) throw new NullPointerException("modelType");
        if (wrappedTypeName == null) throw new NullPointerException("wrappedTypeName");

        this.modelType = modelType;
        this.wrappedTypeName = updateTypeName(modelType, wrappedTypeName);
        this.wrappedConstName = null;
        this.wrappedRef = new AtomicReference<ProjectInfoBuilder<?>>(null);
    }

    public EnumProjectInfoBuilderRef(
            Class<? extends T> modelType,
            String wrappedTypeName,
            String wrappedConstName) {
        if (modelType == null) throw new NullPointerException("modelType");
        if (wrappedTypeName == null) throw new NullPointerException("wrappedTypeName");
        if (wrappedConstName == null) throw new NullPointerException("wrappedConstName");

        this.modelType = modelType;
        this.wrappedTypeName = updateTypeName(modelType, wrappedTypeName);
        this.wrappedConstName = wrappedConstName;
        this.wrappedRef = new AtomicReference<ProjectInfoBuilder<?>>(null);
    }

    private static String updateTypeName(Class<?> defaultPackage, String typeName) {
        if (typeName.indexOf('.') >= 0) {
            return typeName;
        }
        return defaultPackage.getPackage().getName() + "." + typeName;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> Class<T> unsafeEnumCast(Class<?> type) {
        return (Class<T>)type;
    }

    private static Object unsafeEnumValueOf(Class<?> type, String constName) {
        if (constName != null) {
            return Enum.valueOf(unsafeEnumCast(type), constName);
        }
        else {
            Object[] consts = type.getEnumConstants();
            if (consts.length != 1) {
                throw new IllegalStateException("Cannot determine which enum const must be used: " + Arrays.asList(consts));
            }
            return consts[0];
        }
    }

    private ProjectInfoBuilder<?> createWrapped() {
        try {
            return (ProjectInfoBuilder<?>)unsafeEnumValueOf(Class.forName(wrappedTypeName), wrappedConstName);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException)ex;
            }
            throw new RuntimeException(ex);
        }
    }

    private ProjectInfoBuilder<?> getWrapped() {
        ProjectInfoBuilder<?> result = wrappedRef.get();
        if (result == null) {
            result = createWrapped();
            if (!wrappedRef.compareAndSet(null, result)) {
                result = wrappedRef.get();
            }
        }
        return result;
    }

    public T getProjectInfo(Project project) {
        Object result = getWrapped().getProjectInfo(project);
        return modelType.cast(result);
    }

    public String getName() {
        return getWrapped().getName();
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Class<?> modelType;
        private final String wrappedTypeName;
        private final String wrappedConstName;

        public SerializedFormat(EnumProjectInfoBuilderRef<?> source) {
            this.modelType = source.modelType;
            this.wrappedTypeName = source.wrappedTypeName;
            this.wrappedConstName = source.wrappedConstName;
        }

        private Object readResolve() throws ObjectStreamException {
            return wrappedConstName != null
                    ? new EnumProjectInfoBuilderRef<Object>(modelType, wrappedTypeName, wrappedConstName)
                    : new EnumProjectInfoBuilderRef<Object>(modelType, wrappedTypeName);
        }
    }
}
