# Releasing

## Version numbers

The app version is defined in **one place**, [`gradle.properties`](../gradle.properties):

```properties
app.versionName=1.0.0
app.versionCode=4
```

### `versionName`: what people see

Uses [Semantic Versioning](https://semver.org): `MAJOR.MINOR.PATCH`, with an optional pre-release suffix.

| Part | Increase when… |
| --- | --- |
| `MAJOR` | A change breaks existing use, e.g. data that can't be migrated. |
| `MINOR` | New features are added. |
| `PATCH` | Only bugs are fixed. |
| `-alpha.N` | Early testing of a future version. Features may still change. |
| `-beta.N` | Feature-complete, being tested. Updates keep data. |
| `-rc.N` | Release candidate: becomes the stable release unless a bug turns up. |

Most releases are plain versions (`1.0.1` for fixes, `1.1.0` for features). Pre-release suffixes are optional and only needed when a bigger version should be tested before it ships:

```
1.0.0 → 1.0.1 → 1.1.0 → 2.0.0-beta.1 → 2.0.0-rc.1 → 2.0.0
```

Since 1.0.0, every update must keep users' data: any schema change needs a migration (see [ARCHITECTURE.md](ARCHITECTURE.md#schema-changes)).

### `versionCode`: what Android compares

A whole number that **must increase for every APK you publish**. Android refuses to install an update with an equal or lower code. Increase it by 1 with each release, and never reuse or decrease it.

| versionName | versionCode |
| --- | --- |
| (test builds) | 1–2 |
| 0.1.0-alpha.1 | 3 |
| 1.0.0 | 4 |

Debug builds add `-debug` to the name and `.debug` to the app ID, so they install next to the released app.

## Changelog

[`CHANGELOG.md`](../CHANGELOG.md) follows [Keep a Changelog](https://keepachangelog.com):

- While working, add entries under `## [Unreleased]` in `Added` / `Changed` / `Fixed` / `Removed` sections.
- At release time, rename that section to `## [x.y.z] - YYYY-MM-DD`, add a new empty `## [Unreleased]` above it, and update the comparison links at the bottom.

The release script uses that version's section as the GitHub Release notes.

## Signing

Releases are signed with the project's release key. **The same key must sign every release**, or Android refuses to install the update over the existing app.

The key and its passwords are kept **outside the repository**:

| What | Where |
| --- | --- |
| Keystore | `~/.android/keystores/secretsanta-release.jks` |
| Passwords and alias | `~/.gradle/gradle.properties` (`secretsanta.*` entries) |

`app/build.gradle.kts` reads these properties. Without them, release builds are produced unsigned and the release script stops.

> [!IMPORTANT]
> Back up the keystore file and the four `secretsanta.*` lines somewhere safe, such as a password manager. If they're lost, you can never publish an update that installs over existing copies of the app.

Certificate SHA-256 fingerprint, for verifying downloads:

```
E6:3F:38:81:42:AD:DB:06:E0:A0:14:3B:BB:54:68:B2:C9:13:9B:CD:4A:91:DB:1F:F8:3A:12:10:9E:D4:87:EA
```

Check an APK with `apksigner verify --print-certs SecretSanta-<version>.apk`.

## Release checklist

1. Make sure `main` is green in CI.
2. Update `app.versionName` and increase `app.versionCode` in `gradle.properties`.
3. Move the `[Unreleased]` changelog entries into a new version section and update the links.
4. Commit: `git commit -am "Release 1.0.1"` and `git push`.
5. Build and publish:

   ```bash
   scripts/release.sh            # dry run: test, lint, build and verify into dist/
   scripts/release.sh --publish  # also tag vX.Y.Z, push the tag and create the GitHub Release
   ```

   The script checks that the version format is valid, the changelog section exists, the tree is clean and the tag is new. Then it builds a signed, minified APK, verifies the signature, and writes a SHA-256 checksum. Versions with a suffix (`-alpha`, `-beta`, `-rc`) are published as **pre-releases**.

6. Install the APK from the release page on a real phone and check that it starts.

Each release includes `SecretSanta-<version>.apk` and `SecretSanta-<version>.apk.sha256`.

## Tags and branches

- `main` is always releasable; CI runs on every push and pull request.
- Each release is an annotated tag `v<versionName>`, e.g. `v1.0.0`.
- Larger work happens on branches (`feature/…`, `fix/…`) and is merged with a pull request.
