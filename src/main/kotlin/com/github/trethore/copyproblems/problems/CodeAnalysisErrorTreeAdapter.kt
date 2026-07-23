package com.github.trethore.copyproblems.problems

import com.intellij.ide.errorTreeView.ErrorTreeElement
import com.intellij.ide.errorTreeView.ErrorTreeElementKind
import com.intellij.ide.errorTreeView.NavigatableMessageElement
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import java.awt.Component
import javax.swing.SwingUtilities

/** Adapter for the VCS "Code Analysis" tab shown after commit checks. */
internal object CodeAnalysisErrorTreeAdapter {
    fun findPanel(component: Component?): NewErrorTreeViewPanel? {
        return when (component) {
            is NewErrorTreeViewPanel -> component
            null -> null
            else -> SwingUtilities.getAncestorOfClass(
                NewErrorTreeViewPanel::class.java,
                component,
            ) as? NewErrorTreeViewPanel
        }
    }

    fun selectedElements(panel: NewErrorTreeViewPanel): List<ErrorTreeElement> =
        listOfNotNull(panel.selectedErrorTreeElement)

    fun root(panel: NewErrorTreeViewPanel): ErrorTreeElement =
        panel.errorViewStructure.rootElement as ErrorTreeElement

    fun children(panel: NewErrorTreeViewPanel, element: ErrorTreeElement): List<ErrorTreeElement> =
        panel.errorViewStructure.getChildElements(element)
            .filterIsInstance<ErrorTreeElement>()

    fun isProblem(element: ErrorTreeElement): Boolean = element is NavigatableMessageElement

    fun toCopyableProblem(
        element: ErrorTreeElement,
        displayPath: (com.intellij.openapi.vfs.VirtualFile) -> String,
    ): CopyableProblem {
        val message = element as NavigatableMessageElement
        val descriptor = message.navigatable as? OpenFileDescriptor
        val file = descriptor?.file ?: message.parent?.file
        val severity = when (message.kind) {
            ErrorTreeElementKind.ERROR -> "ERROR"
            ErrorTreeElementKind.WARNING -> "WARNING"
            ErrorTreeElementKind.INFO, ErrorTreeElementKind.NOTE -> "INFORMATION"
            else -> null
        }

        return CopyableProblem(
            path = file?.let(displayPath),
            line = descriptor?.line?.takeIf { it >= 0 }?.plus(1),
            column = descriptor?.column?.takeIf { it >= 0 }?.plus(1),
            severity = severity,
            message = message.text.joinToString(" "),
            severityValue = when (message.kind) {
                ErrorTreeElementKind.ERROR -> com.intellij.lang.annotation.HighlightSeverity.ERROR.myVal
                ErrorTreeElementKind.WARNING -> com.intellij.lang.annotation.HighlightSeverity.WARNING.myVal
                else -> com.intellij.lang.annotation.HighlightSeverity.INFORMATION.myVal
            },
        )
    }
}
