package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.models.Line
import com.jetpackduba.gitnuro.domain.models.LineType
import com.jetpackduba.gitnuro.domain.models.SelectedLineText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LineTextEditorTest {
    private val fileLineList = listOf("a\n", "foo bar baz\n", "c\r\n", "last")

    private fun added(text: String, newLineNumber: Int) = Line(text, 0, newLineNumber, LineType.ADDED)

    @Test
    fun `removes a word in the middle of a line`() {
        val result = LineTextEditor.remove(fileLineList, listOf(SelectedLineText(added("foo bar baz\n", 1), 4, 8)))

        assertEquals(listOf("a\n", "foo baz\n", "c\r\n", "last"), result)
    }

    @Test
    fun `keeps the original line delimiter`() {
        val result = LineTextEditor.remove(fileLineList, listOf(SelectedLineText(added("c\r\n", 2), 0, 0)))

        assertEquals(fileLineList, result)
    }

    @Test
    fun `removes the whole line when all its text is selected`() {
        val result = LineTextEditor.remove(fileLineList, listOf(SelectedLineText(added("foo bar baz\n", 1), 0, 11)))

        assertEquals(listOf("a\n", "c\r\n", "last"), result)
    }

    @Test
    fun `removes text from several lines`() {
        val result = LineTextEditor.remove(
            fileLineList,
            listOf(
                SelectedLineText(added("foo bar baz\n", 1), 8, 11),
                SelectedLineText(added("c\r\n", 2), 0, 1),
                SelectedLineText(added("last", 3), 2, 4),
            )
        )

        assertEquals(listOf("a\n", "foo bar \n", "la"), result)
    }

    @Test
    fun `fails when the file changed after the diff was loaded`() {
        assertThrows(IllegalStateException::class.java) {
            LineTextEditor.remove(fileLineList, listOf(SelectedLineText(added("other\n", 1), 0, 2)))
        }
    }

    @Test
    fun `fails for lines that are not added`() {
        val removed = Line("foo bar baz\n", 1, 1, LineType.REMOVED)

        assertThrows(IllegalStateException::class.java) {
            LineTextEditor.remove(fileLineList, listOf(SelectedLineText(removed, 0, 2)))
        }
    }
}
