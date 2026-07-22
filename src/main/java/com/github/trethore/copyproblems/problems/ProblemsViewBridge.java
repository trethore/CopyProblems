package com.github.trethore.copyproblems.problems;

import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.openapi.vfs.VirtualFile;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Keeps the Problems View's internal node API behind a small reflective adapter.
 * Problem data is read from the underlying problem rather than the node's cached
 * presentation fields, which may not be initialized for collapsed tree nodes.
 */
final class ProblemsViewBridge {
    private static final Class<?> PROBLEM_NODE_TYPE = loadProblemNodeType();
    private static final Method GET_PROBLEM = loadGetProblemMethod();

    private ProblemsViewBridge() {
    }

    static Collection<Node> children(Node node) {
        return node.getChildren();
    }

    static boolean isProblemNode(Object node) {
        return PROBLEM_NODE_TYPE.isInstance(node);
    }

    static VirtualFile file(Object node) {
        return invoke(problem(node), "getFile", VirtualFile.class, null);
    }

    static String message(Object node) {
        Object problem = problem(node);

        String description = invoke(problem, "getDescription", String.class, null);
        if (description != null && !description.isBlank()) {
            return description;
        }

        String problemText = invoke(problem, "getText", String.class, "");
        if (!problemText.isBlank()) {
            return problemText;
        }

        if (node instanceof Node treeNode) {
            String presentationText = treeNode.getPresentation().getPresentableText();
            if (presentationText != null && !presentationText.isBlank()) {
                return presentationText;
            }
        }

        return "Problem description unavailable";
    }

    static int line(Object node) {
        return invokeNumber(problem(node), "getLine");
    }

    static int column(Object node) {
        return invokeNumber(problem(node), "getColumn");
    }

    static int severity(Object node) {
        return invokeNumber(problem(node), "getSeverity");
    }

    private static Object problem(Object node) {
        try {
            return GET_PROBLEM.invoke(node);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read IntelliJ problem node", exception);
        }
    }

    private static int invokeNumber(Object target, String methodName) {
        Number value = invoke(target, methodName, Number.class, null);
        return value != null ? value.intValue() : -1;
    }

    private static <T> T invoke(Object target, String methodName, Class<T> type, T fallback) {
        if (target == null) {
            return fallback;
        }

        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return type.isInstance(value) ? type.cast(value) : fallback;
        } catch (ReflectiveOperationException exception) {
            return fallback;
        }
    }

    private static Class<?> loadProblemNodeType() {
        try {
            return Class.forName("com.intellij.analysis.problemsView.toolWindow.ProblemNodeI");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("IntelliJ Problems View API is unavailable", exception);
        }
    }

    private static Method loadGetProblemMethod() {
        try {
            return PROBLEM_NODE_TYPE.getMethod("getProblem");
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("IntelliJ Problems View API has changed", exception);
        }
    }
}
