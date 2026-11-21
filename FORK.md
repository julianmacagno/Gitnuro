# GitIA: how this fork differs from upstream

GitIA is a fork of [Gitnuro](https://github.com/JetpackDuba/Gitnuro), developed entirely with AI.
We keep pulling upstream improvements, but we deliberately **do not** follow it in the areas below.
Read this before every upstream merge and keep it up to date.

## Intentional divergences (do not take from upstream)

| Area | Our decision | Upstream | Commits |
|---|---|---|---|
| Rust native library | Removed. Pure JVM | Uses Rust/uniffi (`rs/`, `useCross`, `isRustRelease`) | `853b4356`, `44db5ad1`, `754ccb40` |
| GraalVM native image | Not used. We ship a universal JAR (`fatJarUniversal`) because native images can't be cross-compiled (e.g. for macOS) | `org.graalvm.buildtools.native` plugin and `--graalvm` flag | `f952d21b` (upstream) |
| Icons | Own SVGs in `composeResources/drawable`. No `material-icons-*` dependency | `Icons.Default.*` | `aa971cd4` |
| Fonts | Only the weights actually used | All Inter / NotoSansMono weights | `8d442f82` |
| Persistence | No SQLite / Liquibase | Not applicable | `f41d44c6` |
| Java | Bytecode targets Java 17 (`-Xjdk-release=17`), so it runs on Java 17 or newer. Build still uses the JDK 25 toolchain | Bytecode targets Java 21 | `1d0b59df` |
| Name and distribution | Renamed to GitIA, with self installers (`install.sh`, `install.ps1`, `gitnuro.iss`) | Gitnuro | `f1d02d9c` |
| User-facing texts | Say "GitIA", not "Gitnuro" (also in extracted `strings.xml` entries) | Say "Gitnuro" | |

## Features that only exist here

Repository groups for tabs, automatic fetch of all remotes, staging and discarding by text selection,
PageUp/PageDown scrolling in diff and log, opening several repositories from the folder picker.

## Merging upstream

```bash
git fetch origin
git log --oneline develop..origin/main        # what is new
git switch -c sync/upstream-YYYY-MM develop   # merge in a throwaway branch
git merge --no-commit --no-ff origin/main
# review each file against the table above, drop what we don't want, then commit
```

- `git rerere` is enabled in this repo: repeated conflict resolutions are reused automatically.
- Merge often: small merges are cheap, big ones are painful.
- Once a merge is committed, Git considers all upstream commits merged, so a rejected feature doesn't come back.
- Upstream edits to files we deleted show up as modify/delete conflicts: resolve with `git rm`.
- New upstream texts must be adapted to "GitIA" and any `Icons.Default.*` replaced by our own drawables.
- Prefer new files over edits to shared upstream code: it keeps future conflicts small.
