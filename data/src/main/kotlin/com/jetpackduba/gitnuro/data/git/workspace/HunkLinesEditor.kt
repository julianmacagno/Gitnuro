package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.models.Hunk
import com.jetpackduba.gitnuro.domain.models.Line
import com.jetpackduba.gitnuro.domain.models.LineType

object HunkLinesEditor {
    fun stage(indexLineList: List<String>, hunk: Hunk, selectedLineList: List<Line>): List<String> {
        val start = hunk.lines.first().oldLineNumber
        val end = start + hunk.lines.count { it.lineType != LineType.ADDED }

        val hunkText = hunk.lines.mapNotNull { line ->
            when (line.lineType) {
                LineType.CONTEXT -> line.text
                LineType.ADDED -> line.text.takeIf { line in selectedLineList }
                LineType.REMOVED -> line.text.takeUnless { line in selectedLineList }
            }
        }

        return indexLineList.subList(0, start) + hunkText + indexLineList.subList(end, indexLineList.size)
    }

    fun unstage(indexLineList: List<String>, hunk: Hunk, selectedLineList: List<Line>): List<String> {
        val start = hunk.lines.first().newLineNumber
        val end = start + hunk.lines.count { it.lineType != LineType.REMOVED }

        val hunkText = hunk.lines.mapNotNull { line ->
            when (line.lineType) {
                LineType.CONTEXT -> line.text
                LineType.ADDED -> line.text.takeUnless { line in selectedLineList }
                LineType.REMOVED -> line.text.takeIf { line in selectedLineList }
            }
        }

        return indexLineList.subList(0, start) + hunkText + indexLineList.subList(end, indexLineList.size)
    }
}
