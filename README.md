Pedometer
=========

Personal Android pedometer using phone-recorded steps from Health Connect.

## Debug builds

Debug installs as `dev.sjaramillo.pedometer.debug`, alongside release
(`dev.sjaramillo.pedometer`). Both use the launcher label **Pedometer**; the debug icon adds a small **D**
behind the right foot. The existing debug signing configuration is unchanged.

Debug starts with real data. Open the toolbar overflow menu, then **Debug → Use
fake steps**, to enable sample data without Health Connect permissions or a
step-counter sensor. The selection survives relaunch. A status message on Home
identifies fake mode. Release has no debug menu or fake provider.

Fake mode refreshes the last 30 days, including today, with a repeating sequence
of 1,500, 4,500, 7,500, 10,500, and 14,000 steps indexed by epoch day. Counts for
a given date are deterministic. Home and Stats observe the shared Room cache;
CSV import/export uses that same cache.

**Fake data can overwrite real or imported debug history in that 30-day window.**
Older history remains. Disabling fake mode resumes Health Connect; sample counts
can remain cached until real data replaces them. If Health Connect is unavailable
or permission is missing, the app retains cached counts and shows its status.

## Verification

Use JDK 25 and Android SDK 37, then run from the repository root:

```sh
./gradlew build
./gradlew ktlintCheck
./gradlew :app:testDebugUnitTest --tests 'dev.sjaramillo.pedometer.data.StepsCsvTest'
```

On an emulator, check fake mode without granting Health Connect access, relaunch
persistence, mode switching, charts, Stats, and CSV import/export. Check that
release has no debug menu and installs alongside debug when a signed release is
available. These interactive checks require a device or emulator; neither was
available in the implementation environment. Release signing remains unfinished.

# Acknowledgements

This project was forked from https://github.com/j4velin/Pedometer. I asked GitHub support to detach this from the parent repo, because my intention is to continue making changes and publish it to the Play Store.
