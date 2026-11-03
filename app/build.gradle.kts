import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    //application
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
}

val javaLanguageVersion = JavaLanguageVersion.of(25)

// Remember to update Constants.APP_VERSION when changing this version
val projectVersion = "2.1"

val projectName = "Gitnuro"

// Required for JPackage, as it doesn't accept additional suffixes after the version.
val projectVersionSimplified = "2.1.0"

val appPackageName = "com.jetpackduba.gitnuro"
group = appPackageName
version = projectVersion

val isLinuxAarch64 = (properties.getOrDefault("isLinuxAarch64", "false") as String).toBoolean()


sourceSets.main.get().java.srcDirs("app/src/main/resources").includes.addAll(arrayOf("**/*.*"))

dependencies {
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":domain"))

    val composeDependency = when {
        currentOs() == OS.LINUX && isLinuxAarch64 -> libs.compose.desktop.linux.arm64
        else -> compose.desktop.currentOs
    }

    println("composeDependency: $composeDependency")
    implementation(composeDependency)

    implementation(libs.compose.ui.util)
    implementation(libs.compose.components.animatedimage)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation3)

    implementation(libs.jgit.core)
    implementation(libs.jgit.gpg)
    implementation(libs.jgit.lfs)
    implementation(libs.jgit.ssh.apache)

    implementation(libs.coroutines)
    implementation(libs.kotlinx.coroutines.jvm)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.mockk)

    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.reload4j)

    implementation(libs.bouncycastle)

    implementation(libs.ktor.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)

    implementation(libs.datastore)
    implementation(libs.datastore.preferences)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}

fun currentOs(): OS {
    val os = System.getProperty("os.name")
    return when {
        os.equals("Mac OS X", ignoreCase = true) -> OS.MAC
        os.startsWith("Win", ignoreCase = true) -> OS.WINDOWS
        os.startsWith("Linux", ignoreCase = true) -> OS.LINUX
        else -> error("Unknown OS name: $os")
    }
}

enum class OS {
    LINUX,
    WINDOWS,
    MAC
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        allWarningsAsErrors.set(false)
        freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
    }
}

compose.desktop {
    application {
        mainClass = "com.jetpackduba.gitnuro.MainKt"

        nativeDistributions {
            includeAllModules = true
            packageName = projectName
            version = projectVersionSimplified
            description = "Multiplatform Git client"
            targetFormats(TargetFormat.Dmg)

            windows {
                iconFile.set(project.file("../icons/icon.ico"))
            }

            macOS {
                jvmArgs(
                    "-Dapple.awt.application.appearance=system"
                )
                iconFile.set(project.file("../icons/icon.icns"))
                bundleID = appPackageName
                signing {
                    sign.set(true)
                    identity.set(providers.environmentVariable("SIGNING_IDENTITY"))
                }
                notarization {
                    val providers = project.providers
                    appleID.set(providers.environmentVariable("NOTARIZATION_APPLE_ID"))
                    password.set(providers.environmentVariable("NOTARIZATION_PASSWORD"))
                    teamID.set(providers.environmentVariable("NOTARIZATION_TEAM_ID"))
                }
            }
        }
    }
}


// Compose's native (Skia) runtimes of every supported platform, only used by fatJarUniversal
val universalRuntime by configurations.creating

dependencies {
    universalRuntime(compose.desktop.linux_x64)
    universalRuntime(libs.compose.desktop.linux.arm64)
    universalRuntime(compose.desktop.windows_x64)
    universalRuntime(compose.desktop.macos_x64)
    universalRuntime(compose.desktop.macos_arm64)
}

fun Jar.configureFatJar(baseName: String, extraClasspath: Configuration? = null) {
    archiveBaseName.set(baseName)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Implementation-Title"] = name
        attributes["Implementation-Version"] = projectVersion
        attributes["Main-Class"] = "com.jetpackduba.gitnuro.MainKt"
    }

    val classpath = listOfNotNull(configurations.runtimeClasspath.get(), extraClasspath).flatMap { it.files }

    from(classpath.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude(
            "META-INF/MANIFEST.MF",
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
        )
    }
    with(tasks.jar.get() as CopySpec)
}

tasks.register("fatJarLinux", type = Jar::class) {
    val archSuffix = if (isLinuxAarch64) {
        "arm_aarch64"
    } else {
        "x86_64"
    }

    configureFatJar("$projectName-linux-$archSuffix-$projectVersion")
}

// Runs on Linux, Windows and macOS (x64 and arm64), as the project has no native code of its own
tasks.register("fatJarUniversal", type = Jar::class) {
    configureFatJar("$projectName-universal-$projectVersion", universalRuntime)
}

tasks.register("tasksList") {
    println("Tasks")
    tasks.forEach {
        println("- ${it.name}")
    }
}

