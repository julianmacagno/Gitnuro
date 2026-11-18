package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.models.Hunk
import com.jetpackduba.gitnuro.domain.models.Line
import com.jetpackduba.gitnuro.domain.models.LineType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HunkLinesEditorTest {
    private val indexLineList = listOf("a\n", "b\n", "c\n", "d\n", "e\n")
    private val stagedLineList = listOf("a\n", "X\n", "Y\n", "c\n", "e\n")

    private val contextA = Line("a\n", 0, 0, LineType.CONTEXT)
    private val removedB = Line("b\n", 1, 1, LineType.REMOVED)
    private val addedX = Line("X\n", 2, 1, LineType.ADDED)
    private val addedY = Line("Y\n", 2, 2, LineType.ADDED)
    private val contextC = Line("c\n", 2, 3, LineType.CONTEXT)
    private val removedD = Line("d\n", 3, 4, LineType.REMOVED)
    private val contextE = Line("e\n", 4, 4, LineType.CONTEXT)

    private val hunk = Hunk("@@ -1,5 +1,5 @@", listOf(contextA, removedB, addedX, addedY, contextC, removedD, contextE))
    private val changedLineList = hunk.lines.filter { it.lineType != LineType.CONTEXT }

    @Test
    fun `stage with nothing selected keeps the index`() {
        assertEquals(indexLineList, HunkLinesEditor.stage(indexLineList, hunk, emptyList()))
    }

    @Test
    fun `stage with everything selected gives the working tree`() {
        assertEquals(stagedLineList, HunkLinesEditor.stage(indexLineList, hunk, changedLineList))
    }

    @Test
    fun `stage only some lines`() {
        val result = HunkLinesEditor.stage(indexLineList, hunk, listOf(removedD, addedX))

        assertEquals(listOf("a\n", "b\n", "X\n", "c\n", "e\n"), result)
    }

    @Test
    fun `stage keeps the file outside the hunk`() {
        val smallHunk = Hunk("@@ -4,2 +4,1 @@", listOf(removedD, contextE))
        val result = HunkLinesEditor.stage(indexLineList, smallHunk, listOf(removedD))

        assertEquals(listOf("a\n", "b\n", "c\n", "e\n"), result)
    }

    @Test
    fun `unstage only some lines`() {
        val result = HunkLinesEditor.unstage(stagedLineList, hunk, listOf(addedY, removedD))

        assertEquals(listOf("a\n", "X\n", "c\n", "d\n", "e\n"), result)
    }

    @Test
    fun `unstage with everything selected gives the original`() {
        assertEquals(indexLineList, HunkLinesEditor.unstage(stagedLineList, hunk, changedLineList))
    }
}
