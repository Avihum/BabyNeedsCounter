# 👶 Baby Needs Counter

<div align="center">

**A modern Android app for tracking your baby's daily activities with real-time sync to Google Sheets**

[![Android](https://img.shields.io/badge/Android-35+-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Tech Stack](#-tech-stack) • [Architecture](#-architecture) • [Contributing](#-contributing)

</div>

---

## 📱 Overview

Baby Needs Counter is a beautiful, intuitive Android app designed for busy parents and caregivers. Track diaper changes, feedings, and other baby activities with just a tap. All data syncs automatically to Google Sheets, making it easy to share information between multiple caregivers in real-time.

### Why Baby Needs Counter?

- **⚡ Quick Logging**: Multi-select interface lets you log multiple activities at once
- **🔄 Real-time Sync**: Automatic synchronization with Google Sheets
- **🎯 Widget Support**: Log activities directly from your home screen without opening the app
- **⏰ Custom Time Entry**: Log activities that happened earlier with the built-in time picker
- **📊 Live Stats**: See today's activity counts and time since last feeding at a glance
- **👨‍👩‍👧‍👦 Multi-caregiver**: Share data seamlessly across family members and caregivers

---

## ✨ Features

### Core Functionality

- **📝 Activity Tracking**
  - Diaper changes (poop & pee, pee only)
  - Feeding/breastmilk
  - Optional notes for each entry
  - Custom timestamp selection for retroactive logging

- **📊 Real-time Statistics**
  - Today's pee count
  - Today's poop count
  - Time since last feeding
  - Auto-refresh every 15 seconds

- **🏠 Home Screen Widgets**
  - **Logging Widget**: Quick-access buttons for logging activities
  - **Stats Widget**: View today's stats without opening the app
  - Automatic updates after each entry

- **☁️ Cloud Sync**
  - Automatic sync to Google Sheets
  - Real-time data sharing between devices
  - View full history in Google Sheets
  - Easy data export and analysis

- **🎨 Beautiful UI**
  - Modern Material Design 3
  - Dark mode support
  - Smooth animations and transitions
  - Intuitive, parent-friendly interface

### Advanced Features

- **Custom Time Picker**: Vertical scroll wheel for easy time selection
- **Multi-select Tracking**: Log multiple activities simultaneously
- **Lifecycle-aware Updates**: Widgets refresh when app opens/closes
- **Emoji-based Types**: Visual identification of activity types (💩💧🐄)
- **Optimized Performance**: Efficient background updates and minimal battery usage

---

## 📸 Screenshots

> **Note**: Add screenshots here to showcase your app!

<div align="center">

| Home Screen | Widgets | Settings |
|------------|---------|----------|
| ![Home](docs/screenshots/home.png) | ![Widgets](docs/screenshots/widgets.png) | ![Settings](docs/screenshots/settings.png) |

*Add screenshots by creating a `docs/screenshots/` folder and placing your images there*

</div>

### 📹 Demo

> **Note**: Add a GIF or video demo here!

<div align="center">

![Demo](docs/demo.gif)

*Record a demo using Android Studio or a screen recorder, convert to GIF using [ezgif.com](https://ezgif.com)*

</div>

---

## 🚀 Installation

### Prerequisites

- Android Studio Ladybug or newer
- Android device or emulator running Android 14 (API 35) or higher
- JDK 11 or higher
- Google account for backend setup

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/BabyNeedsCounter.git
   cd BabyNeedsCounter
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio's Run button (▶️)

4. **Set Up Backend** (Required for sync functionality)
   - Follow the detailed guide in [docs/BACKEND_SETUP.md](docs/BACKEND_SETUP.md)
   - Create a Google Sheet
   - Deploy the Apps Script from `backend/GoogleAppsScript.js`
   - Add the Web App URL in app settings

---

## 🏗️ Architecture

### Project Structure

```
BabyNeedsCounter/
├── .github/                    # GitHub issue and PR templates
├── app/                        # Android app source code
│   └── src/main/java/com/example/babyneedscounter/
│       ├── MainActivity.kt              # Main activity with navigation
│       ├── BackendService.kt           # Google Sheets API integration
│       ├── SettingsManager.kt          # Persistent settings storage
│       ├── SettingsScreen.kt           # Settings UI
│       ├── BabyLoggingWidget.kt        # Quick-logging widget
│       ├── BabyStatsWidget.kt          # Statistics widget
│       ├── BabyNeedsWidget.kt          # Legacy combined widget
│       └── ui/theme/                   # Material Design 3 theming
├── backend/                    # Google Apps Script backend
│   ├── GoogleAppsScript.js            # Main backend script
│   ├── deploy-script.sh               # Deployment automation
│   └── README.md                      # Backend documentation
├── docs/                       # All documentation
│   ├── archive/                       # Historical feature updates
│   ├── screenshots/                   # App screenshots
│   ├── BACKEND_SETUP.md               # Setup guide
│   ├── CHANGELOG.md                   # Version history
│   ├── QUICK_START.md                 # Quick start guide
│   └── README.md                      # Documentation index
├── gradle/                     # Gradle wrapper
├── LICENSE                     # MIT License
├── README.md                   # This file
└── build.gradle.kts            # Project build configuration
```

### Design Patterns

- **MVVM Architecture**: Separation of concerns with ViewModels (implicit in Compose)
- **Repository Pattern**: BackendService acts as data layer
- **Dependency Injection**: Manual injection via constructor parameters
- **Reactive UI**: Jetpack Compose with Flow/StateFlow for reactive updates
- **Single Activity Architecture**: Navigation Compose for screen management

### Data Flow

```
User Action → UI (Compose) → BackendService → Google Apps Script → Google Sheets
                                     ↓
                            SettingsManager (DataStore)
                                     ↓
                              Widget Updates
```

---

## 🛠️ Tech Stack

### Android

- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Repository Pattern
- **Navigation**: Navigation Compose
- **Storage**: DataStore Preferences
- **Networking**: OkHttp
- **Widgets**: AppWidget API (RemoteViews)
- **Build System**: Gradle (KTS)

### Backend

- **Platform**: Google Apps Script
- **Database**: Google Sheets
- **API**: REST (doPost/doGet endpoints)
- **Deployment**: Google Apps Script Web Apps

### Dependencies

```kotlin
// Core
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-ktx

// UI
androidx.compose.ui
androidx.compose.material3
androidx.compose.material.icons.extended
androidx.activity:activity-compose
androidx.navigation:navigation-compose

// Storage
androidx.datastore:datastore-preferences

// Networking
com.squareup.okhttp3:okhttp

// Testing
junit:junit
androidx.test.ext:junit
androidx.compose.ui:ui-test-junit4
```

---

## 📚 Documentation

- **[Quick Start Guide](docs/QUICK_START.md)** - Get up and running in 5 minutes
- **[Backend Setup Guide](docs/BACKEND_SETUP.md)** - Step-by-step Google Sheets integration
- **[Backend Documentation](backend/README.md)** - Google Apps Script API and deployment
- **[Changelog](docs/CHANGELOG.md)** - Complete version history and features
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions
- **[Documentation Index](docs/README.md)** - Full documentation navigation

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### Getting Started

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused
- Write tests for new features

### Commit Guidelines

- Use clear, descriptive commit messages
- Start with a verb (Add, Fix, Update, Remove)
- Reference issues when applicable

**Example:**
```
Add custom time picker for retroactive logging

- Implement vertical scroll wheel component
- Add time selection state management
- Update backend to handle custom timestamps

Fixes #123
```

### Areas for Contribution

- 🐛 Bug fixes
- ✨ New features (sleep tracking, medicine reminders, etc.)
- 📝 Documentation improvements
- 🎨 UI/UX enhancements
- 🧪 Test coverage
- 🌍 Translations/Localization
- ♿ Accessibility improvements

---

## 🗺️ Roadmap

### Planned Features

- [ ] Sleep tracking
- [ ] Medicine/supplement logging
- [ ] Photo attachments
- [ ] Multiple baby profiles
- [ ] Data analytics and insights
- [ ] Export to PDF reports
- [ ] Offline mode with sync queue
- [ ] Push notifications for reminders
- [ ] Dark/Light theme toggle in settings
- [ ] Localization (multiple languages)

### Under Consideration

- Integration with health apps
- Voice logging via Google Assistant
- Smart watch companion app
- AI-powered pattern detection
- Collaborative notes between caregivers

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Avihu Marco**

- GitHub: [@Avihum](https://github.com/Avihum)
- Project Link: [https://github.com/Avihum/BabyNeedsCounter](https://github.com/Avihum/BabyNeedsCounter)

---

## 🙏 Acknowledgments

- Thanks to all parents and caregivers who inspired this project
- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Icons from [Material Design Icons](https://fonts.google.com/icons)
- Inspired by modern baby tracking apps with a focus on simplicity

---

## 📞 Support

If you find this project helpful, please consider:

- ⭐ Starring the repository
- 🐛 Reporting bugs via [Issues](https://github.com/Avihum/BabyNeedsCounter/issues)
- 💡 Suggesting features via [Discussions](https://github.com/Avihum/BabyNeedsCounter/discussions)
- 📢 Sharing with other parents

---

<div align="center">

**Made with ❤️ for busy parents everywhere**

</div>
