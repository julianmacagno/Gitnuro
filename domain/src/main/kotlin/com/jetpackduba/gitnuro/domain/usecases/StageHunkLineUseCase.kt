package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IStageHunkLineGitAction
import com.jetpackduba.gitnuro.domain.models.Hunk
import com.jetpackduba.gitnuro.domain.models.Line
import com.jetpackduba.gitnuro.domain.models.TaskType
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class StageHunkLineUseCase @Inject constructor(
    private val stageHunkLineGitAction: IStageHunkLineGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(diffEntry: DiffEntry, hunkLineList: List<Pair<Hunk, List<Line>>>) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.StageLine,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            hunkLineList.asReversed().forEach { (hunk, lineList) ->
                stageHunkLineGitAction(repositoryPath, diffEntry, hunk, lineList).bind()
            }

            Either.Ok(Unit)
        }
    }
}
