package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import com.jetpackduba.gitnuro.domain.models.FileChanged
import com.jetpackduba.gitnuro.domain.models.FileType
import com.jetpackduba.gitnuro.domain.models.WatcherEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystemException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val WATCH_TIMEOUT_MS = 500L
private const val MIN_TIME_BETWEEN_REFRESHES_MS = 500L

// Error codes reported through [WatcherEvent.WatchInitError]
private const val ERROR_IO = 2
private const val ERROR_PATH_NOT_FOUND = 3
private const val ERROR_MAX_FILES_WATCH = 6

/**
 * ponytail: java.nio WatchService polls every ~10s on macOS, upgrade to a native watcher if that matters.
 */
@TabScope
class FileChangesWatcher @Inject constructor() : AutoCloseable, IFileChangesWatcher {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val watchedDirs = ConcurrentHashMap<WatchKey, Path>()
    private val recursiveRoots = ConcurrentHashMap.newKeySet<Path>()
    private val errors = Channel<Int>(Channel.UNLIMITED)

    @Volatile
    private var isClosed = false

    override fun addPathToWatch(path: String, isRecursive: Boolean) {
        val root = Path.of(path)

        if (isRecursive) {
            recursiveRoots.add(root)
        }

        register(root, isRecursive)
    }

    override fun removePathFromWatch(path: String) {
        val dir = Path.of(path)

        recursiveRoots.remove(dir)
        watchedDirs.entries
            .filter { it.value == dir }
            .forEach {
                it.key.cancel()
                watchedDirs.remove(it.key)
            }
    }

    private fun register(dir: Path, isRecursive: Boolean) {
        try {
            if (isRecursive) {
                Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        registerDirectory(dir)
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                        FileVisitResult.CONTINUE
                })
            } else {
                registerDirectory(dir)
            }
        } catch (e: ClosedWatchServiceException) {
            // Closed while registering, nothing to watch anymore
        } catch (e: IOException) {
            errors.trySend(e.toErrorCode())
        }
    }

    private fun registerDirectory(dir: Path) {
        val key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        watchedDirs[key] = dir
    }

    private fun IOException.toErrorCode() = when {
        this is NoSuchFileException -> ERROR_PATH_NOT_FOUND
        this is FileSystemException && message.orEmpty().contains("limit", ignoreCase = true) -> ERROR_MAX_FILES_WATCH
        else -> ERROR_IO
    }

    override suspend fun observeEvents(): Flow<WatcherEvent> = channelFlow {
        val pending = HashMap<FileChanged, MutableList<WatchEvent.Kind<*>>>()
        var lastUpdate = 0L

        suspend fun flush() {
            // A file created and then removed before being reported is a temporary file, nobody cares about it
            val changes = pending
                .filter { (_, kinds) ->
                    val created = kinds.indexOf(ENTRY_CREATE)
                    val removed = kinds.indexOf(ENTRY_DELETE)

                    !(created != -1 && removed != -1 && created < removed)
                }
                .keys
                .toList()

            pending.clear()
            lastUpdate = System.currentTimeMillis()

            if (changes.isNotEmpty()) {
                send(WatcherEvent.ChangesDetected(changes))
            }
        }

        while (isActive && !isClosed) {
            while (true) {
                send(WatcherEvent.WatchInitError(errors.tryReceive().getOrNull() ?: break))
            }

            val key = try {
                watchService.poll(WATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (e: ClosedWatchServiceException) {
                break
            }

            if (key == null) {
                flush()
                continue
            }

            val dir = watchedDirs[key]

            if (dir != null) {
                for (event in key.pollEvents()) {
                    val name = event.context() as? Path ?: continue // OVERFLOW has no path
                    val path = dir.resolve(name)
                    val kind = event.kind()

                    val fileType = when {
                        kind == ENTRY_CREATE && Files.isDirectory(path) -> {
                            if (recursiveRoots.any { path.startsWith(it) }) {
                                register(path, isRecursive = true)
                            }

                            FileType.DIRECTORY
                        }

                        kind == ENTRY_DELETE && watchedDirs.containsValue(path) -> FileType.DIRECTORY
                        else -> FileType.FILE
                    }

                    pending.getOrPut(FileChanged(path.toString(), fileType)) { mutableListOf() }.add(kind)
                }
            }

            if (!key.reset()) {
                watchedDirs.remove(key)
            }

            if (lastUpdate != 0L && System.currentTimeMillis() - lastUpdate > MIN_TIME_BETWEEN_REFRESHES_MS) {
                flush()
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun close() {
        isClosed = true
        watchService.close()
    }
}
