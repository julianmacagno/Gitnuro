# GitIA - An enhanced, AI-developed fork of Gitnuro

[![Latest release](https://img.shields.io/github/v/release/julianmacagno/Gitnuro?color=blue&label=latest%20release)](https://github.com/julianmacagno/Gitnuro/releases/latest)

<img src="icons/logo.svg" alt="GitIA icon" width="128">

> **GitIA is an unofficial, enhanced, AI-developed fork** of [Gitnuro](https://github.com/JetpackDuba/Gitnuro), created by Abdelilah El Aissaoui
> ([JetpackDuba](https://github.com/JetpackDuba)) and contributors. It is not affiliated with or endorsed by the original
> author. This fork modifies the original code since November 2026 (see the git history for every change) and is
> distributed under the same license (GPLv3). Please report problems of this fork in its own issue tracker, not in the
> original project's. The official builds are in the
> [original repository](https://github.com/JetpackDuba/Gitnuro/releases/latest).

A FOSS Git client based on (Jetbrains) Compose and JGit.

The main goal of GitIA is to provide a multiplatform open source Git client without any kind of constraint to how you
can use it nor relying on web technologies.

## Download/Install

One-line installer for Linux, macOS and Windows. It downloads the latest release and installs GitIA for the current
user (menu entry, icon, uninstall). Requires Java 17 or newer.

Linux and macOS:

```sh
curl -fsSL https://raw.githubusercontent.com/julianmacagno/Gitnuro/main/install.sh | sh
```

Windows (PowerShell):

```powershell
irm https://raw.githubusercontent.com/julianmacagno/Gitnuro/main/install.ps1 | iex
```

Uninstall: `java -jar GitIA-universal.jar --uninstall` (on Windows also from "Installed apps"). To publish a release, push a tag (`git tag v2.1.1 && git push --tags`) and publish the draft
created by the workflow; the installers always download the latest published release.

Official builds of the original project (not this fork):

Linux:

- Available as Flatpak [here](https://flathub.org/apps/details/com.jetpackduba.Gitnuro) or by
  running `flatpak install com.jetpackduba.Gitnuro` if you have Flatpak & Flathub already setup.
    - If Flathub is not set up, you can add the source with the following
      command `flatpak remote-add --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo`.

Windows:

- [Installable & portable version](https://github.com/JetpackDuba/Gitnuro/releases/latest).

MacOS:

- [ZIP which contains the app bundle](https://github.com/JetpackDuba/Gitnuro/releases/latest).
- Using hombrew: `brew install jetpackduba/apps/gitnuro`

## Features

GitIA has support for the following features:

- View diffs for text based files.
- View your history log and all its branches.
- Add (stage) & reset (unstage) files.
- Stage & unstage of hunks.
- Checkout files (revert changes of uncommitted files).
- Clone.
- Commit.
- Reset commits.
- Revert commits.
- Amend previous commit.
- Merge.
- Rebase.
- Create and delete branches locally.
- Create and delete tags locally.
- View remote branches.
- Pull and push.
- Stash and pop stash.
- Checkout a commit (detached HEAD).
- View changes/diff in images (side to side comparison).
- Force push.
- Remove branches from remote.
- Manage remotes.
- Start a new local repository.
- Search by commit message/author/commit id.
- Rebase interactive.
- Blame file.
- View file history.
- Theming.
- Side by side diff in text files.
- Stage/Unstage specific lines.
- Submodules support.
- Change the tracking of a specific branch.

<details>
  <summary><b>Features planned</b></summary>

- Create/Apply patches
- Remove tags from remote.
- View stashes in the log tree.
- Syntax highlighting for diff.
- Various log options like showing the author, filtering by current branch o hide remote branches.
- Customizations settings.

</details>

## Contributing

If you find a bug or you would like to suggest a new feature, feel free to open an issue.

Pull requests are also welcome but please create an issue first if it's a new feature. If you want to work on an
existing issue, please comment so I'm aware of it and discuss the changes if required.
See [this page](DEVELOPMENT.md) for how to set up your development environment.

## FAQ

> Is GitIA completely free?

Yes, free in both meanings of the word (in money and freedom).

> Does GitIA keep track of my data?

GitIA does not track data in any way, don't worry.

> I don't like the built-in themes, can I create a custom one?

GitIA includes the option to set custom themes in a JSON format. Keep in mind that themes may break with new releases,
making the default theme the fallback option.

For the latest stable version, you can use this JSON as an example:

```
{
    "primary": "FF456b00",
    "primaryVariant": "FF456b00",
    "onPrimary": "FFFFFFFFF",
    "secondary": "FF9c27b0",
    "onBackground": "FF141f00",
    "onBackgroundSecondary": "FF595858",
    "error": "FFc93838",
    "onError": "FFFFFFFF",
    "background": "FFe7f2d3",
    "backgroundSelected": "C0cee1f2",
    "surface": "FFc5f078",
    "secondarySurface": "FFedeef2",
    "tertiarySurface": "FFF4F6FA",
    "addFile": "FF32A852",
    "deletedFile": "FFc93838",
    "modifiedFile": "FF0070D8",
    "conflictingFile": "FFFFB638",
    "dialogOverlay": "AA000000",
    "normalScrollbar": "FFCCCCCC",
    "hoverScrollbar": "FF0070D8",
    "diffLineAdded": "FF0070D8",
    "diffLineRemoved": "FF0070D8",
    "onSecondary": "FF000000",
    "diffContentAdded": "FF000000",
    "diffContentRemoved": "FF000000",
    "diffKeyword": "FF000000", 
    "diffAnnotation": "FF000000",
    "diffComment": "FF000000",
    "isLight": true
}
```

Colors are in ARGB Hex format.

> Why isn't the Mac version signed?

The cost of the Apple Developer Program is quite high with a platform that currently has very few users. I may pay for
it if it's a very requested feature but not for now.

> Authentication has failed. What's wrong?

Currently there are some limitations regarding this topic. Here are some known problematic setups:

- Multicast DNS remote URL (https://github.com/JetpackDuba/Gitnuro/issues/19) with this
  workaround (https://github.com/JetpackDuba/Gitnuro/issues/19#issuecomment-1374431720).
- Self signed server certificate (https://github.com/JetpackDuba/Gitnuro/issues/48)

If the authentication fails and you think its due to a different reason, please open a new issue.


> Does it support Git credentials manager (aka manager-core)?

Yes, but it requires specifying the full path of the binary in your `.gitconfig`.

Example for linux:

```
[credential]
   helper = /usr/share/git-credential-manager-core/git-credential-manager-core
```

Example for windows (you may want to edit `C:\Program Files\Git\etc\gitconfig`):

```
[credential]
   helper = C:/Program Files/Git/mingw64/bin/git-credential-manager-core.exe
```

## Credits

This fork exists thanks to the work of **Abdelilah El Aissaoui** ([JetpackDuba](https://github.com/JetpackDuba)), who
created Gitnuro and has developed it for years, and of every
[contributor of the original project](https://github.com/JetpackDuba/Gitnuro/graphs/contributors). Almost all of the code
in this repository is theirs. If you find this fork useful, please consider
[sponsoring the original author](https://github.com/sponsors/JetpackDuba) and starring the
[original repository](https://github.com/JetpackDuba/Gitnuro).

Thanks also to JetBrains for the tooling and to the authors of the open source libraries this project uses.

## License

GitIA is free software licensed under the GNU General Public License v3.0 (see [LICENSE](LICENSE)). It comes with
ABSOLUTELY NO WARRANTY. Third-party components and their licenses are listed in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

