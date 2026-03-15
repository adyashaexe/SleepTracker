# 🌙 SleepTracker

A minimalist Android sleep tracking app built with Kotlin that helps you monitor your sleep duration, track sleep stages, and meet your nightly sleep goals.

## 📱 Screenshots

_Coming soon_

## ✨ Features

- **Sleep Timer** — Start and stop a chronometer to track exactly how long you sleep
- **Sleep Stage Detection** — Automatically estimates your current sleep stage based on elapsed time:
  - Awake → Falling Asleep → Light Sleep (N1/N2) → Deep Sleep (N3) → REM Sleep
  - Cycles through realistic 90-minute sleep cycles
- **Sleep Goal** — Set a nightly sleep goal (default: 8 hours) and get notified whether you met it
- **Session History** — Every session is saved locally and your all-time average sleep is calculated
- **Sleep Summary** — After each session, a summary dialog shows your session duration and all-time average
- **Dark UI** — Deep navy dark theme designed for nighttime use

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Platform**: Android (minimum SDK 21)
- **Storage**: SharedPreferences with JSON serialization
- **UI**: XML layouts with custom drawables
- **Architecture**: Single Activity

## 🚀 Getting Started

### Prerequisites
- Android Studio
- Android device or emulator running Android 5.0+

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/adyashaexe/SleepTracker.git
   ```
2. Open the project in Android Studio
3. Let Gradle sync
4. Run on your device or emulator

## 📖 How to Use

1. Tap **Start** when you go to bed
2. The app tracks elapsed time and updates your sleep stage automatically
3. Tap **Stop** when you wake up — a summary will appear
4. Tap **Set Goal** to configure your target sleep duration
5. Tap **Reset** to clear the current session

## 📁 Project Structure

```
app/src/main/
├── java/com/example/sleeptracker/
│   └── MainActivity.kt
└── res/
    ├── layout/
    │   └── activity_main.xml
    ├── drawable/
    │   ├── card_bg.xml
    │   └── pill_bg.xml
    └── values/
        └── themes.xml
```

## 🐛 Known Issues

- Sleep stage estimation is based on elapsed time only, not actual biometric data
- Sessions are not editable after saving

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
