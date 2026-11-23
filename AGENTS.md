# Pedometer

- This is a single Android module, `:app`, with application ID and namespace
  `dev.sjaramillo.pedometer`. The min, compile, and target SDK are all 37; CI
  uses JDK 25.
- Use the Kotlin DSL and dependency/plugin versions in
  `gradle/libs.versions.toml`. The app uses AndroidX, Hilt/KSP, Room, Health
  Connect, and a single Compose Foundation screen.
- Keep `MainActivity` as the Compose host. Wire Android lifecycle events,
  activity results, and privacy intents in `PedometerRoute`. Have
  `HomeViewModel` expose one `HomeUiState`, and pass state and callbacks to
  `HomeScreen`.
- Add deterministic unit tests under `app/src/test` for behavior changes.
- Room schemas under `app/schemas` are tracked artifacts. Database version 1 is
  the current reinstall-only policy; do not change the version or add migrations
  without an explicit policy change.

## Naming and unit tests

- Name types after the responsibility they own. Use a provider name when the
  contract is provider-specific and a domain name when it works across providers.
- Name new and updated unit tests using
  `given <condition> when <action> then <expected outcome>`. Use
  `// Given`, `// When`, and `// Then` blocks for setup, execution, and assertions.

## Health Connect

- Phone-only step queries must include `DataOrigin("android")` and, when
  available, the current device origin from
  `HealthConnectManager.getCurrentDeviceDataSource`; if unavailable, use only
  the legacy origin.
- On API 37, `getCurrentDeviceDataSource` is callback-based. Keep it suspendable
  via the callback; do not use reflection or assume a synchronous result.
- Refresh Health Connect on lifecycle and user events. Observe committed data
  through Room. Resolve one total per day, preferring Health Connect even
  when its total is zero. Use CSV backup totals when Health Connect has no
  total for that date and preserve `fromBackup`.

## Android device deployment

- Use the Android CLI to deploy to a connected device. Identify the main Owner
  user and explicitly scope installation to that user, for example with
  `--install-options='--user 0'` after confirming Owner is user 0.
- Install into Private Space or another profile only when explicitly requested.
  After deployment, verify the app is installed for the intended user and that
  its installation status in other profiles did not change.
- Leave the debug app installed after testing. Use synthetic step data for
  screenshots shared outside the device.

## Verification

When a PR includes UI changes, include before and after screenshots of the
affected UI in the PR description.

Run from the repository root:

```sh
./gradlew build
./gradlew ktlintCheck
./gradlew :app:testDebugUnitTest --tests 'dev.sjaramillo.pedometer.data.StepsCsvTest'
```

Ktlint exceptions are defined in `.editorconfig` for annotated constructors,
class signatures, and Compose function naming. If the SDK or signing setup
blocks a check, report the exact failed command. Do not expose signing
passwords or keystores; release signing is unfinished.

When updating Gradle, run the wrapper task twice to regenerate the complete
wrapper and synchronize `gradlew`, `gradlew.bat`, and
`gradle/wrapper/gradle-wrapper.properties`:
`./gradlew wrapper --gradle-version <version> --distribution-type bin`; do not
edit only the properties file.
