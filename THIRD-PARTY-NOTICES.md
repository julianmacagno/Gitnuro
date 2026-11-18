# Third-party notices

GitIA, a fork of Gitnuro, is distributed under the GNU General Public License v3.0 (see `LICENSE`). It bundles the following third-party
components, each under its own license. This list is not a substitute for the license files shipped inside each
library.

| Component                                                                                 | License                                         | Source                                              |
|-------------------------------------------------------------------------------------------|-------------------------------------------------|-----------------------------------------------------|
| Kotlin and kotlinx (coroutines, serialization)                                            | Apache 2.0                                      | https://kotlinlang.org/                             |
| Compose Multiplatform, Skiko, AndroidX (DataStore, Lifecycle, Navigation)                 | Apache 2.0                                      | https://www.jetbrains.com/lp/compose-multiplatform/ |
| Google Dagger                                                                             | Apache 2.0                                      | https://dagger.dev/                                 |
| Ktor                                                                                      | Apache 2.0                                      | https://ktor.io/                                    |
| Coil                                                                                      | Apache 2.0                                      | https://coil-kt.github.io/coil/                     |
| kotlin-logging                                                                            | Apache 2.0                                      | https://github.com/oshai/kotlin-logging             |
| Apache MINA SSHD                                                                          | Apache 2.0                                      | https://mina.apache.org/sshd-project/               |
| JGit                                                                                      | Eclipse Distribution License 1.0 (BSD-3-Clause) | https://www.eclipse.org/jgit/                       |
| Bouncy Castle                                                                             | Bouncy Castle License (MIT)                     | https://www.bouncycastle.org/                       |
| SLF4J and reload4j                                                                        | MIT / Apache 2.0                                | https://www.slf4j.org/                              |
| Inter font                                                                                | SIL Open Font License 1.1                       | https://github.com/rsms/inter                       |
| Noto Sans Mono font                                                                       | SIL Open Font License 1.1                       | https://github.com/notofonts/latin-greek-cyrillic   |
| Material Design icons (most of the SVG icons in `app/src/main/composeResources/drawable`) | Apache 2.0                                      | https://github.com/google/material-design-icons     |

The full text of the SIL Open Font License 1.1 is in `licenses/OFL-1.1.txt`. The Apache 2.0 text is at
https://www.apache.org/licenses/LICENSE-2.0 and the GPLv3 text is in `LICENSE`.

Gitnuro itself was created by Abdelilah El Aissaoui and contributors (https://github.com/JetpackDuba/Gitnuro). This
repository is an unofficial fork, modified since November 2026, and is not affiliated with or endorsed by the original
author.

The fat jar keeps every bundled library's own `LICENSE` and `NOTICE` files under `META-INF/licenses/<library>/`.
