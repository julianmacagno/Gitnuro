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

Check which Java version this project currently uses (`grep JavaLanguageVersion app/build.gradle.kts`) and install it.
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

## Memory

The launchers written by `--install` (and `./gradlew run`) start the JVM with memory-saving flags, listed in
`Installer.jvmFlags`: serial GC with a shrinking heap, C1 only, and on Linux Skia's software renderer and a periodic `malloc_trim`. Without them an
idle window takes ~330 MB and 20 open repositories ~1.1 GB. Set `GITIA_JAVA_OPTS` to override any of them, e.g.
`GITIA_JAVA_OPTS="-XX:TieredStopAtLevel=4"` brings C2 back if big diffs feel slow. The Linux and macOS launchers append it;
a Windows shortcut can't read variables, so there the app restarts itself once with it (`Installer.relaunchWithJavaOpts`). The flags are plain HotSpot options valid on every OS; only `MALLOC_ARENA_MAX` and `TrimNativeHeapInterval` (glibc)
and Skia's software renderer are Linux-only, since macOS and Windows render through Metal/Direct3D.

To measure, `java [jvm flags] -Dgitia.bench.groups="/a,/b;/c,/d" -Dgitia.bench.out=out.tsv -jar GitIA.jar` drives the
real UI with `BenchScenario`: it opens each group of repositories (`;` separates groups, `,` repositories), scrolls the
log, selects commits and opens diffs, rotates the tabs, closes the group, samples RSS/heap at each step and exits. It
never fetches, but it opens tabs in the app's settings: point `XDG_CONFIG_HOME` (Linux) or `-Duser.home` (macOS,
Windows) to a throwaway directory. RSS comes from `/proc` on Linux, `ps` on macOS and PowerShell on Windows.
`-Dgitia.bench.limit=5` limits the repos per group for a quick run. Add `-XX:NativeMemoryTracking=summary` and run
`jcmd <pid> VM.native_memory summary` to see where the native memory goes. `-Dgitia.bench.hold=<seconds>` keeps the app
idle with the group open, so heap dumps (`jcmd <pid> GC.heap_dump -gz=1 heap.hprof.gz`), class histograms
(`GC.class_histogram`) and memory maps (`System.map`) can be taken then.

Keep Compose UI state that outlives its composition free of layout references: a `LazyListState` kept in a ViewModel
retains the whole UI tree it last measured (and Skia's native text memory). Hidden tabs used to keep ~50 MB of heap that
way, so the ViewModels expose their list states as separate flows and swap them for a copy at the same position when
the tab is hidden (`TabViewModel.onUiDetached`, `detachedCopy`). Never put one inside a `stateIn` initial value: it is
never released.

## Releasing

The app checks `latest.json` in this repository (`AppConstants.VERSION_CHECK_URL`) and shows an update notice when its
`appCode` is greater than `AppConstants.APP_VERSION_CODE`. To release:

1. Bump `AppConstants.APP_VERSION` and `AppConstants.APP_VERSION_CODE` (always increase the code), and `projectVersion` in
   `app/build.gradle.kts`.
2. Update `appVersion` and `appCode` in `latest.json` with the same values.
3. Merge to `main`, push the tag (`git tag v<version> && git push --tags`) and publish the draft release created by the
   workflow. `latest.json` must reach `main` only after the release is published, otherwise users get a notice with
   nothing to download.

## Keeping up with upstream

See [FORK.md](FORK.md).
