package com.github.trethore.copyproblems.problems

import org.junit.Assert.assertEquals
import org.junit.Test

class ProblemFormatterTest {
    @Test
    fun `formats a complete problem`() {
        val problem = CopyableProblem(
            path = "src/Foo.kt",
            line = 12,
            column = 18,
            severity = "ERROR",
            message = "Unresolved reference: bar",
        )

        assertEquals(
            "src/Foo.kt:12:18 [ERROR] Unresolved reference: bar",
            ProblemFormatter.format(problem),
        )
    }

    @Test
    fun `normalizes multiline messages`() {
        val problem = CopyableProblem(
            path = "src/Foo.kt",
            line = 3,
            column = null,
            severity = "WARNING",
            message = "First line\n  second line",
        )

        assertEquals(
            "src/Foo.kt:3 [WARNING] First line second line",
            ProblemFormatter.format(problem),
        )
    }

    @Test
    fun `normalizes html descriptions`() {
        val problem = CopyableProblem(
            path = "src/Foo.kt",
            line = 3,
            column = 2,
            severity = "ERROR",
            message = "<html>Expected <b>&lt;expression&gt;</b><br/>but found nothing</html>",
        )

        assertEquals(
            "src/Foo.kt:3:2 [ERROR] Expected <expression> but found nothing",
            ProblemFormatter.format(problem),
        )
    }

    @Test
    fun `formats problems without locations`() {
        val problem = CopyableProblem(
            path = null,
            line = null,
            column = null,
            severity = null,
            message = "Project problem",
        )

        assertEquals("Project problem", ProblemFormatter.format(problem))
    }

    @Test
    fun `joins multiple problems with newlines`() {
        val problems = listOf(
            CopyableProblem("A.kt", 1, 2, "ERROR", "First"),
            CopyableProblem("B.kt", 3, 4, "WARNING", "Second"),
        )

        assertEquals(
            "A.kt:1:2 [ERROR] First\nB.kt:3:4 [WARNING] Second",
            ProblemFormatter.format(problems),
        )
    }
}
