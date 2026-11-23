# Contributing

Thanks for your interest in Secret Santa! Bug reports, ideas and pull requests are all welcome.

## Reporting bugs and ideas

Open an [issue](https://github.com/Chibisueru/secret-santa/issues/new/choose) and pick a template. For bugs, include the app version (Settings → About) and your phone's Android version. Please blur any real names or contact details in screenshots.

## Development setup

1. Install JDK 17 or newer and the Android SDK, either through Android Studio or the command-line tools.
2. Install SDK platform **android-37.2**: `sdkmanager "platforms;android-37.2"`.
3. Clone the repository and open it in Android Studio, or build from the terminal:

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # install on a device or emulator
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android Lint
```

Start with [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for a tour of the code.

## Making a change

1. Create a branch: `feature/short-description` or `fix/short-description`.
2. Keep changes focused. One pull request should do one thing.
3. Add or update unit tests for anything in `domain/`.
4. Make sure `./gradlew testDebugUnitTest lintDebug assembleDebug` passes. CI runs the same checks.
5. If users will notice the change, add a line under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md).
6. If you change the database schema, follow "Schema changes" in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#schema-changes). A missing migration can wipe people's data.

## Code style

- Kotlin official style (`kotlin.code.style=official`); 4-space indents, trailing commas.
- Compose: hoist state, pass callbacks down, and keep screens free of database code.
- UI text is plain English for now. Put user-facing strings in one place in the composable, so they're easy to move to resources when translations are added.
- Commit messages: a short imperative summary line ("Add rule editing"), then details if needed.

## License

By contributing, you agree that your contributions are licensed under the [GNU GPL v3.0](LICENSE), the same license as the project.
