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
