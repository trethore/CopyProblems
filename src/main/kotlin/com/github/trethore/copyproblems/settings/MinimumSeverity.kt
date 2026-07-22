package com.github.trethore.copyproblems.settings

import com.intellij.lang.annotation.HighlightSeverity

enum class MinimumSeverity(
    val displayName: String,
    val value: Int,
) {
    ERROR("Error", HighlightSeverity.ERROR.myVal),
    WARNING("Warning", HighlightSeverity.WARNING.myVal),
    WEAK_WARNING("Weak Warning", HighlightSeverity.WEAK_WARNING.myVal),
    INFORMATION("Information", HighlightSeverity.INFORMATION.myVal),
    ;

    override fun toString() = displayName
}
