# TODO

- [ ] **Make Gitnuro a real installable app on every OS.** Find a way to build the app, distribute it and install it on the system. Today it only ships as a fat JAR, so on Ubuntu it doesn't show up as an app with an icon that can be pinned to the taskbar/dock. It should install like any native app (menu entry, icon, uninstall). `compose.desktop` only declares `TargetFormat.Dmg` (macOS) and there is a `gitnuro.iss` for Windows: check what already works on each OS (Linux `.deb`/`.rpm`/AppImage/Flatpak with a `.desktop` file and icon, Windows installer, macOS `.dmg`) and what is missing. Branch: `feature/native-installers`
- [ ] **Replace app storage with SQLite + Liquibase.** Move app persistence to a SQLite database with Liquibase-managed migrations. Branch: `feature/sqlite-liquibase-storage`
- [ ] **Persist the selected tab when switching projects.** Switching to another project and back loses the tab that was open. Branch: `fix/persist-tab-on-project-switch`
