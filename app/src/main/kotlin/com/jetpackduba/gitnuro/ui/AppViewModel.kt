package com.jetpackduba.gitnuro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.domain.extensions.removeGitSuffix
import com.jetpackduba.gitnuro.domain.repositories.GlobalSettingsRepository
import com.jetpackduba.gitnuro.domain.repositories.TabsOpenRepository
import com.jetpackduba.gitnuro.domain.usecases.CleanRepositoriesResourcesUseCase
import com.jetpackduba.gitnuro.ui.components.TabInformation
import com.jetpackduba.gitnuro.viewmodels.RepositoryTabViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppViewModel @Inject constructor(
    private val tabsOpenRepository: TabsOpenRepository,
    private val tabComponentFactory: TabComponent.Factory,
    private val cleanRepositoriesResourcesUseCase: CleanRepositoriesResourcesUseCase,
) : ViewModel() {
    val tabs: StateFlow<List<TabInformation<RepositoryTabViewModel>>>
        field = MutableStateFlow<List<TabInformation<RepositoryTabViewModel>>>(emptyList())

    val currentTab: StateFlow<TabInformation<RepositoryTabViewModel>?>
        field = MutableStateFlow<TabInformation<RepositoryTabViewModel>?>(null)

    val groups: StateFlow<Map<String, List<String>>>
        field = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val activeGroup: StateFlow<String>
        field = MutableStateFlow("")

    private var groupSelectedTabIndexMap = emptyMap<String, Int>()

    suspend fun loadPersistedTabs() {
        activeGroup.value = tabsOpenRepository.activeGroup.first()
        groupSelectedTabIndexMap = tabsOpenRepository.groupSelectedTabIndexMap.first()
        groups.value = tabsOpenRepository.groups.first().let {
            if (activeGroup.value in it) it else mapOf(activeGroup.value to emptyList<String>()) + it
        }

        val repositoriesSaved = tabsOpenRepository.openTabs.firstOrNull().orEmpty()
        val repositoriesList = repositoriesSaved.map { path ->
            newAppTab2(
                path = path,
            )
        }

        tabs.value = repositoriesList.ifEmpty { listOf(newAppTab2()) }

        val latestSelectedTabIndex = tabsOpenRepository.selectedTabIndex.firstOrNull() ?: -1

        currentTab.value = when (latestSelectedTabIndex < 0) {
            true -> tabs.value.first()
            false -> tabs.value.getOrNull(latestSelectedTabIndex) ?: tabs.value.first()
        }
    }

    /**
     * Tab of the active group that already has [path] open.
     * ponytail: plain string compare, symlinked paths to the same repo are not detected.
     */
    fun findOpenTab(path: String): TabInformation<RepositoryTabViewModel>? {
        val normalizedPath = path.removeGitSuffix()

        return tabs.value.firstOrNull { it.data.repositoryPath.value == normalizedPath }
    }

    suspend fun addNewTabFromPath(path: String, selectTab: Boolean, tabToBeReplacedPath: RepositoryTabViewModel? = null) {
        val openTab = findOpenTab(path)

        if (openTab != null) {
            if (selectTab) selectTab(openTab)
            return
        }

        val tabToBeReplaced = tabs
            .value
            .firstOrNull {
                it.data == tabToBeReplacedPath
            }

        val newTab = newAppTab2(
            path = path,
        )

        tabs.update {
            val newTabsList = it.toMutableList()

            if (tabToBeReplaced != null) {
                val index = newTabsList.indexOf(tabToBeReplaced)
                newTabsList[index] = newTab
                tabToBeReplaced.data.dispose()
            } else {
                newTabsList.add(newTab)
            }

            newTabsList
        }

        if (selectTab) {
            currentTab.value = newTab
        }

        if (tabToBeReplaced != null) releaseUnusedRepositories()

        updatePersistedTabs()
    }

    /** Closes the cached JGit instances of repositories that no loaded tab shows anymore. */
    private fun releaseUnusedRepositories() {
        cleanRepositoriesResourcesUseCase(
            tabs.value.mapNotNull { if (it.data.isLoaded) it.data.repositoryPath.value else null }
        )
    }

    fun selectTab(tab: TabInformation<RepositoryTabViewModel>) {
        currentTab.value = tab

        viewModelScope.launch {
            updatePersistedTabs()
        }
    }

    fun closeTab(tab: TabInformation<RepositoryTabViewModel>) = viewModelScope.launch {
        val tabsList = tabs.value.toMutableList()
        var newCurrentTab: TabInformation<RepositoryTabViewModel>? = null
        tab.data.dispose()

        if (currentTab.value == tab) {
            val index = tabsList.indexOf(tab)

            if (tabsList.count() == 1) {
                newCurrentTab = newAppTab2()
            } else if (index > 0) {
                newCurrentTab = tabsList[index - 1]
            } else if (index == 0) {
                newCurrentTab = tabsList[1]
            }
        }

        tabsList.remove(tab)

        if (newCurrentTab != null) {
            if (!tabsList.contains(newCurrentTab)) {
                tabsList.add(newCurrentTab)
            }

            tabs.value = tabsList
            currentTab.value = newCurrentTab
        } else {
            tabs.value = tabsList
        }

        releaseUnusedRepositories()

        updatePersistedTabs()
        System.gc()
    }

    fun switchGroup(name: String) = viewModelScope.launch {
        if (name == activeGroup.value || name !in groups.value) return@launch

        updatePersistedTabs()
        groups.update { it + (activeGroup.value to tabsOpenRepository.openTabs.first()) }
        groupSelectedTabIndexMap += activeGroup.value to (tabsOpenRepository.selectedTabIndex.first() ?: 0)

        val paths = groups.value.getValue(name)
        val newTabs = paths.map { newAppTab2(it) }.ifEmpty { listOf(newAppTab2()) }
        val selectedIndex = groupSelectedTabIndexMap[name]?.takeIf { it in newTabs.indices } ?: 0

        tabs.value.forEach { it.data.dispose() }
        tabs.value = newTabs
        currentTab.value = newTabs[selectedIndex]
        activeGroup.value = name

        cleanRepositoriesResourcesUseCase(paths)
        tabsOpenRepository.updateGroups(groups.value)
        tabsOpenRepository.updateActiveGroup(name)
        tabsOpenRepository.updateOpenTabs(paths)
        tabsOpenRepository.updateSelectedTabIndex(selectedIndex)
        tabsOpenRepository.updateGroupSelectedTabIndexMap(groupSelectedTabIndexMap)
        System.gc()
    }

    fun switchToAdjacentGroup(offset: Int) {
        val nameList = groups.value.keys.toList()
        val index = nameList.indexOf(activeGroup.value)

        if (index < 0 || nameList.size < 2) return

        switchGroup(nameList[Math.floorMod(index + offset, nameList.size)])
    }

    fun createGroup(name: String) {
        if (name in groups.value) return

        groups.update { it + (name to emptyList<String>()) }
        switchGroup(name)
    }

    fun renameGroup(oldName: String, newName: String) = viewModelScope.launch {
        if (newName in groups.value) return@launch

        groups.update { it.mapKeys { (key, _) -> if (key == oldName) newName else key } }
        groupSelectedTabIndexMap = groupSelectedTabIndexMap.mapKeys { (key, _) -> if (key == oldName) newName else key }
        tabsOpenRepository.updateGroupSelectedTabIndexMap(groupSelectedTabIndexMap)
        if (activeGroup.value == oldName) {
            activeGroup.value = newName
            tabsOpenRepository.updateActiveGroup(newName)
        }
        tabsOpenRepository.updateGroups(groups.value)
    }

    fun deleteGroup(name: String) = viewModelScope.launch {
        if (name == activeGroup.value) return@launch

        groups.update { it - name }
        groupSelectedTabIndexMap -= name
        tabsOpenRepository.updateGroupSelectedTabIndexMap(groupSelectedTabIndexMap)
        tabsOpenRepository.updateGroups(groups.value)
    }

    suspend fun updatePersistedTabs() {
        val tabsToPersist = tabs
            .value
            .mapNotNull { tab -> tab.data.persistedPath?.let { tab to it } }

        val paths = tabsToPersist.map { it.second }
        tabsOpenRepository.updateOpenTabs(paths)
        tabsOpenRepository.updateSelectedTabIndex(tabsToPersist.indexOfFirst { it.first == currentTab.value })
    }

    fun addNewEmptyTab() {
        val newTab = newAppTab2()

        tabs.update {
            it.toMutableList().apply {
                add(newTab)
            }
        }

        currentTab.value = newTab
    }

    private fun newAppTab2(path: String? = null): TabInformation<RepositoryTabViewModel> {
        val tabComponent: TabComponent = tabComponentFactory.create()
        val viewModel = tabComponent
            .repositoryTabViewModelFactory()
            .create(path)

        return TabInformation(viewModel)
    }

    fun onMoveTab(fromIndex: Int, toIndex: Int) = viewModelScope.launch {
        tabs.update {
            it.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        }

        updatePersistedTabs()
    }
}
