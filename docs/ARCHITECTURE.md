# Architecture

Secret Santa is a single-module Android app written in Kotlin with Jetpack Compose and Material 3. It runs entirely offline: there is no backend, no network access and no permissions.

## Stack

| Concern | Library |
| --- | --- |
| UI | Jetpack Compose, Material 3, Material Icons Extended |
| Navigation | Navigation Compose (string routes) |
| State | ViewModel + Kotlin `StateFlow` |
| Storage | Room (SQLite) for groups and participants; DataStore Preferences for settings |
| Build | Gradle (Kotlin DSL) with a version catalog, AGP 9 with built-in Kotlin, KSP for Room |

Versions live in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml).

## Source layout

```
app/src/main/java/io/github/chibisueru/secretsanta/
├── SecretSantaApp.kt     Application: creates the database, settings and an app-wide scope
├── MainActivity.kt       Loads settings, applies the theme, hosts navigation
├── data/
│   ├── Entities.kt       GroupEntity, ParticipantEntity, ExclusionEntity, GroupSummary
│   ├── SantaDao.kt       All queries, plus transactions (draw, organizer changes)
│   ├── SantaDatabase.kt  Room database and schema migrations
│   └── SettingsRepository.kt  Theme, dark mode and snowfall (DataStore)
├── domain/               Pure Kotlin, unit-tested
│   ├── SecretSantaDraw.kt  The draw algorithm
│   ├── NameCode.kt       Base64 encoding/decoding of names
│   ├── MatchMessage.kt   The message text sent to each person
│   └── Formatting.kt     Date formatting
└── ui/
    ├── AppNavHost.kt     Routes and the bottom dock
    ├── groups/           Home: list of groups
    ├── newgroup/         Create a group with organizer details
    ├── group/            Group editor (Details / People / Rules) + GroupViewModel
    ├── share/            Send matches; SMS/email/share intents
    ├── decoder/          Decoder tab
    ├── settings/         Settings and About
    ├── theme/            Festive color schemes, Material You
    └── components/       Shared UI: dock, person fields, snowfall, avatars…
```

There's no dependency-injection framework. `SecretSantaApp` holds the few app-wide objects, and screens create their ViewModels with `viewModel { … }` initializers.

## Data model

```
santa_groups 1 ──< participants 1 ──< exclusions (fromId, toId)
     │                   │
     │                   └─ assignedToId → participants.id   (who they give to)
     └─ organizerName / organizerPhone / organizerEmail
```

- A **group** is a draft while `drawnAt` is null. Once names are drawn, `drawnAt` is set and the UI locks people and rules until the draw is reset.
- A **participant** whose `isOrganizer` is true is the organizer taking part in the draw. Turning participation off deletes that row. The organizer's details stay on the group.
- An **exclusion** means "`fromId` must not draw `toId`". A two-way rule is stored as two rows. The UI merges them back into one ("A & B").
- Deleting a group or participant cascades to its participants and rules.

### Schema changes

Room's database version is in `SantaDatabase`. Every schema change must:

1. Increase `version`.
2. Add a `Migration(old, new)` to `SantaDatabase.create()`. New `NOT NULL` columns need a `DEFAULT`, matched on the entity with `@ColumnInfo(defaultValue = …)`.
3. Be mentioned in `CHANGELOG.md`.

| Version | Change |
| --- | --- |
| 1 | Initial schema |
| 2 | Organizer columns on `santa_groups`; `isOrganizer` on `participants` |

## The draw

[`SecretSantaDraw`](../app/src/main/java/io/github/chibisueru/secretsanta/domain/SecretSantaDraw.kt) turns a list of people and rules into a giver → receiver mapping in which nobody draws themselves or someone they're excluded from.

1. **Quick checks.** If anyone has nobody they're allowed to draw, or nobody is allowed to draw someone, the draw fails with that person's name in the message.
2. **Random shuffles.** Up to 2,000 times, shuffle the receivers and accept the first valid result. Every valid result is equally likely.
3. **Backtracking.** If the rules are so tight that shuffling rarely works, a randomized backtracking search finds a valid result if one exists, trying the most constrained givers first. If none exists, the draw fails with a general message.

The result is saved in one transaction (`SantaDao.saveDraw`).

## Sending matches

[`MatchMessage`](../app/src/main/java/io/github/chibisueru/secretsanta/domain/MatchMessage.kt) builds the text. The recipient's name goes through [`NameCode`](../app/src/main/java/io/github/chibisueru/secretsanta/domain/NameCode.kt): UTF-8 → Base64, wrapped in `[brackets]` so the decoder can find it inside a whole pasted message.

[`ShareActions`](../app/src/main/java/io/github/chibisueru/secretsanta/ui/share/ShareActions.kt) opens:

- `ACTION_SENDTO smsto:` when the person has a phone number,
- `ACTION_SENDTO mailto:` when they have an email,
- otherwise the `ACTION_SEND` share sheet. This is also the fallback when no SMS or email app is installed.

None of these needs a permission. The app can't know whether the message was actually sent, so a person is marked as sent when a messaging app opens, and the organizer can undo that.

## Theming

`SecretSantaTheme` picks the hand-tuned festive color schemes in `theme/Color.kt`, or `dynamicLight/DarkColorScheme` when Material You is selected and the phone runs Android 12+. `MainActivity` waits for the saved settings before drawing anything, so the wrong theme never flashes on launch.

## Tests

Unit tests in `app/src/test` cover the domain layer:

- `SecretSantaDrawTest`: validity across many seeds, couples rules, a heavily constrained single-solution case, and impossible inputs.
- `NameCodeTest`: UTF-8 round trips, loose input (whitespace, missing padding, whole messages), and rejecting garbage.

Run them with `./gradlew testDebugUnitTest`. CI runs them, plus lint and a debug build, on every push and pull request.
