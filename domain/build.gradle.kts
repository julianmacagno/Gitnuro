plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(project(":common"))

    // TODO Remove this after refactor
    implementation(compose.desktop.currentOs)

    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    implementation(libs.coroutines)


    // TODO This should be removed after refactor is finished
    implementation(libs.jgit.core)
    implementation(libs.jgit.lfs)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client)

}
