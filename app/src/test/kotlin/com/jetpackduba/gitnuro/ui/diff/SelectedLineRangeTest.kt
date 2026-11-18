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

class SelectedTextPieceTest {
    private val lineList = listOf("foo bar", "x = foo", "    foo bar baz", "end")

    @Test
    fun `word inside a line`() {
        assertEquals(listOf(SelectedTextPiece(0, 4, 7)), findSelectedTextPieceList(lineList, "bar", anchorIndex = 0))
    }

    @Test
    fun `repeated text inside the line is ambiguous`() {
        assertNull(findSelectedTextPieceList(listOf("a = a"), "a", anchorIndex = 0))
    }

    @Test
    fun `several lines with partial edges`() {
        val result = findSelectedTextPieceList(lineList, "bar\nx = foo\n    foo", anchorIndex = 1)

        assertEquals(
            listOf(SelectedTextPiece(0, 4, 7), SelectedTextPiece(1, 0, 7), SelectedTextPiece(2, 0, 7)),
            result,
        )
    }

    @Test
    fun `selection ending with a line break needs a complete last line`() {
        assertNull(findSelectedTextPieceList(lineList, "bar\nx\n", anchorIndex = 1))
    }

    @Test
    fun `tabs are mapped back to the raw text`() {
        assertEquals(1 to 4, displayedToRawRange("\tfoo", 4, 7))
        assertEquals(0 to 4, displayedToRawRange("\tfoo", 0, 7))
        assertEquals(1 to 4, displayedToRawRange("\tfoo", 2, 7))
    }
}
