package com.jetpackduba.gitnuro.ui.diff

internal fun findSelectedLineRange(displayedLineList: List<String>, selectedText: String, anchorIndex: Int): IntRange? {
    if (anchorIndex !in displayedLineList.indices) return null

    val fragmentList = selectedText.replace("\r", "").split("\n").toMutableList()

    if (fragmentList.size > 1 && fragmentList.last().isEmpty()) fragmentList.removeLast()
    if (fragmentList.size > 1 && fragmentList.first().isEmpty()) fragmentList.removeFirst()

    if (fragmentList.size == 1) {
        return if (fragmentList.first() in displayedLineList[anchorIndex]) anchorIndex..anchorIndex else null
    }

    val lastFragmentIndex = fragmentList.lastIndex
    val firstStart = maxOf(0, anchorIndex - lastFragmentIndex)

    for (start in firstStart..anchorIndex) {
        val end = start + lastFragmentIndex
        if (end > displayedLineList.lastIndex) break

        val matches = displayedLineList[start].endsWith(fragmentList.first()) &&
                displayedLineList[end].startsWith(fragmentList.last()) &&
                (1 until lastFragmentIndex).all { displayedLineList[start + it] == fragmentList[it] }

        if (matches) return start..end
    }

    return null
}

internal data class SelectedTextPiece(val lineIndex: Int, val start: Int, val end: Int)

internal fun findSelectedTextPieceList(
    displayedLineList: List<String>,
    selectedText: String,
    anchorIndex: Int,
): List<SelectedTextPiece>? {
    val range = findSelectedLineRange(displayedLineList, selectedText, anchorIndex) ?: return null
    val fragmentList = selectedText.replace("\r", "").split("\n").toMutableList()
    val endsWithLineBreak = fragmentList.size > 1 && fragmentList.last().isEmpty()

    if (endsWithLineBreak) fragmentList.removeLast()
    if (fragmentList.size > 1 && fragmentList.first().isEmpty()) fragmentList.removeFirst()

    if (fragmentList.size == 1) {
        val line = displayedLineList[range.first]
        val fragment = fragmentList.first()
        val start = line.indexOf(fragment)

        if (fragment.isEmpty() || start != line.lastIndexOf(fragment)) return null

        return listOf(SelectedTextPiece(range.first, start, start + fragment.length))
    }

    if (endsWithLineBreak && displayedLineList[range.last].length != fragmentList.last().length) return null

    return range.mapIndexed { position, lineIndex ->
        val lineLength = displayedLineList[lineIndex].length

        when (position) {
            0 -> SelectedTextPiece(lineIndex, lineLength - fragmentList.first().length, lineLength)
            fragmentList.lastIndex -> SelectedTextPiece(lineIndex, 0, fragmentList.last().length)
            else -> SelectedTextPiece(lineIndex, 0, lineLength)
        }
    }
}

internal fun displayedToRawRange(rawText: String, displayedStart: Int, displayedEnd: Int): Pair<Int, Int>? {
    var position = 0
    var rawStart = -1
    var rawEnd = -1

    rawText.forEachIndexed { index, char ->
        val width = if (char == '\t') TAB_DISPLAY_WIDTH else 1

        if (position >= displayedStart && position + width <= displayedEnd) {
            if (rawStart < 0) rawStart = index
            rawEnd = index + 1
        }

        position += width
    }

    return if (rawStart < 0) null else rawStart to rawEnd
}

private const val TAB_DISPLAY_WIDTH = 4
