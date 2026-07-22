package com.github.trethore.copyproblems.problems

import com.intellij.analysis.problemsView.toolWindow.Node
import com.intellij.analysis.problemsView.toolWindow.ProblemsView
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.reference.RefElement
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import java.awt.Component
import javax.swing.tree.TreePath

class ProblemsViewAdapter(
    private val project: Project,
    codeAnalysisView: Any? = null,
    contextComponent: Component? = null,
    selectedItems: Array<out Any> = emptyArray(),
) {
    private val codeAnalysisView = codeAnalysisView
        ?: contextComponent?.let(CodeAnalysisViewBridge::findSource)
    private val codeAnalysisSelection = selectedItems
        .filter(CodeAnalysisViewBridge::isTreeNode)
        .ifEmpty {
            codeAnalysisView?.let(CodeAnalysisViewBridge::selectedNodes).orEmpty()
        }
    private val isCodeAnalysis = codeAnalysisView != null || codeAnalysisSelection.isNotEmpty()

    private val panel
        get() = ProblemsView.getSelectedPanel(project)

    fun selectedProblem(): List<CopyableProblem> {
        if (isCodeAnalysis) {
            return codeAnalysisSelection
                .filter(CodeAnalysisViewBridge::isProblemNode)
                .map(::toCodeAnalysisProblem)
        }

        return panel?.tree?.selectionPaths
            .orEmpty()
            .map { it.lastPathComponent }
            .filter(ProblemsViewBridge::isProblemNode)
            .map(::toCopyableProblem)
    }

    fun problemsInSelection(): List<CopyableProblem> {
        if (isCodeAnalysis) {
            return collectCodeAnalysis(codeAnalysisSelection)
        }

        return collect(panel?.tree?.selectionPaths.orEmpty().mapNotNull(::nodeFrom))
    }

    fun allVisibleProblems(): List<CopyableProblem> {
        if (isCodeAnalysis) {
            val root = codeAnalysisView?.let(CodeAnalysisViewBridge::root)
                ?: codeAnalysisSelection.firstOrNull()?.let(CodeAnalysisViewBridge::rootFrom)
            return root?.let { collectCodeAnalysis(listOf(it)) }.orEmpty()
        }

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

    private fun collectCodeAnalysis(nodes: Collection<Any>): List<CopyableProblem> {
        val result = mutableListOf<CopyableProblem>()

        fun visit(node: Any) {
            if (CodeAnalysisViewBridge.isProblemNode(node)) {
                result += toCodeAnalysisProblem(node)
            }
            CodeAnalysisViewBridge.children(node).forEach(::visit)
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

    private fun toCodeAnalysisProblem(node: Any): CopyableProblem {
        val descriptor = CodeAnalysisViewBridge.descriptor(node)
        val psiElement = (descriptor as? ProblemDescriptor)?.psiElement
            ?: (CodeAnalysisViewBridge.element(node) as? RefElement)?.psiElement
        val location = psiElement?.let {
            location(it, descriptor as? ProblemDescriptor)
        }
        val severity = CodeAnalysisViewBridge.level(node)?.severity
            ?: HighlightSeverity.ERROR

        return CopyableProblem(
            path = location?.file?.let(::displayPath),
            line = location?.line,
            column = location?.column,
            severity = severityName(severity.myVal),
            message = CodeAnalysisViewBridge.message(node),
            severityValue = severity.myVal,
        )
    }

    private fun location(element: PsiElement, descriptor: ProblemDescriptor?): ProblemLocation? {
        if (!element.isValid) return null

        val injectionManager = InjectedLanguageManager.getInstance(project)
        val file = injectionManager.getTopLevelFile(element)
        val virtualFile = file.virtualFile ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(file)
            ?: return ProblemLocation(virtualFile, null, null)
        val elementRange = element.textRange ?: return ProblemLocation(virtualFile, null, null)
        val descriptorRange = descriptor?.textRangeInElement
            ?.takeIf { it.endOffset <= elementRange.length }
            ?.shiftRight(elementRange.startOffset)
            ?: elementRange
        val hostRange = injectionManager.injectedToHost(element, descriptorRange)
        val offset = hostRange.startOffset.coerceIn(0, document.textLength)
        val lineIndex = document.getLineNumber(offset)

        return ProblemLocation(
            file = virtualFile,
            line = lineIndex + 1,
            column = offset - document.getLineStartOffset(lineIndex) + 1,
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

    private data class ProblemLocation(
        val file: VirtualFile,
        val line: Int?,
        val column: Int?,
    )
}
