package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageHunkLineGitAction
import com.jetpackduba.gitnuro.domain.models.Hunk
import com.jetpackduba.gitnuro.domain.models.Line
import com.jetpackduba.gitnuro.domain.models.TaskType
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class UnstageHunkLineUseCase @Inject constructor(
    private val unstageHunkLineGitAction: IUnstageHunkLineGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(diffEntry: DiffEntry, hunkLineList: List<Pair<Hunk, List<Line>>>) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.UnstageLine,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            hunkLineList.asReversed().forEach { (hunk, lineList) ->
                unstageHunkLineGitAction(repositoryPath, diffEntry, hunk, lineList).bind()
            }

            Either.Ok(Unit)
        }
    }
}
