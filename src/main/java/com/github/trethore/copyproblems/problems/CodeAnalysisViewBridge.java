package com.github.trethore.copyproblems.problems;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.reference.RefEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Keeps access to the Code Analysis tree's internal severity field isolated.
 */
final class CodeAnalysisViewBridge {
    private static final Class<?> PROBLEM_NODE_TYPE = loadProblemNodeType();
    private static final Method GET_CHILDREN = loadMethod(PROBLEM_NODE_TYPE.getSuperclass(), "getChildren");
    private static final Method GET_DESCRIPTOR = loadMethod(PROBLEM_NODE_TYPE, "getDescriptor");
    private static final Method GET_ELEMENT = loadMethod(PROBLEM_NODE_TYPE, "getElement");
    private static final Method GET_PRESENTABLE_TEXT = loadMethod(PROBLEM_NODE_TYPE.getSuperclass(), "getPresentableText");
    private static final Field LEVEL_FIELD = loadLevelField();

    private CodeAnalysisViewBridge() {
    }

    static List<Object> selectedNodes(Object view) {
        JTree tree = tree(view);
        if (tree == null) {
            return Collections.emptyList();
        }

        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(paths).map(TreePath::getLastPathComponent).toList();
    }

    static Object root(Object view) {
        JTree tree = tree(view);
        return tree == null ? null : tree.getModel().getRoot();
    }

    static List<Object> children(Object node) {
        Object children = invoke(GET_CHILDREN, node);
        if (!(children instanceof Collection<?> collection)) {
            return Collections.emptyList();
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .map(Object.class::cast)
                .toList();
    }

    static boolean isProblemNode(Object node) {
        return PROBLEM_NODE_TYPE.isInstance(node);
    }

    static CommonProblemDescriptor descriptor(Object node) {
        return (CommonProblemDescriptor) invoke(GET_DESCRIPTOR, node);
    }

    static RefEntity element(Object node) {
        return (RefEntity) invoke(GET_ELEMENT, node);
    }

    static String message(Object node) {
        String text = (String) invoke(GET_PRESENTABLE_TEXT, node);
        return text == null || text.isBlank() ? "Problem description unavailable" : text;
    }

    static HighlightDisplayLevel level(Object node) {
        try {
            return (HighlightDisplayLevel) LEVEL_FIELD.get(node);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read IntelliJ Code Analysis severity", exception);
        }
    }

    @SuppressWarnings("java:S3011") // Required to read the Code Analysis node's internal severity.
    private static Field loadLevelField() {
        try {
            Field field = PROBLEM_NODE_TYPE.getDeclaredField("myLevel");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("IntelliJ Code Analysis API has changed", exception);
        }
    }

    private static JTree tree(Object view) {
        if (view == null) {
            return null;
        }

        Method getTree = loadMethod(view.getClass(), "getTree");
        Object tree = invoke(getTree, view);
        return tree instanceof JTree jTree ? jTree : null;
    }

    private static Class<?> loadProblemNodeType() {
        try {
            return Class.forName("com.intellij.codeInspection.ui.ProblemDescriptionNode");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("IntelliJ Code Analysis API is unavailable", exception);
        }
    }

    private static Method loadMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("IntelliJ Code Analysis API has changed", exception);
        }
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read IntelliJ Code Analysis node", exception);
        }
    }
}
