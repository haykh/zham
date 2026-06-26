# World Clock — Android app + widget (localized NixOS dev env)

A minimal Jetpack Compose app showing the time in several cities, plus a Glance
home-screen widget. Developed entirely from this folder with **nvim or VS Code**
(no Android Studio), and torn down completely with two commands.

## Stack
- **Kotlin + Jetpack Compose** — app UI (`MainActivity.kt`, `Clocks.kt`)
- **Glance** — the home-screen widget (`WorldClockWidget*.kt`); Glance is "Compose for App Widgets"
- **devenv** (`devenv.nix`) — pins JDK 17, Gradle, a single Android SDK platform/build-tools, one emulator system image, plus the LSPs/formatters your editor needs

## (a) Everything heavy stays in this directory
| Thing | Location | Remove with |
|---|---|---|
| AVD disk + snapshots | `./.cache/android` | `rm -rf .cache` |
| Gradle deps + cache | `./.cache/gradle` | `rm -rf .cache` |
| SDK / emulator / system image binaries | `/nix/store` (shared, immutable) | `nix store gc` |

Nothing is written to `~/.android`, `~/.gradle`, or `~/Android/Sdk`.
`rm -rf .cache && nix store gc` wipes 100% of it.

## (b) "Hot reload" without an IDE
There is no stateful hot reload outside Android Studio. What you get instead is
an automatic **rebuild → reinstall-in-place → relaunch** loop, run by `watch-app`:

```sh
watch-app   # watches app/src + app/build.gradle.kts, redeploys on every save
```

- Uses `gradlew installDebug` — an incremental update install, so the app is **not
  uninstalled** and its data is preserved (no manual uninstall/reinstall).
- For an app this small the loop is a few seconds, then `MainActivity` relaunches
  automatically. You lose in-memory state across edits; that's the one thing only
  Android Studio's Live Edit can keep.

## Editor setup
Both editors use `kotlin-language-server` (provided by the dev shell).

- **Tooling on PATH**: launch your editor from inside `devenv shell`, or install
  [direnv](https://direnv.net), add an `.envrc` containing `use devenv`, and run
  `direnv allow` once — it then auto-loads the environment (and `adb`, the LSPs,
  formatters) whenever you `cd` in.
- **nvim** (LazyVim): add to `lua/plugins/lang.lua` →
  - `nvim-lspconfig` servers: `kotlin_language_server = {}`, `lemminx = {}`
  - `conform` `formatters_by_ft`: `kotlin = { "ktlint" }` (XML formats via lemminx LSP fallback)
  - treesitter `ensure_installed`: `"kotlin"`
- **VS Code**: install the **fwcd "Kotlin"** extension (it drives `kotlin-language-server`);
  Kotlin completion for Android/Compose symbols is decent but not as deep as Android Studio.

## First-time setup
```sh
devenv shell                             # enter the dev shell (first run downloads the SDK)
gradle wrapper --gradle-version 8.10.2   # generate ./gradlew (once)
create-avd                               # build the emulator image into ./.cache
```

## Each session
```sh
devenv shell         # (or just cd in, if direnv is set up)
run-emulator &       # boot the localized emulator; wait for the home screen
watch-app            # live-redeploy loop — edit a .kt and watch it reload
```

Place the widget: long-press the emulator home screen → Widgets → **World Clock**.

Manual one-shot build/run (instead of `watch-app`):
```sh
./gradlew installDebug
adb shell am start -n com.example.worldclock/.MainActivity
```
