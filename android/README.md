# Android FreeMind-MMX

Modern Android client for FreeMind-compatible mind maps, including FreeMind-MMX
sidecar behavior. Legacy desktop FreeMind sources remain under `/freemind` and
are **not** compiled into this app.

## Status (Milestone 1)

| Capability | Status |
|------------|--------|
| Project skeleton + Material 3 shell | Done |
| Module boundaries (core / format / layout / app) | Done |
| Open / parse real `.mm` | Milestone 2 |
| Mind-map canvas viewer | Milestone 3 |
| Editing + undo/redo | Milestone 4 |
| `.mm` writer + round-trip tests | Milestone 5 |
| `.mmx` join/split | Milestone 6 |
| SAF polish, intents, recents | Milestone 7 |

See [`docs/android-port-analysis.md`](../docs/android-port-analysis.md) and
[`docs/adr/`](../docs/adr/).

## Modules

| Gradle module | Role |
|---------------|------|
| `:app` | Compose UI, Android integration |
| `:mindmap-core` | Domain model + editing/undo (JVM) |
| `:freemind-format` | `.mm` / `.mmx` I/O (JVM) |
| `:mindmap-layout` | Layout geometry (JVM) |

## Requirements

- JDK 17+
- Android SDK with `platforms;android-35` and Build-Tools 35
- Set `sdk.dir` in `local.properties` (or `ANDROID_HOME`)

## Build

```bash
cd android
./gradlew :app:assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

## Test

```bash
cd android
./gradlew test
```

JVM unit tests cover `:mindmap-core`, `:freemind-format`, and `:mindmap-layout`.
They do not require an emulator.

## Lint

```bash
cd android
./gradlew :app:lintDebug
```

## Compatibility notes

- Primary documents remain FreeMind `.mm` files (UTF-8).
- FreeMind-MMX stores fold/timestamps in a hidden `.<name>.mmx` sidecar.
  Android SAF may not always allow automatic sibling creation; see ADR 0002.
- Unknown FreeMind features should be preserved where practical rather than
  inventing a proprietary format.

## License

FreeMind is GPL-2.0-or-later. This Android port lives in the same repository
and is intended to remain GPL-compatible. Prefer clean-room reimplementation of
format/domain logic over copying Swing-coupled desktop classes.
