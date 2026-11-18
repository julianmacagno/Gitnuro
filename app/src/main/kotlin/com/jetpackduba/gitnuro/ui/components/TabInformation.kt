package com.jetpackduba.gitnuro.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow

@Immutable
class TabInformation<T: TabInformationProvider>(val data: T) {
    val name = data.tabName
    val extraInfo = data.extraInfo
    val isBackgroundTaskRunning = data.isBackgroundTaskRunning
}

interface TabInformationProvider {
    val tabName: StateFlow<String?>
    val extraInfo: StateFlow<String?>
    val isBackgroundTaskRunning: StateFlow<Boolean>

    fun dispose()
}