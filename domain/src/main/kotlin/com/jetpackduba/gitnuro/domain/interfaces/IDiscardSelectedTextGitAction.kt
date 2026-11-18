package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.SelectedLineText
import org.eclipse.jgit.diff.DiffEntry

interface IDiscardSelectedTextGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        diffEntry: DiffEntry,
        selectedLineTextList: List<SelectedLineText>,
    ): Either<Unit, GitError>
}
