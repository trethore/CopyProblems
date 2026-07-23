package com.github.trethore.copyproblems.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class CopyProblemsConfigurable : Configurable {
    private var minimumSeverityComboBox: ComboBox<MinimumSeverity>? = null

    override fun getDisplayName() = "CopyProblems"

    override fun createComponent(): JComponent = JPanel(GridBagLayout()).apply {
        val constraints = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            gridx = 0
            weightx = 1.0
        }

        add(JLabel("CopyProblems").apply {
            font = font.deriveFont(Font.BOLD, font.size2D * 1.6f)
        }, constraints.apply {
            gridy = 0
            insets = Insets(0, 0, 16, 0)
        })

        add(JLabel("Choose the minimum severity of problems to copy").apply {
            font = font.deriveFont(Font.BOLD, font.size2D * 1.15f)
        }, constraints.apply {
            gridy = 1
            insets = Insets(0, 0, 8, 0)
        })

        add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(JLabel("Minimum severity:").apply {
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 8)
            })
            add(ComboBox(MinimumSeverity.entries.toTypedArray()).also {
                minimumSeverityComboBox = it
            })
        }, constraints.apply {
            gridy = 2
            insets = Insets(0, 0, 0, 0)
        })

        add(JPanel(), constraints.apply {
            gridy = 3
            weighty = 1.0
            fill = GridBagConstraints.BOTH
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
