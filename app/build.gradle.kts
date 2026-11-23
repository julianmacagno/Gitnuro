import org.gradle.jvm.tasks.Jar

plugins {    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
}

val javaLanguageVersion = JavaLanguageVersion.of(25)

// Remember to update Constants.APP_VERSION when changing this version
val projectVersion = "2.2"

val projectName = "GitIA"

val appPackageName = "com.jetpackduba.gitnuro"
group = appPackageName
version = projectVersion

dependencies {
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(compose.desktop.currentOs)

    implementation(libs.compose.ui.util)
    implementation(libs.compose.components.animatedimage)
    implementation(libs.compose.components.resources)
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

    implementation(libs.slf4j.reload4j)

    implementation(libs.bouncycastle)

    implementation(libs.ktor.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)

    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}

tasks.processResources {
    from("..") {
        include("LICENSE", "THIRD-PARTY-NOTICES.md", "licenses/*")
        into("META-INF/gitnuro")
    }
    from("../icons") {
        include("icon.png", "icon.ico", "icon.icns")
        into("installer")
    }
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

tasks.register<JavaExec>("run") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.jetpackduba.gitnuro.MainKt")
    // Same memory flags as the installed launchers (Installer.jvmFlags)
    jvmArgs("-XX:+UseSerialGC", "-Xms16m", "-Xmx2g", "-XX:MinHeapFreeRatio=10", "-XX:MaxHeapFreeRatio=25", "-XX:TieredStopAtLevel=1")
    environment("MALLOC_ARENA_MAX", "2")
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
        attributes["Add-Opens"] = "java.desktop/sun.awt.X11"
    }

    val classpath = listOfNotNull(configurations.runtimeClasspath.get(), extraClasspath).flatMap { it.files }

    classpath.filter { it.isFile }.forEach { dependencyJar ->
        from(zipTree(dependencyJar)) {
            include("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/COPYING*")
            into("META-INF/licenses/${dependencyJar.nameWithoutExtension}")
        }
    }

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

// Runs on Linux, Windows and macOS (x64 and arm64), as the project has no native code of its own
tasks.register("fatJarUniversal", type = Jar::class) {
    configureFatJar("$projectName-universal-$projectVersion", universalRuntime)
}

