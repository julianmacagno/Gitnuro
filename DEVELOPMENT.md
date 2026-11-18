# Developing GitIA

## Requirements

- **JDK 17 or higher**: You don't need this if you only use the JDK installed by IntelliJ IDEA. If you want to build
  using the CLI, check this [section](#alternative-setting-up-jdk-for-use-on-cli).

## Setting up an IDE

If you don't have another preference, the recommendation is to download and install
[IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/)
(possibly through the JetBrains Toolbox, if you have it already). The recommended plugins to improve the DX are:

- [Compose Multiplatform IDE Support](https://plugins.jetbrains.com/plugin/16541-compose-multiplatform-ide-support)

Building requires JDK 25, the Gradle toolchain version (`JavaLanguageVersion` in `app/build.gradle.kts`). The produced
bytecode targets Java 17 (`buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`), so the app runs on Java 17 or newer. Code
must not use JDK APIs newer than 17; the compiler enforces it with `-Xjdk-release=17`.

## Alternative: Setting up JDK for use on CLI

You don't need this if you only use the JDK installed by IntelliJ IDEA and build using the IDE.

Check which Java version this project currently uses (`cat build.gradle.kts | grep JavaLanguageVersion`) and install it.
For instance on Debian-based systems, you'd run:

```bash
sudo apt install openjdk-25-jdk
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
```

Once it works (e.g. `./gradlew build`). On Linux, you may want to add that latter line to your `/etc/environment` or
user-specific files such as `.profile` or `.bashrc`.

## Running the app / unit tests

From the "Gradle" window in the IDE, under "Tasks" select "compose desktop > run" or "verification > test"
for the main app or unit tests, respectively, and run it.
Next time, it will already be in the "Run Configurations" at the top right of the IDE, so you
won't have to open the "Gradle" window again.
You can also run these in debug mode and set break points in the code.

Alternatively on CLI: `./gradlew run` or `./gradlew test`.

## Releasing

The app checks `latest.json` in this repository (`AppConstants.VERSION_CHECK_URL`) and shows an update notice when its
`appCode` is greater than `AppConstants.APP_VERSION_CODE`. To release:

1. Bump `AppConstants.APP_VERSION` and `AppConstants.APP_VERSION_CODE` (always increase the code), `projectVersion` and
   `projectVersionSimplified` in `app/build.gradle.kts`.
2. Update `appVersion` and `appCode` in `latest.json` with the same values.
3. Merge to `main`, push the tag (`git tag v<version> && git push --tags`) and publish the draft release created by the
   workflow. `latest.json` must reach `main` only after the release is published, otherwise users get a notice with
   nothing to download.
