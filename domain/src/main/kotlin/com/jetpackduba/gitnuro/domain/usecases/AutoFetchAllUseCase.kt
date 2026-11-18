package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IFetchAllRemotesGitAction
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import com.jetpackduba.gitnuro.domain.services.GlobalSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

private val AUTO_FETCH_INTERVAL_MILLIS = 10.minutes.inWholeMilliseconds

@TabScope
class AutoFetchAllUseCase @Inject constructor(
    private val globalSettingsService: GlobalSettingsService,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val fetchAllGitAction: IFetchAllRemotesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    private var lastAutoFetchTimestamp = 0L

    suspend operator fun invoke() {
        if (repositoryDataRepository.repositorySelectionState.value !is RepositorySelectionState.Open) return
        if (repositoryStateRepository.currentTask.value != null) return
        if (!globalSettingsService.autoFetchAll.first()) return

        val now = System.currentTimeMillis()
        val lastFetchTimestamp = repositoryStateRepository.completedTasks.value
            .lastOrNull { it.taskType == TaskType.Fetch }
            ?.date ?: 0L

        if (now - maxOf(lastAutoFetchTimestamp, lastFetchTimestamp) < AUTO_FETCH_INTERVAL_MILLIS) return

        lastAutoFetchTimestamp = now

        useCaseExecutor.executeLaunch(
            taskType = TaskType.Fetch,
            refreshEvenIfFailed = true,
            isForegroundTask = false,
            dataToRefresh = arrayOf(DataToRefresh.LOG),
        ) { repositoryPath ->
            fetchAllGitAction(repositoryPath, null)
        }
    }
}
