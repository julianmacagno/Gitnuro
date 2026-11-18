package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class GraphCommits(
    val commits: LinkedHashMap<String, GraphCommit> = LinkedHashMap(),
    val maxLane: Int = 0,
) : Map<String, GraphCommit> by commits