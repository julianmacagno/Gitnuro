package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IOpenRepositoryGitAction
import javax.inject.Inject

/**
 * Keeps only the directories that can be opened as repositories. If a single directory is given, or none is valid,
 * the first one is kept so the failure is reported when opening it.
 */
class FilterValidRepositoriesUseCase @Inject constructor(
    private val openRepositoryGitAction: IOpenRepositoryGitAction,
) {
    suspend operator fun invoke(directories: List<String>): List<String> {
        if (directories.size <= 1) return directories

        return directories
            .filter { openRepositoryGitAction(it) is Either.Ok }
            .ifEmpty { directories.take(1) }
    }
}
