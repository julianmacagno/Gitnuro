package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.extensions.filePath
import com.jetpackduba.gitnuro.domain.interfaces.IDiscardSelectedTextGitAction
import com.jetpackduba.gitnuro.domain.models.SelectedLineText
import org.eclipse.jgit.diff.DiffEntry
import java.io.File
import javax.inject.Inject

class DiscardSelectedTextGitAction @Inject constructor(
    private val jgit: JGit,
    private val getLinesFromTextGitAction: GetLinesFromTextGitAction,
) : IDiscardSelectedTextGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        diffEntry: DiffEntry,
        selectedLineTextList: List<SelectedLineText>,
    ) = jgit.provide(repositoryPath) { git ->
        try {
            val file = File(git.repository.workTree, diffEntry.filePath)
            val fileLineList = getLinesFromTextGitAction(file.readText(Charsets.UTF_8))

            file.writeText(LineTextEditor.remove(fileLineList, selectedLineTextList).joinToString(""), Charsets.UTF_8)
        } catch (ex: Exception) {
            throw Exception(
                "Discard selected text failed. Check if the file still exists, has not changed and has the write permissions set",
                ex
            )
        }
    }
}
