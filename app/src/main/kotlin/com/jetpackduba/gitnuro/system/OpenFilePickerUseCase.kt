package com.jetpackduba.gitnuro.system

import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.domain.ShellManager
import java.awt.FileDialog
import javax.inject.Inject
import javax.swing.JFileChooser
import javax.swing.UIManager

private const val TAG = "SystemDialogs"

// TODO This is not a domain use case, using a different name would be nice.
/**
 * Shows a picker dialog to select a file or directory
 */
class OpenFilePickerUseCase @Inject constructor(
    private val shellManager: ShellManager,
) {
    operator fun invoke(pickerType: PickerType, basePath: String?): String? =
        pick(pickerType, basePath, multiple = false).firstOrNull()

    /**
     * Same as [invoke] but allows selecting several entries. Returns an empty list if the dialog is cancelled.
     */
    fun pickMultiple(pickerType: PickerType, basePath: String?): List<String> =
        pick(pickerType, basePath, multiple = true)

    private fun pick(pickerType: PickerType, basePath: String?, multiple: Boolean): List<String> {
        val isLinux = currentOs.isLinux()
        val isMac = currentOs.isMac()

        return if (isLinux) {
            openDirectoryDialogLinux(pickerType, multiple)
        } else
            openJvmDialog(pickerType, basePath, false, isMac, multiple)
    }

    private fun openDirectoryDialogLinux(pickerType: PickerType, multiple: Boolean): List<String> {
        var dirsToOpen: List<String> = emptyList()

        val checkZenityInstalled = shellManager.runCommand(listOf("which", "zenity", "2>/dev/null"))
        val isZenityInstalled = !checkZenityInstalled.isNullOrEmpty()

        printLog(TAG, "IsZenityInstalled $isZenityInstalled")

        if (isZenityInstalled) {
            val command = buildList {
                addAll(listOf("zenity", "--file-selection", "--title=Open"))
                if (pickerType == PickerType.DIRECTORIES) add("--directory")
                // Unit separator can't appear in a path, unlike zenity's default "|"
                if (multiple) addAll(listOf("--multiple", "--separator=\u001F"))
            }

            val openDirectory = shellManager.runCommand(command)?.replace("\n", "")

            if (!openDirectory.isNullOrEmpty())
                dirsToOpen = openDirectory.split('\u001F').filter { it.isNotEmpty() }
        } else
            dirsToOpen = openJvmDialog(pickerType, "", isLinux = true, isMac = false, multiple = multiple)

        return dirsToOpen
    }

    private fun openJvmDialog(
        pickerType: PickerType,
        basePath: String?,
        isLinux: Boolean,
        isMac: Boolean,
        multiple: Boolean,
    ): List<String> {
        if (!isLinux) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }

        if (isMac) {
            if (pickerType == PickerType.DIRECTORIES) {
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
            }

            val fileChooser = if (basePath.isNullOrEmpty()) {
                FileDialog(null as java.awt.Frame?, "Open", FileDialog.LOAD)
            } else {
                FileDialog(null as java.awt.Frame?, "Open", FileDialog.LOAD).apply {
                    directory = basePath
                }
            }

            fileChooser.isMultipleMode = multiple
            fileChooser.isVisible = true

            System.setProperty("apple.awt.fileDialogForDirectories", "false")

            return fileChooser.files.map { it.absolutePath }
        } else {
            val fileChooser = if (basePath.isNullOrEmpty())
                JFileChooser()
            else
                JFileChooser(basePath)
            fileChooser.fileSelectionMode = pickerType.value
            fileChooser.isMultiSelectionEnabled = multiple
            fileChooser.showOpenDialog(null)
            // getSelectedFiles is empty in single selection mode
            val selected = if (multiple) fileChooser.selectedFiles.toList() else listOfNotNull(fileChooser.selectedFile)
            return selected.map { it.absolutePath }
        }
    }
}

enum class PickerType(val value: Int) {
    FILES(JFileChooser.FILES_ONLY),
    DIRECTORIES(JFileChooser.DIRECTORIES_ONLY);
}