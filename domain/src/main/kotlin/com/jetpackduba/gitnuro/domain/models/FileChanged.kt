package com.jetpackduba.gitnuro.domain.models

enum class FileType {
    FILE,
    DIRECTORY,
}

data class FileChanged(val path: String, val fileType: FileType)
