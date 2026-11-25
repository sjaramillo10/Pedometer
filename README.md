# ![](media/nemi-icon.png) Nemi

Nemi is an Android step tracker powered by Health Connect. Set a daily goal,
see how much you walked this week, and keep your step history on your phone.

<p>
  <img src="media/home-light.png" alt="Nemi Home in light mode with synthetic step data" width="300">
  <img src="media/home-dark.png" alt="Nemi Home in dark mode with synthetic step data" width="300">
</p>

## What you can do

- Check today’s steps and your progress toward your daily goal.
- Browse past weeks and see your daily totals, weekly average, best day, and
  lifetime steps.
- Save a dated CSV backup or restore one from Settings.

## How Nemi uses your data

Connect Health Connect in Settings to let Nemi read steps recorded by your
phone. Nemi stores daily totals on your device, leaves Health Connect data
untouched, and never transmits your health data. Imported CSV totals
fill in dates when Health Connect has no total.

## Why Nemi?

The name comes from classical Nahuatl.
[*Nemi*](https://nahuatl.wired-humanities.org/content/nemi) means “to live,
exist, or dwell,” and
[*nenemi*](https://nahuatl.wired-humanities.org/content/nenemi) means “to walk
or wander.”

## Build from source

With JDK 25 and Android SDK Platform 37.2 installed, run this from the repository root:

```sh
./gradlew :app:assembleDebug
```

See [AGENTS.md](AGENTS.md) for verification and device deployment.

## Acknowledgements

Nemi began as a fork of [Pedometer](https://github.com/j4velin/Pedometer),
originally created by Thomas Hoffmann.
