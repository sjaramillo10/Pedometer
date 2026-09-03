# Pedometer

## Project overview

Pedometer is a Kotlin Android application in active development, intended for
eventual publication on the Play Store. The main module is `app`. The project
uses Kotlin DSL Gradle scripts, the Gradle version catalog, AndroidX, Jetpack
Compose, Hilt with KSP, and Room.

The package and application ID are `dev.sjaramillo.pedometer`. The app currently
has a minimum, compile, and target SDK of API 37. CI runs on JDK 25.

## Repository conventions

- Use the Gradle version catalog in `gradle/libs.versions.toml` for dependency,
  plugin, and tool versions. Avoid scattering versions through build scripts.
- Follow the existing Kotlin formatting and ktlint configuration. Keep changes
  idiomatic and focused; do not reformat unrelated files.
- Keep UI state and lifecycle-sensitive work in the existing ViewModel and
  repository layers rather than adding global state.
- Treat Room schema files under `app/schemas` as versioned project artifacts.
  The app intentionally keeps its destructive, reinstall-only schema at version
  1; do not add migrations or retain legacy data unless that policy changes.
- Do not commit keystores, passwords, API keys, or other signing credentials.
  Release signing configuration is intentionally unfinished.
- Add or update unit and instrumentation tests with behavior changes. Prefer
  deterministic tests.

## Health Connect

- Phone steps captured by Health Connect can use a device-specific synthetic
  package name (SPN), rather than the historical `android` data origin. Queries
  for phone-only steps must include both `DataOrigin("android")` and the current
  device SPN returned by `HealthConnectManager.getCurrentDeviceDataSource(...)`.
- On API 37, `getCurrentDeviceDataSource` is callback-based. Do not use
  reflection or assume it returns a result synchronously.

## Verification

Run these checks from the repository root before submitting a change:

```sh
./gradlew build
./gradlew ktlintCheck
```

For focused work, run the narrowest relevant module or test task as well. If
the Android SDK or signing setup prevents a full build, report that clearly and
include the checks that did run.

## Android CLI

The following commands assume the `android` CLI is installed and configured
with the Android SDK.

- Use `android info` to inspect the SDK configuration, and `android sdk list`
  or `android sdk install <package>` to manage SDK packages. The API 37 platform
  package is `platforms/android-37.0`.
- Use `android run` to build, deploy, and launch the app. Use `android install
  --device=<serial> --apks=<apk-path> --install-options=-r` to install an
  already-built APK while retaining compatible app data.
- Use `android screen capture --device=<serial> --output=<path>` to capture a
  device screenshot for visual verification.
- For Wi-Fi debugging, use ADB to discover and connect before passing its serial
  to the Android CLI: `adb mdns services`, then `adb connect <host:port>`.

## Updating the Gradle version

Gradle updates must regenerate the complete wrapper. Do not only edit
`gradle/wrapper/gradle-wrapper.properties`.

1. Check compatibility between the proposed Gradle version and the Android
   Gradle Plugin, Kotlin, KSP, Compose, and the JDK used by CI. Update related
   tool versions only when required by compatibility.
2. From the repository root, run the wrapper task with the intended version:

   ```sh
   ./gradlew wrapper --gradle-version <version> --distribution-type bin
   ```

   If the current wrapper cannot bootstrap the update, use a matching locally
   installed Gradle or set a temporary `GRADLE_USER_HOME`; do not check in a
   hand-edited or partially regenerated wrapper.
3. Review and commit all wrapper outputs together:
   `gradle/wrapper/gradle-wrapper.jar`,
   `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, and `gradlew.bat`.
4. Verify the selected version and project configuration:

   ```sh
   ./gradlew --version
   ./gradlew help
   ./gradlew build
   ./gradlew ktlintCheck
   ```

5. Review the diff for unexpected dependency, script, or line-ending changes.
   Keep the Gradle update separate from unrelated source or formatting changes
   when practical.
