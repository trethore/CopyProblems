package com.github.trethore.copyproblems.problems

object ProblemFormatter {
    fun format(problems: Collection<CopyableProblem>): String =
        problems.joinToString(separator = "\n", transform = ::format)

    fun format(problem: CopyableProblem): String = buildString {
        problem.path?.let { path ->
            append(path)
            problem.line?.let { line ->
                append(':').append(line)
                problem.column?.let { column -> append(':').append(column) }
            }
            append(' ')
        }

        problem.severity?.let { append('[').append(it).append("] ") }
        append(normalizeMessage(problem.message))
    }.trim()

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
