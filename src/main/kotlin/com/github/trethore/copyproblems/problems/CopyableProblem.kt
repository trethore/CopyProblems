package com.github.trethore.copyproblems.problems

data class CopyableProblem(
    val path: String?,
    val line: Int?,
    val column: Int?,
    val severity: String?,
    val message: String,
    val severityValue: Int? = null,
)
