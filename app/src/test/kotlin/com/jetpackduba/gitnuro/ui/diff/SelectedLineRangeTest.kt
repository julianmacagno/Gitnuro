package com.jetpackduba.gitnuro.ui.diff

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SelectedLineRangeTest {
    private val lineList =
        listOf("fun a() {", "    val x = 1", "    val y = 2", "}", "", "fun b() {", "    val x = 1", "}")

    @Test
    fun `part of a single line`() {
        assertEquals(1..1, findSelectedLineRange(lineList, "val x", anchorIndex = 1))
    }

    @Test
    fun `single line is the right-clicked one when text is repeated`() {
        assertEquals(6..6, findSelectedLineRange(lineList, "val x = 1", anchorIndex = 6))
    }

    @Test
    fun `incomplete first and last lines`() {
        assertEquals(1..2, findSelectedLineRange(lineList, "x = 1\n    val", anchorIndex = 2))
    }

    @Test
    fun `incomplete edges with full lines in the middle`() {
        assertEquals(0..3, findSelectedLineRange(lineList, "a() {\n    val x = 1\n    val y = 2\n}", anchorIndex = 1))
        assertEquals(2..5, findSelectedLineRange(lineList, "val y = 2\n}\n\nfun", anchorIndex = 3))
    }

    @Test
    fun `selection that only touches the edge of a line doesn't select it`() {
        assertEquals(1..2, findSelectedLineRange(lineList, "\n    val x = 1\n    val y = 2\n", anchorIndex = 1))
    }

    @Test
    fun `repeated blocks are disambiguated with the right-clicked line`() {
        val repeatedLineList = listOf("a", "b", "x", "a", "b")

        assertEquals(0..1, findSelectedLineRange(repeatedLineList, "a\nb", anchorIndex = 1))
        assertEquals(3..4, findSelectedLineRange(repeatedLineList, "a\nb", anchorIndex = 4))
    }

    @Test
    fun `no match returns null`() {
        assertNull(findSelectedLineRange(lineList, "not in the diff\nat all", anchorIndex = 1))
        assertNull(findSelectedLineRange(lineList, "val x", anchorIndex = 99))
    }
}
