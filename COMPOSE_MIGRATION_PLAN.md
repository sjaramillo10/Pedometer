# Compose Migration Plan

This migration converts the application screen by screen. Each screen is
implemented in a separate pull request so that behavior changes remain small,
reviewable, and easy to validate. Existing Fragment and navigation wrappers
remain until every user-facing screen has been migrated.

## Pull Requests

### 1. Stats screen

- Keep `StatsFragment` as the temporary Compose host.
- Keep statistics state in `StatsViewModel`.
- Preserve loading, empty-data, date-range, total, average, and record behavior.
- Add or retain deterministic state tests.

### 2. Home screen

- Add a lifecycle-aware `HomeViewModel` for steps, history, preferences, and
  Health Connect state.
- Replace the EazeGraph pie chart with a Compose progress visualization.
- Replace the EazeGraph bar chart with a Compose history visualization.
- Preserve steps/distance switching and Health Connect permission states.
- Remove EazeGraph after the Compose Home screen is complete.

### 3. Settings screen

- Replace `PreferenceFragmentCompat` with a Compose settings screen.
- Preserve SharedPreferences keys, defaults, goal editing, step-size editing,
  CSV import/export, and Health Connect refresh after import.
- Use Compose dialogs for goal and step-size editing.

### 4. Health Connect privacy screen

- Render the privacy policy with Compose.
- Preserve the Health Connect permission-usage activity alias and manifest
  contract.

### 5. Compose app shell

- Replace the XML activity layout with a Compose shell.
- Replace the XML toolbar, bottom navigation, fragment container, and navigation
  graph with Compose UI and screen selection.
- Preserve titles, back behavior, About, system-bar insets, and Health Connect
  permission launching.

### 6. Remove Fragment infrastructure

- Delete the migrated Fragment classes, screen layouts, and navigation graph.
- Remove Fragment and Navigation dependencies once no longer referenced.
- Keep the required Health Connect privacy activity.

## Dependency Cleanup Order

The UI migration intentionally retains Room, Hilt, and lifecycle components.
They should be removed in separate, focused changes after the screen migration:

1. Migrate Stats.
2. Migrate Home and remove EazeGraph.
3. Migrate Settings.
4. Migrate the privacy screen.
5. Replace the Activity shell.
6. Remove Fragment and Navigation infrastructure.
7. Remove Hilt with manual dependency construction.
8. Reassess lifecycle and ViewModel dependencies.
9. Replace Room with a smaller SQLite implementation if the measured APK size
   justifies the storage rewrite.

## Validation

Every pull request must include:

- Unit tests for new state and behavior.
- Relevant Compose UI tests for important visible states.
- `./gradlew build` and `./gradlew ktlintCheck` results.
- Release APK size and `apkanalyzer` download-size measurements.
- Manual validation by installing and running the app on the physical Pixel 9
  through the Android CLI.
