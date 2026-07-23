package com.github.trethore.copyproblems.problems

object ProblemFormatter {
    const val DEFAULT_TEMPLATE = "{{path}} {{line}}:{{col}} [{{level}}] {{desc}}"
    val SUPPORTED_VARIABLES = listOf("path", "line", "col", "level", "desc")

    private val variablePattern = Regex("\\{\\{(${SUPPORTED_VARIABLES.joinToString("|")})}}")

    fun format(
        problems: Collection<CopyableProblem>,
        template: String = DEFAULT_TEMPLATE,
    ): String = problems.joinToString(separator = "\n") { format(it, template) }

    fun format(
        problem: CopyableProblem,
        template: String = DEFAULT_TEMPLATE,
    ): String {
        var normalizedMessage: String? = null
        return variablePattern.replace(template) { match ->
            when (match.groupValues[1]) {
                "path" -> problem.path.orEmpty()
                "line" -> problem.line?.toString().orEmpty()
                "col" -> problem.column?.toString().orEmpty()
                "level" -> problem.severity.orEmpty()
                "desc" -> normalizedMessage ?: normalizeMessage(problem.message).also {
                    normalizedMessage = it
                }
                else -> match.value
            }
        }.trim()
    }

    private fun normalizeMessage(message: String): String = message
        .replace(Regex("(?i)<br\\s*/?>"), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()
}
