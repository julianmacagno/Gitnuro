package com.jetpackduba.gitnuro.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import com.jetpackduba.gitnuro.LocalTab

/**
 * A [LazyListState] that outlives its composition (kept in a ViewModel or [rememberInTab] to keep the scroll of hidden
 * tabs) still references the LayoutNode tree it last measured, and with it Skia's native text memory: every hidden tab
 * retained its whole UI. A copy at the same position keeps the scroll without that tree.
 */
fun LazyListState.detachedCopy() = LazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)

@Composable
fun <T : Any> rememberInTab(
    id: String,
    vararg inputs: Any?,
    init: () -> T,
): T {
    val tab = LocalTab.current
    val inputs = inputs.toList()

    return remember(id, inputs) {
        if (tab.savedStates.contains(id)) {
            val pair = tab.savedStates[id]
            if (pair?.first == inputs) {
                return@remember pair.second as T
            }
        }

        val newValue = init()
        tab.savedStates[id] = Pair(inputs, newValue)
        newValue
    }
}