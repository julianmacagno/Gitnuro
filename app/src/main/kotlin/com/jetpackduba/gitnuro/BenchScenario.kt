package com.jetpackduba.gitnuro

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.ViewDiffResult
import com.jetpackduba.gitnuro.repositoryopen.CommitChangesAction
import com.jetpackduba.gitnuro.repositoryopen.RepositoryOpenViewModel
import com.jetpackduba.gitnuro.ui.AppViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.jetpackduba.gitnuro.data.DataStoreFilesProvider
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.lang.management.ManagementFactory
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Memory and speed benchmark that drives the real UI like a person would: opens groups of repositories in tabs, scrolls
 * the log, selects commits, opens diffs, rotates tabs and closes them. Enabled only by the jar's `--bench` argument,
 * which takes the groups (and their repositories) saved in GitIA's settings; `--bench=a,b;c,d` lists them instead
 * (groups separated by `;`, repositories by `,`). Options: `--bench-out=<tsv>` (default bench.tsv),
 * `--bench-limit=<repos per group>`, `--bench-commits=<commits per repo>`, `--bench-hold=<seconds>`, `--bench-rss=ps`.
 *
 * It runs with a throwaway settings directory, so the real tabs and groups are never touched, never fetches (auto fetch
 * is disabled while enabled) and only reads the repositories.
 */
object BenchScenario {
    private var options: Map<String, String> = emptyMap()

    var enabled = false
        private set

    private var groups: List<List<String>> = emptyList()
    private var configDir: File? = null

    /** Called before the app starts: picks the repositories and points the app to a throwaway settings directory. */
    fun prepare(args: Array<String>) {
        // --bench[=groups] and --bench-<option>=<value>
        options = args.mapNotNull { Regex("--bench(?:-(\\w+))?(?:=(.*))?").matchEntire(it) }
            .associate { it.groupValues[1].ifEmpty { "groups" } to it.groupValues[2] }
        enabled = "groups" in options
        if (!enabled) return
        val limit = options["limit"]?.toInt() ?: Int.MAX_VALUE
        val listed = options.getValue("groups").ifBlank { null }?.split(';')?.map { it.split(',') } ?: settingsGroups()

        groups = listed
            .map { group -> group.map { it.trim().removeSuffix("/").removeSuffix("/.git") }.filter { File(it, ".git").exists() }.take(limit) }
            .filter { it.isNotEmpty() }
        configDir = Files.createTempDirectory("gitia-bench").toFile()
        System.setProperty("gitia.configDir", configDir!!.path) // read by DataStoreFilesProvider
        System.err.println("BENCH groups: ${groups.map { it.size }} repositories")
    }

    /** Groups saved in the real settings (a DataStore protobuf whose values are JSON); the active group is the open tabs. */
    private fun settingsGroups(): List<List<String>> {
        val file = File(DataStoreFilesProvider().getTabsOpenPath())
        if (!file.exists()) return emptyList()

        val prefs = protoFields(file.readBytes()).mapNotNull { (_, entry) ->
            val fields = protoFields(entry).toMap()
            val value = fields[2]?.let { protoFields(it).toMap()[5] } ?: return@mapNotNull null // Value.string
            fields[1]?.decodeToString()?.let { it to value.decodeToString() }
        }.toMap()
        val saved = prefs["tab_groups"]?.let { Json.decodeFromString<Map<String, List<String>>>(it) }.orEmpty()
        val openTabs = prefs["tabs_open"]?.let { Json.decodeFromString<List<String>>(it) }.orEmpty()

        return (saved + (prefs["active_tab_group"].orEmpty() to openTabs)).values.toList()
    }

    /** Length-delimited fields (number to bytes) of a protobuf message; other wire types are skipped. */
    private fun protoFields(bytes: ByteArray): List<Pair<Int, ByteArray>> {
        val fields = mutableListOf<Pair<Int, ByteArray>>()
        var i = 0
        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val b = bytes[i++].toInt() and 0xff
                result = result or ((b and 0x7f).toLong() shl shift)
                shift += 7
                if (b < 0x80) return result
            }
        }
        while (i < bytes.size) {
            val key = varint().toInt()
            when (key and 7) {
                0 -> varint()
                1 -> i += 8
                2 -> varint().toInt().let { n -> fields += (key ushr 3) to bytes.copyOfRange(i, i + n); i += n }
                5 -> i += 4
                else -> return fields
            }
        }
        return fields
    }

    /** Set from the window's composition: its context has the frame clock that animations and frame counting need. */
    @Volatile
    var uiScope: CoroutineScope? = null

    private val out by lazy { File(options["out"] ?: "bench.tsv") }
    private val commitsPerRepo by lazy { options["commits"]?.toInt() ?: 6 }
    private val rows = StringBuilder("step\trssMb\trssAnonMb\trssFileMb\theapLiveMb\theapCommittedMb\tnonHeapMb\tpeakRssMb\tcpuSec\telapsedSec\n")

    suspend fun run(appViewModel: AppViewModel) {
        try {
            withTimeoutOrNull(30_000) { while (uiScope == null) delay(50) } // the window is composed
            delay(1000) // and painted
            sample("start")

            for ((index, group) in groups.withIndex()) {
                val name = "g$index"
                group.forEach { repo -> openAndUse(appViewModel, repo, name) }
                sample("$name-all-open")
                // --bench-hold=<seconds>: stay idle with the group open, to take heap/native dumps from outside
                options["hold"]?.toLong()?.let { System.err.println("BENCH HOLD $it s"); delay(it * 1000) }

                repositoryTabs(appViewModel).take(3).forEachIndexed { i, tab ->
                    appViewModel.selectTab(tab)
                    delay(300)
                    currentViewModel(appViewModel)?.let { measureFps(it, "$name-tab$i") }
                }

                repeat(2) { round ->
                    repositoryTabs(appViewModel).forEach { tab ->
                        appViewModel.selectTab(tab)
                        delay(300)
                        useCurrent(appViewModel, 2)
                    }
                    sample("$name-rotate-$round")
                }

                // Like switchGroup: every tab of the group is disposed
                appViewModel.tabs.value.toList().forEach { appViewModel.closeTab(it).join() }
                delay(500)
                sample("$name-closed")
                reportLatencies(name)
            }
        } catch (e: Throwable) {
            rows.append("ERROR\t${e}\n")
            e.printStackTrace()
        } finally {
            sample("end")
            out.writeText(rows.toString())
            print(rows)
            configDir?.deleteRecursively()
            exitProcess(0)
        }
    }

    private suspend fun openAndUse(appViewModel: AppViewModel, repo: String, group: String) {
        val openStart = System.nanoTime()
        appViewModel.addNewTabFromPath(repo, selectTab = true)
        val vm = currentViewModel(appViewModel) ?: run { rows.append("TIMEOUT\t$repo\n"); return }
        latency("open", openStart)

        // Grow the log like scrolling to the bottom does, until no more pages come
        withTimeoutOrNull(60_000) {
            while (true) {
                val size = vm.logState.value.commitList.size
                val state: LazyListState = vm.verticalListState.value
                withContext(Dispatchers.Main) { state.scrollToItem((size - 1).coerceAtLeast(0)) }
                val grew = withTimeoutOrNull(1000) { while (vm.logState.value.commitList.size == size) delay(20) } != null
                if (!grew) break
            }
        }
        useCurrent(appViewModel, commitsPerRepo)
        sample("$group-${File(repo).name}")
    }

    /** Tabs with a repository: the app always keeps an empty one (sometimes with an unused RepositoryOpen view model). */
    private fun repositoryTabs(appViewModel: AppViewModel) =
        appViewModel.tabs.value.filter { it.data.repositoryPath.value != null && it.data.viewModelsMap[Screen.RepositoryOpen] != null }

    private suspend fun currentViewModel(appViewModel: AppViewModel): RepositoryOpenViewModel? {
        val tab = appViewModel.currentTab.value?.data ?: return null

        return withTimeoutOrNull(60_000) {
            while (true) {
                val vm = tab.viewModelsMap[Screen.RepositoryOpen] as? RepositoryOpenViewModel
                if (vm != null && vm.logState.value.commitList.isNotEmpty()) return@withTimeoutOrNull vm
                delay(2)
            }
            @Suppress("UNREACHABLE_CODE") null
        }
    }

    /**
     * Smoothness: animates a scroll of the log (like the mouse wheel) and counts the frames the UI really renders.
     * Writes `FPS <label> <frames per second> <95th percentile frame time ms>`; the screen refresh rate caps it.
     */
    private suspend fun measureFps(vm: RepositoryOpenViewModel, label: String) {
        val scope = uiScope ?: return
        vm.clearDiff() // an open diff replaces the log
        delay(300)
        val state = vm.verticalListState.value
        withContext(Dispatchers.Main) { state.scrollToItem(0) }
        delay(200)

        // A hidden or covered window renders ~1 frame per second with OpenGL: keep it on top while measuring
        val windows = withContext(Dispatchers.Main) {
            java.awt.Window.getWindows().filter { it.isVisible }.onEach {
                (it as? java.awt.Frame)?.extendedState = java.awt.Frame.MAXIMIZED_BOTH // software rendering cost grows with pixels
                it.isAlwaysOnTop = true
                it.toFront()
            }
        }
        delay(1000)
        scope.launch { state.animateScrollBy(3_000f, tween(1000, easing = LinearEasing)) }.join() // warm up the JIT

        val frames = mutableListOf<Long>()
        val counter = scope.launch { while (true) withFrameNanos { frames += it } }
        scope.launch { state.animateScrollBy(20_000f, tween(2000, easing = LinearEasing)) }.join()
        counter.cancel()
        withContext(Dispatchers.Main) { windows.forEach { it.isAlwaysOnTop = false } }

        val gaps = withContext(Dispatchers.Main) { frames.zipWithNext { a, b -> (b - a) / 1e6 }.sorted() }
        val fps = if (gaps.isEmpty()) 0.0 else 1000 / gaps.average()
        val p95 = gaps.getOrElse((gaps.size * 0.95).toInt()) { 0.0 }
        val line = "FPS\t$label\t%.1f\t%.1f\n".format(Locale.ROOT, fps, p95)
        rows.append(line)
        System.err.print("BENCH $line")
    }

    private val latencies = mutableMapOf<String, MutableList<Double>>()

    private fun latency(kind: String, start: Long) {
        latencies.getOrPut(kind) { mutableListOf() } += (System.nanoTime() - start) / 1e6
    }

    /** Writes `LAT <group> <kind> <median ms> <90th percentile ms> <count>`: how long the UI waits for each operation. */
    private fun reportLatencies(group: String) {
        for ((kind, values) in latencies.toSortedMap()) {
            val sorted = values.sorted()
            val line = "LAT\t$group-$kind\t%.0f\t%.0f\t%d\n".format(
                Locale.ROOT, sorted[sorted.size / 2], sorted[(sorted.size * 0.9).toInt().coerceAtMost(sorted.size - 1)], sorted.size,
            )
            rows.append(line)
            System.err.print("BENCH $line")
        }
        latencies.clear()
    }

    /** Selects [count] commits spread over the log, opening the diff of the first two files of each. */
    private suspend fun useCurrent(appViewModel: AppViewModel, count: Int) {
        val vm = currentViewModel(appViewModel) ?: return
        val commits = vm.logState.value.commitList.values.map { it.commit }
        if (commits.isEmpty()) return
        val step = (commits.size / count).coerceAtLeast(1)

        for (i in 0 until count) {
            val commit = commits[(i * step + 3).coerceAtMost(commits.size - 1)]
            val commitStart = System.nanoTime()
            vm.selectCommit(commit, isStash = false, scrollToItem = true).join()

            val changes = withTimeoutOrNull(10_000) {
                while (true) {
                    val state = vm.commitChangesState.value
                    if (state != null && !state.isLoading && state.commit.hash == commit.hash && state.changes.isNotEmpty()) {
                        return@withTimeoutOrNull state.changes
                    }
                    delay(2)
                }
                @Suppress("UNREACHABLE_CODE") null
            } ?: continue
            latency("commit", commitStart)

            for (entry in changes.take(2)) {
                val type = DiffType.CommitDiff(entry)
                val diffStart = System.nanoTime()
                vm.onAction(CommitChangesAction.SelectEntry(entry))
                withTimeoutOrNull(10_000) {
                    while (vm.diffResult.value?.let { it is ViewDiffResult.Loaded && it.diffType == type } != true) delay(2)
                    latency("diff", diffStart)
                }
                delay(100) // let it render
            }
        }
    }

    // /proc only exists on Linux; elsewhere (or with --bench-rss=ps) the RSS comes from `ps` / PowerShell
    private val useProc by lazy { options["rss"] != "ps" && File("/proc/self/status").exists() }
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

    private val startNanos = System.nanoTime()

    /** CPU used by the whole process (every thread, GC and JIT included): the cost of a flag that trades CPU for RAM. */
    private fun cpuSeconds() = (ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.OperatingSystemMXBean)
        ?.processCpuTime?.let { it / 1_000_000_000.0 } ?: 0.0

    private fun sample(step: String) {
        val cpu = "%.1f".format(Locale.ROOT, cpuSeconds())
        val elapsed = "%.0f".format(Locale.ROOT, (System.nanoTime() - startNanos) / 1e9)
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
        val line = "$step\t$rss\t$anon\t$file\t${heap.used shr 20}\t${heap.committed shr 20}\t${mem.nonHeapMemoryUsage.committed shr 20}\t$peak\t$cpu\t$elapsed\n"
        rows.append(line)
        System.err.print("BENCH $line")
    }
}
