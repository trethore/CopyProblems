package com.github.trethore.copyproblems.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class CopyProblemsConfigurable : Configurable {
    private var minimumSeverityComboBox: ComboBox<MinimumSeverity>? = null

    override fun getDisplayName() = "CopyProblems"

    override fun createComponent(): JComponent = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(JLabel("Minimum severity:"))
        add(ComboBox(MinimumSeverity.entries.toTypedArray()).also {
            minimumSeverityComboBox = it
        })
        reset()
    }

    override fun isModified(): Boolean =
        minimumSeverityComboBox?.selectedItem != CopyProblemsSettings.getInstance().minimumSeverity

    override fun apply() {
        val selected = minimumSeverityComboBox?.selectedItem as? MinimumSeverity ?: return
        CopyProblemsSettings.getInstance().minimumSeverity = selected
    }

    override fun reset() {
        minimumSeverityComboBox?.selectedItem = CopyProblemsSettings.getInstance().minimumSeverity
    }

    override fun disposeUIResources() {
        minimumSeverityComboBox = null
    }
}
