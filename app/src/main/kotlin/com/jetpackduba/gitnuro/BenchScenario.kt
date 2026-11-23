package com.jetpackduba.gitnuro

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.ViewDiffResult
import com.jetpackduba.gitnuro.repositoryopen.CommitChangesAction
import com.jetpackduba.gitnuro.repositoryopen.RepositoryOpenViewModel
import com.jetpackduba.gitnuro.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

/**
 * Memory benchmark that drives the real UI like a person would: opens groups of repositories in tabs, scrolls the log,
 * selects commits, opens diffs, rotates tabs and closes them. Enabled only with `-Dgitia.bench.groups=a,b;c,d`
 * (groups separated by `;`, repositories by `,`) and writes a TSV to `-Dgitia.bench.out`.
 *
 * It never fetches (auto fetch is disabled while enabled) and only reads the repositories.
 */
object BenchScenario {
    private val groups: List<List<String>> = System.getProperty("gitia.bench.groups").orEmpty()
        .split(';').map { g -> g.split(',').map { it.trim().removeSuffix("/.git") }.filter { it.isNotEmpty() } }
        .filter { it.isNotEmpty() }
        .map { it.take(System.getProperty("gitia.bench.limit")?.toInt() ?: Int.MAX_VALUE) }

    val enabled = groups.isNotEmpty()

    private val out = File(System.getProperty("gitia.bench.out") ?: "bench.tsv")
    private val commitsPerRepo = System.getProperty("gitia.bench.commits")?.toInt() ?: 12
    private val rows = StringBuilder("step\trssMb\trssAnonMb\trssFileMb\theapLiveMb\theapCommittedMb\tnonHeapMb\tpeakRssMb\n")

    suspend fun run(appViewModel: AppViewModel) {
        try {
            delay(4000) // let the window appear
            sample("start")

            for ((index, group) in groups.withIndex()) {
                val name = "g$index"
                group.forEach { repo -> openAndUse(appViewModel, repo, name) }
                sample("$name-all-open")

                repeat(2) { round ->
                    appViewModel.tabs.value.forEach { tab ->
                        appViewModel.selectTab(tab)
                        delay(700)
                        useCurrent(appViewModel, 3)
                    }
                    sample("$name-rotate-$round")
                }

                // Like switchGroup: every tab of the group is disposed
                appViewModel.tabs.value.toList().forEach { appViewModel.closeTab(it).join() }
                delay(2000)
                sample("$name-closed")
            }
        } catch (e: Throwable) {
            rows.append("ERROR\t${e}\n")
            e.printStackTrace()
        } finally {
            sample("end")
            out.writeText(rows.toString())
            print(rows)
            exitProcess(0)
        }
    }

    private suspend fun openAndUse(appViewModel: AppViewModel, repo: String, group: String) {
        appViewModel.addNewTabFromPath(repo, selectTab = true)
        val vm = currentViewModel(appViewModel) ?: run { rows.append("TIMEOUT\t$repo\n"); return }

        // Grow the log like scrolling to the bottom does
        withTimeoutOrNull(60_000) {
            var last = -1
            while (vm.logState.value.commitList.size != last) {
                last = vm.logState.value.commitList.size
                val state: LazyListState = vm.logState.value.verticalScrollState
                withContext(Dispatchers.Main) { state.scrollToItem((last - 1).coerceAtLeast(0)) }
                delay(1500)
            }
        }
        useCurrent(appViewModel, commitsPerRepo)
        sample("$group-${File(repo).name}")
    }

    private suspend fun currentViewModel(appViewModel: AppViewModel): RepositoryOpenViewModel? {
        val tab = appViewModel.currentTab.value?.data ?: return null

        return withTimeoutOrNull(60_000) {
            while (true) {
                val vm = tab.viewModelsMap[Screen.RepositoryOpen] as? RepositoryOpenViewModel
                if (vm != null && vm.logState.value.commitList.isNotEmpty()) return@withTimeoutOrNull vm
                delay(200)
            }
            @Suppress("UNREACHABLE_CODE") null
        }
    }

    /** Selects [count] commits spread over the log, opening the diff of the first two files of each. */
    private suspend fun useCurrent(appViewModel: AppViewModel, count: Int) {
        val vm = currentViewModel(appViewModel) ?: return
        val commits = vm.logState.value.commitList.values.map { it.commit }
        if (commits.isEmpty()) return
        val step = (commits.size / count).coerceAtLeast(1)

        for (i in 0 until count) {
            val commit = commits[(i * step + 3).coerceAtMost(commits.size - 1)]
            vm.selectCommit(commit, isStash = false, scrollToItem = true).join()

            val changes = withTimeoutOrNull(10_000) {
                while (true) {
                    val state = vm.commitChangesState.value
                    if (state != null && !state.isLoading && state.commit.hash == commit.hash && state.changes.isNotEmpty()) {
                        return@withTimeoutOrNull state.changes
                    }
                    delay(100)
                }
                @Suppress("UNREACHABLE_CODE") null
            } ?: continue

            for (entry in changes.take(2)) {
                val type = DiffType.CommitDiff(entry)
                vm.onAction(CommitChangesAction.SelectEntry(entry))
                withTimeoutOrNull(10_000) {
                    while (vm.diffResult.value?.let { it is ViewDiffResult.Loaded && it.diffType == type } != true) delay(100)
                }
                delay(400) // let it render
            }
        }
    }

    // /proc only exists on Linux; elsewhere (or with -Dgitia.bench.rss=ps) the RSS comes from `ps` / PowerShell
    private val useProc = System.getProperty("gitia.bench.rss") != "ps" && File("/proc/self/status").exists()
    private var peakRss = 0L

    private fun status(): Map<String, Long> =
        if (useProc) {
            File("/proc/self/status").readLines().mapNotNull { line ->
                val (key, value) = line.split(':', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                value.trim().removeSuffix(" kB").toLongOrNull()?.let { key to it / 1024 }
            }.toMap()
        } else {
            mapOf("VmRSS" to processRssMb())
        }

    private fun processRssMb(): Long {
        val pid = ProcessHandle.current().pid()
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val command = if (isWindows) {
            listOf("powershell", "-NoProfile", "-Command", "(Get-Process -Id $pid).WorkingSet64 / 1MB")
        } else {
            listOf("ps", "-o", "rss=", "-p", "$pid") // KB on Linux and macOS
        }
        val output = ProcessBuilder(command).redirectErrorStream(true).start().inputStream.bufferedReader().readText().trim()
        val value = output.toDoubleOrNull() ?: return 0

        return if (isWindows) value.toLong() else value.toLong() / 1024
    }

    private fun sample(step: String) {
        val s = status()
        val rss = s["VmRSS"] ?: 0
        peakRss = maxOf(peakRss, rss, s["VmHWM"] ?: 0)
        val peak = peakRss
        val anon = s["RssAnon"] ?: 0
        val file = s["RssFile"] ?: 0
        // RSS is read before forcing a GC so it reflects what the process actually holds in steady state
        System.gc()
        val mem = ManagementFactory.getMemoryMXBean()
        val heap = mem.heapMemoryUsage
        val line = "$step\t$rss\t$anon\t$file\t${heap.used shr 20}\t${heap.committed shr 20}\t${mem.nonHeapMemoryUsage.committed shr 20}\t$peak\n"
        rows.append(line)
        System.err.print("BENCH $line")
    }
}
