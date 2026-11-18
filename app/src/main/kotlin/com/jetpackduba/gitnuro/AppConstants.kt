package com.jetpackduba.gitnuro

object AppConstants {
    val openSourceProjects = listOf(
        Project("Google Dagger", "https://dagger.dev/", apache__2_0),
        Project("Compose Multiplatform", "https://www.jetbrains.com/lp/compose-mpp/", apache__2_0),
        Project("JGit", "https://www.eclipse.org/jgit/", edl),
        Project("Kotlin", "https://kotlinlang.org/", apache__2_0),
        Project(
            "Kotlinx.serialization",
            "https://kotlinlang.org/docs/serialization.html#example-json-serialization",
            apache__2_0
        ),
        Project("Mockk", "https://mockk.io/", apache__2_0),
        Project("Apache MINA SSHD", "https://mina.apache.org/sshd-project/", apache__2_0),
        Project("Ktor", "https://ktor.io/", apache__2_0),
        Project("Coil", "https://coil-kt.github.io/coil/", apache__2_0),
        Project("Bouncy Castle", "https://www.bouncycastle.org/", mit),
        Project("SLF4J", "https://www.slf4j.org/", mit),
        Project("Inter font", "https://github.com/rsms/inter", ofl),
        Project("Noto Sans Mono font", "https://github.com/notofonts/latin-greek-cyrillic", ofl),
        Project("Material Icons and Symbols (SVG files)", "https://github.com/google/material-design-icons", apache__2_0),
    )


    // Remember to update build.gradle when changing this
    const val APP_NAME = "GitIA"
    const val APP_DESCRIPTION =
        "GitIA is an enhanced, AI-developed fork of Gitnuro, a Git client that allows you to manage multiple repositories with a modern experience and live visual representation of your repositories' state."
    const val APP_VERSION = "2.2"
    const val APP_VERSION_CODE = 29
    const val UPSTREAM_URL = "https://github.com/JetpackDuba/Gitnuro"
    const val UPSTREAM_SPONSOR_URL = "https://github.com/sponsors/JetpackDuba"
    const val GPL_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
    const val REPOSITORY_URL = "https://github.com/julianmacagno/Gitnuro"
    const val VERSION_CHECK_URL = "https://raw.githubusercontent.com/julianmacagno/Gitnuro/main/latest.json"
}


private val apache__2_0 = License("Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
private val mit = License("MIT", "https://opensource.org/license/mit")
private val ofl = License("SIL OFL 1.1", "https://openfontlicense.org")
private val edl = License("EDL", "https://www.eclipse.org/org/documents/edl-v10.php")

data class License(
    val name: String,
    val url: String,
)

data class Project(val name: String, val url: String, val license: License)
