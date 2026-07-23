package com.github.trethore.copyproblems.settings

import com.github.trethore.copyproblems.problems.ProblemFormatter
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "CopyProblemsSettings", storages = [Storage("copyProblems.xml")])
class CopyProblemsSettings : PersistentStateComponent<CopyProblemsSettings.SettingsState> {
    data class SettingsState(
        var minimumSeverity: MinimumSeverity = MinimumSeverity.WARNING,
        var problemTemplate: String = ProblemFormatter.DEFAULT_TEMPLATE,
    )

    private var settingsState = SettingsState()

    var minimumSeverity: MinimumSeverity
        get() = settingsState.minimumSeverity
        set(value) {
            settingsState.minimumSeverity = value
        }

    var problemTemplate: String
        get() = settingsState.problemTemplate
        set(value) {
            settingsState.problemTemplate = value
        }

    override fun getState() = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = state
    }

    companion object {
        fun getInstance(): CopyProblemsSettings = service()
    }
}
