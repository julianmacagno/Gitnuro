package com.jetpackduba.gitnuro

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText

object Installer {
    private val osName = System.getProperty("os.name").lowercase()
    private val isWindows = osName.contains("win")
    private val isMac = osName.contains("mac")
    private val home = Path.of(System.getProperty("user.home"))
    private val javaBin = Path.of(System.getProperty("java.home")) / "bin" / if (isWindows) "javaw.exe" else "java"

    fun run(argList: Array<String>): Boolean {
        val install = "--install" in argList
        val uninstall = "--uninstall" in argList

        if (!install && !uninstall) return false

        when {
            isWindows -> if (install) installWindows() else uninstallWindows()
            isMac -> if (install) installMac() else uninstallMac()
            else -> if (install) installLinux() else uninstallLinux()
        }

        println(if (install) "${AppConstants.APP_NAME} installed" else "${AppConstants.APP_NAME} uninstalled")
        return true
    }

    private fun installLinux() {
        val dir = home / ".local/share/gitia"
        val jar = dir / "gitia.jar"
        val icon = dir / "icon.png"
        val launcher = home / ".local/bin/gitia"
        val desktopFile = home / ".local/share/applications/gitia.desktop"

        copyJar(jar)
        copyResource("icon.png", icon)

        writeFile(launcher, "#!/bin/sh\nexec \"$javaBin\" -jar \"$jar\" \"\$@\"\n", "rwxr-xr-x")
        writeFile(
            desktopFile,
            """
            [Desktop Entry]
            Name=${AppConstants.APP_NAME}
            Comment=An enhanced, AI-developed fork of Gitnuro
            Exec="$launcher"
            Icon=$icon
            Terminal=false
            Type=Application
            Categories=Development;
            StartupWMClass=gitia
            """.trimIndent() + "\n",
            "rw-r--r--",
        )
    }

    private fun uninstallLinux() {
        (home / ".local/bin/gitia").deleteIfExists()
        (home / ".local/share/applications/gitia.desktop").deleteIfExists()
        (home / ".local/share/gitia").toFile().deleteRecursively()
    }

    private fun installMac() {
        val app = home / "Applications/GitIA.app"
        val resourcesDir = app / "Contents/Resources"
        val executable = app / "Contents/MacOS/GitIA"

        copyJar(resourcesDir / "gitia.jar")
        copyResource("icon.icns", resourcesDir / "icon.icns")

        writeFile(
            executable,
            "#!/bin/sh\n" +
                "DIR=\"\$(cd \"\$(dirname \"\$0\")/../Resources\" && pwd)\"\n" +
                "exec \"$javaBin\" -Dapple.awt.application.appearance=system -Xdock:name=GitIA " +
                "-jar \"\$DIR/gitia.jar\" \"\$@\"\n",
            "rwxr-xr-x",
        )
        writeFile(
            app / "Contents/Info.plist",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleName</key><string>${AppConstants.APP_NAME}</string>
                <key>CFBundleDisplayName</key><string>${AppConstants.APP_NAME}</string>
                <key>CFBundleIdentifier</key><string>io.github.julianmacagno.gitia</string>
                <key>CFBundleExecutable</key><string>GitIA</string>
                <key>CFBundleIconFile</key><string>icon</string>
                <key>CFBundlePackageType</key><string>APPL</string>
                <key>CFBundleVersion</key><string>${AppConstants.APP_VERSION}</string>
                <key>CFBundleShortVersionString</key><string>${AppConstants.APP_VERSION}</string>
                <key>NSHighResolutionCapable</key><true/>
            </dict>
            </plist>
            """.trimIndent() + "\n",
            "rw-r--r--",
        )
    }

    private fun uninstallMac() {
        (home / "Applications/GitIA.app").toFile().deleteRecursively()
    }

    private fun installWindows() {
        val dir = Path.of(System.getenv("LOCALAPPDATA")) / "GitIA"
        val jar = dir / "gitia.jar"
        val icon = dir / "icon.ico"

        copyJar(jar)
        copyResource("icon.ico", icon)

        exec(
            "powershell", "-NoProfile", "-Command",
            "\$s = (New-Object -ComObject WScript.Shell).CreateShortcut('${windowsShortcut()}'); " +
                "\$s.TargetPath = '$javaBin'; " +
                "\$s.Arguments = '-jar \"$jar\"'; " +
                "\$s.IconLocation = '$icon'; " +
                "\$s.Save()",
        )

        mapOf(
            "DisplayName" to AppConstants.APP_NAME,
            "DisplayVersion" to AppConstants.APP_VERSION,
            "DisplayIcon" to icon.toString(),
            "UninstallString" to "\"$javaBin\" -jar \"$jar\" --uninstall",
        ).forEach { (name, value) ->
            exec("reg", "add", WINDOWS_UNINSTALL_KEY, "/v", name, "/d", value, "/f")
        }
    }

    private fun uninstallWindows() {
        val dir = Path.of(System.getenv("LOCALAPPDATA")) / "GitIA"

        windowsShortcut().deleteIfExists()
        exec("reg", "delete", WINDOWS_UNINSTALL_KEY, "/f")
        ProcessBuilder("cmd", "/c", "ping -n 3 127.0.0.1 >nul & rmdir /s /q \"$dir\"").start()
    }

    private fun windowsShortcut(): Path =
        Path.of(System.getenv("APPDATA")) / "Microsoft/Windows/Start Menu/Programs/GitIA.lnk"

    private fun currentJar(): Path {
        val jar = Path.of(Installer::class.java.protectionDomain.codeSource.location.toURI())

        require(jar.isRegularFile() && jar.extension == "jar") {
            "Run the installer from the GitIA jar: java -jar GitIA-universal.jar --install"
        }

        return jar
    }

    private fun copyJar(target: Path) {
        val source = currentJar()

        if (target.exists() && Files.isSameFile(source, target)) return

        val temp = target.resolveSibling("${target.fileName}.new")
        target.parent.createDirectories()
        Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING)
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun copyResource(name: String, target: Path) {
        target.parent.createDirectories()

        Installer::class.java.getResourceAsStream("/installer/$name")!!.use {
            Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun writeFile(target: Path, content: String, permission: String) {
        target.parent.createDirectories()
        target.writeText(content)
        target.setPosixFilePermissions(PosixFilePermissions.fromString(permission))
    }

    private fun exec(vararg command: String) {
        val exitCode = ProcessBuilder(*command).inheritIO().start().waitFor()

        check(exitCode == 0) { "${command.first()} failed with exit code $exitCode" }
    }

    private const val WINDOWS_UNINSTALL_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\GitIA"
}
