package com.github.trethore.copyproblems.actions

import com.github.trethore.copyproblems.problems.CopyableProblem
import com.github.trethore.copyproblems.problems.ProblemFormatter
import com.github.trethore.copyproblems.problems.ProblemsViewAdapter
import com.github.trethore.copyproblems.settings.CopyProblemsSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction

abstract class CopyProblemsAction : DumbAwareAction() {
    protected abstract fun problems(adapter: ProblemsViewAdapter): List<CopyableProblem>

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(event: AnActionEvent) {
        val project = event.project
        val available = project != null && problems(ProblemsViewAdapter(project)).isNotEmpty()
        event.presentation.isEnabledAndVisible = available
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val text = ProblemFormatter.format(problems(ProblemsViewAdapter(project)))
        if (text.isNotEmpty()) {
            CopyPasteManager.copyTextToClipboard(text)
        }
    }
}

class CopyProblemAction : CopyProblemsAction() {
    override fun problems(adapter: ProblemsViewAdapter) = adapter.selectedProblem()
}

class CopySelectedNodeProblemsAction : CopyProblemsAction() {
    override fun problems(adapter: ProblemsViewAdapter) = adapter.problemsInSelection()
}

class CopyAllVisibleProblemsAction : CopyProblemsAction() {
    override fun problems(adapter: ProblemsViewAdapter) = adapter.allVisibleProblems()
}

class CopyMinimumSeverityAction : CopyProblemsAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.text =
            "Copy Problems at ${CopyProblemsSettings.getInstance().minimumSeverity.displayName} or Above"
        super.update(event)
    }

    override fun problems(adapter: ProblemsViewAdapter): List<CopyableProblem> {
        val threshold = CopyProblemsSettings.getInstance().minimumSeverity.value
        return adapter.allVisibleProblems().filter {
            (it.severityValue ?: Int.MAX_VALUE) >= threshold
        }
    }
}
