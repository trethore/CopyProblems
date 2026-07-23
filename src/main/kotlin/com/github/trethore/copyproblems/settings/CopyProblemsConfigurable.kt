package com.github.trethore.copyproblems.settings

import com.github.trethore.copyproblems.problems.ProblemFormatter
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
import javax.swing.JTextField

class CopyProblemsConfigurable : Configurable {
    private var minimumSeverityComboBox: ComboBox<MinimumSeverity>? = null
    private var problemTemplateField: JTextField? = null

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

        add(JLabel("Choose how copied problems are formatted").apply {
            font = font.deriveFont(Font.BOLD, font.size2D * 1.15f)
        }, constraints.apply {
            gridy = 3
            insets = Insets(24, 0, 8, 0)
        })

        add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(JLabel("Template:").apply {
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 8)
            })
            add(JTextField(ProblemFormatter.DEFAULT_TEMPLATE, 45).also {
                problemTemplateField = it
            })
        }, constraints.apply {
            gridy = 4
            insets = Insets(0, 0, 4, 0)
        })

        val availableVariables = ProblemFormatter.SUPPORTED_VARIABLES.joinToString { "{{$it}}" }
        add(JLabel("Available variables: $availableVariables"), constraints.apply {
            gridy = 5
            insets = Insets(0, 0, 0, 0)
        })

        add(JPanel(), constraints.apply {
            gridy = 6
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        })

        reset()
    }

    override fun isModified(): Boolean {
        val settings = CopyProblemsSettings.getInstance()
        return minimumSeverityComboBox?.selectedItem != settings.minimumSeverity ||
            problemTemplateField?.text != settings.problemTemplate
    }

    override fun apply() {
        val settings = CopyProblemsSettings.getInstance()
        val selected = minimumSeverityComboBox?.selectedItem as? MinimumSeverity ?: return
        settings.minimumSeverity = selected
        settings.problemTemplate = problemTemplateField?.text ?: ProblemFormatter.DEFAULT_TEMPLATE
    }

    override fun reset() {
        val settings = CopyProblemsSettings.getInstance()
        minimumSeverityComboBox?.selectedItem = settings.minimumSeverity
        problemTemplateField?.text = settings.problemTemplate
    }

    override fun disposeUIResources() {
        minimumSeverityComboBox = null
        problemTemplateField = null
    }
}
