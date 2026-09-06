# Pedometer

- This is a single Android module, `:app`, with application ID and namespace
  `dev.sjaramillo.pedometer`. The min, compile, and target SDK are all 37; CI
  uses JDK 25.
- Use the Kotlin DSL and dependency/plugin versions in
  `gradle/libs.versions.toml`. The app uses AndroidX, Hilt/KSP, Room, Health
  Connect, and a hybrid XML Fragment plus Compose UI.
- Keep lifecycle-sensitive UI state in the existing ViewModels and repository
  layer. Add deterministic unit tests under `app/src/test` for behavior changes.
- Room schemas under `app/schemas` are tracked artifacts. Database version 1 is
  the current reinstall-only policy; do not change the version or add migrations
  without an explicit policy change.

## Health Connect

- Phone-only step queries must include `DataOrigin("android")` and, when
  available, the current device origin from
  `HealthConnectManager.getCurrentDeviceDataSource`; if unavailable, use only
  the legacy origin.
- On API 37, `getCurrentDeviceDataSource` is callback-based. Keep it suspendable
  via the callback; do not use reflection or assume a synchronous result.

## Verification

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
