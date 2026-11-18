package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDiscardSelectedTextGitAction
import com.jetpackduba.gitnuro.domain.models.SelectedLineText
import com.jetpackduba.gitnuro.domain.models.TaskType
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class DiscardSelectedTextUseCase @Inject constructor(
    private val discardSelectedTextGitAction: IDiscardSelectedTextGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(diffEntry: DiffEntry, selectedLineTextList: List<SelectedLineText>) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.Unspecified,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            discardSelectedTextGitAction(repositoryPath, diffEntry, selectedLineTextList)
        }
    }
}
