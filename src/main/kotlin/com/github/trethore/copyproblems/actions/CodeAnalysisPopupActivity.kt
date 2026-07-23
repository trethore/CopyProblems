package com.github.trethore.copyproblems.actions

import com.github.trethore.copyproblems.problems.CodeAnalysisErrorTreeAdapter
import com.intellij.ide.DataManager
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Container
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.MenuSelectionManager
import javax.swing.SwingUtilities

/**
 * Adds CopyProblems actions to the VCS Code Analysis popup, which IntelliJ
 * builds from an unregistered action group that plugin.xml cannot extend.
 */
internal class CodeAnalysisPopupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val listener = AWTEventListener(::handleEvent)
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK)
        Disposer.register(project) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
        }
    }

    private fun handleEvent(event: AWTEvent) {
        val mouseEvent = event as? MouseEvent ?: return
        val isContextMenuEvent = mouseEvent.isPopupTrigger ||
            (SwingUtilities.isRightMouseButton(mouseEvent) &&
                mouseEvent.id in setOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED))
        if (!isContextMenuEvent) return

        val panel = CodeAnalysisErrorTreeAdapter.findPanel(mouseEvent.component) ?: return
        SwingUtilities.invokeLater { augmentCurrentPopup(panel, mouseEvent.component) }
    }

    private fun augmentCurrentPopup(panel: NewErrorTreeViewPanel, invoker: Component) {
        val popup = MenuSelectionManager.defaultManager().selectedPath
            .mapNotNull { it.component as? JPopupMenu }
            .lastOrNull()
            ?: visiblePopups().lastOrNull { popup ->
                popup.invoker === invoker || SwingUtilities.isDescendingFrom(invoker, popup.invoker)
            }
            ?: return
        if (popup.getClientProperty(POPUP_MARKER) == true) return

        popup.putClientProperty(POPUP_MARKER, true)
        val actionGroup = popup.actionGroup()
        val copyProblemsGroup = ActionManager.getInstance()
            .getAction("CopyProblems.PopupGroup")

        if (actionGroup is DefaultActionGroup && copyProblemsGroup != null) {
            // Adding Swing menu items directly does not work because
            // ActionPopupMenu rebuilds them from this backing group.
            actionGroup.addSeparator()
            actionGroup.add(copyProblemsGroup)
            reopen(popup, invoker)
        } else {
            popup.addSeparator()
            ACTION_IDS.forEach { actionId ->
                val action = ActionManager.getInstance().getAction(actionId) ?: return@forEach
                popup.add(createMenuItem(action, panel))
            }
            popup.revalidate()
            popup.repaint()
        }
    }

    // ActionPopupMenu does not expose its dynamically-created backing group.
    private fun JPopupMenu.actionGroup(): ActionGroup? =
        runCatching {
            generateSequence(javaClass as Class<*>?) { it.superclass }
                .flatMap { it.declaredFields.asSequence() }
                .firstOrNull { ActionGroup::class.java.isAssignableFrom(it.type) }
                ?.let { field ->
                    field.isAccessible = true
                    field.get(this) as? ActionGroup
                }
        }.getOrNull()

    private fun reopen(popup: JPopupMenu, invoker: Component) {
        val popupLocation = popup.locationOnScreen
        val invokerLocation = invoker.locationOnScreen
        popup.isVisible = false
        popup.show(
            invoker,
            popupLocation.x - invokerLocation.x,
            popupLocation.y - invokerLocation.y,
        )
    }

    private fun visiblePopups(): Sequence<JPopupMenu> = Window.getWindows()
        .asSequence()
        .flatMap(::descendants)
        .filterIsInstance<JPopupMenu>()
        .filter { it.isShowing }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) {
            component.components.forEach { yieldAll(descendants(it)) }
        }
    }

    private fun createMenuItem(action: AnAction, panel: NewErrorTreeViewPanel): JMenuItem =
        JMenuItem(action.templatePresentation.text).apply {
            addActionListener {
                val dataContext = DataManager.getInstance().getDataContext(panel)
                val event = AnActionEvent.createEvent(
                    action,
                    dataContext,
                    action.templatePresentation.clone(),
                    "CompilerMessagesPopup",
                    ActionUiKind.POPUP,
                    null,
                )
                ActionUtil.performAction(action, event)
            }
        }

    private companion object {
        const val POPUP_MARKER = "CopyProblems.CodeAnalysisPopup"
        val ACTION_IDS = listOf(
            "CopyProblems.CopyProblem",
            "CopyProblems.CopySelection",
            "CopyProblems.CopyAllVisible",
            "CopyProblems.CopyMinimumSeverity",
        )
    }
}
