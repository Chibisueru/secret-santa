# Secret Santa 🎅

[![CI](https://github.com/Chibisueru/secret-santa/actions/workflows/ci.yml/badge.svg)](https://github.com/Chibisueru/secret-santa/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/Chibisueru/secret-santa?include_prereleases&label=release)](https://github.com/Chibisueru/secret-santa/releases)
[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

An offline Android app for organizing Secret Santa gift exchanges. Add people, set rules, draw names, and send everyone their match by SMS, email or any messaging app. The recipient's name is encoded, so even the person sending the messages doesn't see who has whom.

> [!WARNING]
> **This is alpha software.** It works, but expect rough edges. The data format may still change, and updates are not yet guaranteed to keep your groups. Please [report anything that goes wrong](https://github.com/Chibisueru/secret-santa/issues).

## Features

- **Groups** for family, friends or work, each with its own people, rules and draw.
- **Organizer setup**: enter your own details and choose whether you're taking part in the draw.
- **Event details**: budget, date, location and notes are included in every match message.
- **Exclusion rules**: "Anna can't draw Mark", one way or both ways (for couples).
- **Fair draw**: every valid result is equally likely. If the rules make a draw impossible, the app tells you who's causing it.
- **Private sharing**: each person gets their own message. The match is written as a Base64 code like `[TcOhdMOp]`, and the app tracks who has been sent their match.
- **Built-in decoder**: recipients paste their code or the whole message, then tap to reveal the name.
- **Material 3 design**: a festive red, green and gold theme or Material You wallpaper colors, light and dark mode, and optional snowfall.
- **Private by design**: no account, no internet access, no permissions. Everything stays on your phone.

## Install

1. Download `SecretSanta-<version>.apk` from the [latest release](https://github.com/Chibisueru/secret-santa/releases).
2. Open it on your phone and allow installing from that source when Android asks.

Requires Android 8.0 or newer. Each release also includes a `.sha256` file, if you want to verify the download.

## Documentation

| Document | For |
| --- | --- |
| [User guide](docs/USER_GUIDE.md) | Running a Secret Santa with the app, step by step |
| [Architecture](docs/ARCHITECTURE.md) | How the code is organized and how the draw works |
| [Releasing](docs/RELEASING.md) | Version numbers, signing and publishing a release |
| [Contributing](CONTRIBUTING.md) | Building the project and sending changes |
| [Changelog](CHANGELOG.md) | What changed in each version |

## Building from source

Requirements: JDK 17 or newer, and the Android SDK with platform **android-37.2**.

```bash
git clone https://github.com/Chibisueru/secret-santa.git
cd secret-santa
./gradlew assembleDebug      # → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # install on a connected device
./gradlew testDebugUnitTest  # unit tests
```

Debug builds install as a separate app ("…secretsanta.debug"), so they don't replace a released version on the same phone.

## License

Copyright © 2026 Chibisueru

Secret Santa is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License v3.0](LICENSE) or (at your option) any later version.
