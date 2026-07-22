package com.github.trethore.copyproblems.problems

import com.intellij.analysis.problemsView.toolWindow.Node
import com.intellij.analysis.problemsView.toolWindow.ProblemsView
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.tree.TreePath

class ProblemsViewAdapter(private val project: Project) {
    private val panel
        get() = ProblemsView.getSelectedPanel(project)

    fun selectedProblem(): List<CopyableProblem> =
        panel?.tree?.selectionPaths
            .orEmpty()
            .map { it.lastPathComponent }
            .filter(ProblemsViewBridge::isProblemNode)
            .map(::toCopyableProblem)

    fun problemsInSelection(): List<CopyableProblem> =
        collect(panel?.tree?.selectionPaths.orEmpty().mapNotNull(::nodeFrom))

    fun allVisibleProblems(): List<CopyableProblem> {
        val root = panel?.treeModel?.root ?: return emptyList()
        return collect(listOf(root))
    }

    private fun collect(nodes: Collection<Node>): List<CopyableProblem> {
        val result = mutableListOf<CopyableProblem>()

        fun visit(node: Node) {
            if (ProblemsViewBridge.isProblemNode(node)) result += toCopyableProblem(node)
            ProblemsViewBridge.children(node).forEach(::visit)
        }

        nodes.forEach(::visit)
        return result
    }

    private fun toCopyableProblem(node: Any): CopyableProblem {
        val file = ProblemsViewBridge.file(node)
        val severityValue = ProblemsViewBridge.severity(node)
            .takeIf { it >= 0 }
            ?: HighlightSeverity.ERROR.myVal

        return CopyableProblem(
            path = file?.let(::displayPath),
            line = ProblemsViewBridge.line(node).takeIf { it >= 0 }?.plus(1),
            column = ProblemsViewBridge.column(node).takeIf { it >= 0 }?.plus(1),
            severity = severityName(severityValue),
            message = ProblemsViewBridge.message(node),
            severityValue = severityValue,
        )
    }

    private fun displayPath(file: VirtualFile): String {
        val projectDir = project.basePath?.let {
            LocalFileSystem.getInstance().findFileByPath(it)
        }
        return projectDir?.let { VfsUtilCore.getRelativePath(file, it, '/') }
            ?: file.presentableUrl
    }

    private fun severityName(value: Int): String? {
        if (value < 0) return null
        listOf(
            HighlightSeverity.ERROR,
            HighlightSeverity.WARNING,
            HighlightSeverity.WEAK_WARNING,
            HighlightSeverity.INFORMATION,
        ).firstOrNull { it.myVal == value }?.let { return it.name }

        return SeverityRegistrar.getSeverityRegistrar(project)
            .allSeverities
            .firstOrNull { it.myVal == value }
            ?.name
    }

    private fun nodeFrom(path: TreePath): Node? = path.lastPathComponent as? Node
}
