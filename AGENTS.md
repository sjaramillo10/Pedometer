# Nemi

## Project and build

- Nemi is a single Android module, `:app`. Its application ID and namespace
  are `dev.sjaramillo.nemi`, and debug builds add the `.debug` suffix.
- Read the Android SDK levels from `app/build.gradle.kts` and the minimum JDK
  from `settings.gradle.kts`. Use Kotlin DSL and manage dependency and plugin
  versions in `gradle/libs.versions.toml`.
- When updating Gradle, run
  `./gradlew wrapper --gradle-version <version> --distribution-type bin`
  twice so the wrapper scripts and properties stay synchronized.

## App architecture and data

- Keep `MainActivity` as the Android Views host. Handle lifecycle events, activity
  results, and privacy intents in `NemiRoute`. Have `HomeViewModel` expose a
  single `HomeUiState` and pass state and callbacks to `HomeScreen`.
- Refresh Health Connect on lifecycle and user events. Observe committed data
  through framework SQLite. Resolve one total per day, preferring Health Connect even when
  its total is zero. Use CSV backup totals when Health Connect has no total for
  that date and preserve `fromBackup`.
- Keep the SQLite database at version 1. Change the version or add migrations
  only with an explicit policy change.
- Preserve compatibility with existing CSV backups. Suggest
  `Nemi_YYYY-MM-DD.csv` for new exports using the device's local date when
  the user starts the export.

## Health Connect integration

- For phone-only queries, include `DataOrigin("android")` and add the current
  device origin when `HealthConnectManager.getCurrentDeviceDataSource` returns
  one. Wait for the callback and use the legacy origin alone when the
  current origin is unavailable.
- Keep the privacy alias qualified by `${applicationId}` so the system can
  resolve it in each build variant. When changing app identity or manifest
  wiring, inspect the built APK for the application ID, launcher activity,
  and privacy alias.

## Tests and local verification

- Name types for the responsibility they own. Use provider names for
  provider-specific contracts and domain names for provider-independent ones.
- Add deterministic tests under `app/src/test` for behavior changes. Name new
  and updated tests `givenCondition_whenAction_thenExpectedOutcome`
  and use `// Given`, `// When`, and `// Then` blocks.

Install the SQLite CLI (`sqlite3`) on your PATH for SQL regression tests.
Run these commands from the repository root:

```sh
./gradlew build
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest --tests 'dev.sjaramillo.nemi.data.StepsCsvTest'
```

- If the SDK or signing setup blocks verification, report the exact failed
  command. Keep signing passwords and keystores private.
  See [the release guide](docs/releasing.md) for release signing.

## Device testing

- Deploy with the Android CLI. Identify the Owner with `adb shell pm list users`
  and explicitly scope installation to that user. For example, use
  `--install-options='--user 0'` after confirming Owner is user 0.
- Install into another profile only when explicitly requested. Compare app
  installation status across profiles before and after deployment, and leave
  the debug app installed for the intended user.
- Use synthetic step data for shared captures. If a before screenshot requires
  an older build and an existing install contains personal data, use an
  isolated baseline build with a temporary application ID.

## Pull requests

Follow the [PR template](.github/pull_request_template.md).
