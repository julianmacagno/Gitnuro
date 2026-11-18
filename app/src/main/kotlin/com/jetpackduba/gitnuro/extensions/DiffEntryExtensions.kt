package com.jetpackduba.gitnuro.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import com.jetpackduba.gitnuro.theme.addFile
import com.jetpackduba.gitnuro.theme.conflictFile
import com.jetpackduba.gitnuro.theme.modifyFile
import org.eclipse.jgit.diff.DiffEntry
import org.jetbrains.compose.resources.DrawableResource

val StatusEntry.icon: DrawableResource
    get() = statusType.icon

val StatusEntry.iconColor: Color
    @Composable
    get() = statusType.iconColor



val StatusType.icon: DrawableResource
    get() {
        return when (this) {
            StatusType.ADDED -> Res.drawable.add
            StatusType.MODIFIED -> Res.drawable.edit_filled
            StatusType.REMOVED -> Res.drawable.delete_filled
            StatusType.CONFLICTING -> Res.drawable.warning
        }
    }

val DiffEntry.icon: DrawableResource
    get() {
        return when (this.changeType) {
            DiffEntry.ChangeType.ADD -> Res.drawable.add
            DiffEntry.ChangeType.MODIFY -> Res.drawable.edit_filled
            DiffEntry.ChangeType.DELETE -> Res.drawable.delete_filled
            DiffEntry.ChangeType.COPY -> Res.drawable.add
            DiffEntry.ChangeType.RENAME -> Res.drawable.refresh
            else -> throw NotImplementedError("Unexpected ChangeType")
        }
    }

val StatusType.iconColor: Color
    @Composable
    get() {
        return when (this) {
            StatusType.ADDED -> MaterialTheme.colors.addFile
            StatusType.MODIFIED -> MaterialTheme.colors.modifyFile
            StatusType.REMOVED -> MaterialTheme.colors.error
            StatusType.CONFLICTING -> MaterialTheme.colors.conflictFile
        }
    }

val DiffEntry.iconColor: Color
    @Composable
    get() {
        return when (this.changeType) {
            DiffEntry.ChangeType.ADD -> MaterialTheme.colors.addFile
            DiffEntry.ChangeType.MODIFY -> MaterialTheme.colors.modifyFile
            DiffEntry.ChangeType.DELETE -> MaterialTheme.colors.error
            DiffEntry.ChangeType.COPY -> MaterialTheme.colors.addFile
            DiffEntry.ChangeType.RENAME -> MaterialTheme.colors.modifyFile
            else -> throw NotImplementedError("Unexpected ChangeType")
        }
    }

fun DiffEntry.toStatusType(): StatusType = when (this.changeType) {
    DiffEntry.ChangeType.ADD -> StatusType.ADDED
    DiffEntry.ChangeType.MODIFY -> StatusType.MODIFIED
    DiffEntry.ChangeType.DELETE -> StatusType.REMOVED
    DiffEntry.ChangeType.COPY -> StatusType.ADDED
    DiffEntry.ChangeType.RENAME -> StatusType.MODIFIED
    else -> throw NotImplementedError("Unexpected ChangeType")
}