# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
See [docs/RELEASING.md](docs/RELEASING.md) for how versions are numbered.

## [Unreleased]

## [0.1.0-alpha.1] - 2026-11-23

First public alpha.

### Added
- Groups with event details: name, budget, gift exchange date, location and notes.
- Participants with optional phone number and email.
- Organizer details when creating a group, with a switch for whether the organizer takes part in the draw.
- Exclusion rules, one-way or both ways.
- A fair random draw that respects all rules and explains when no valid draw exists.
- A share screen that sends each person their match by SMS, email or the share sheet, and tracks who has been sent their match.
- Encoded match names: the recipient's name appears as a Base64 code in brackets, e.g. `[TcOhdMOp]`.
- A Decoder tab in a new bottom dock that finds the code in a pasted message and reveals the name only on tap.
- "Reveal all pairings" (behind a confirmation), plus re-draw and reset draw.
- Festive and Material You themes, light/dark/system mode, and optional snowfall.
- An About section in Settings showing the app version and project links.

### Notes
- The app ID is now `io.github.chibisueru.secretsanta`, and releases are signed with the project's release key. If you tried an earlier test build, uninstall it first. That build used a different app ID, so it can't be updated in place.

[Unreleased]: https://github.com/Chibisueru/secret-santa/compare/v0.1.0-alpha.1...HEAD
[0.1.0-alpha.1]: https://github.com/Chibisueru/secret-santa/releases/tag/v0.1.0-alpha.1
