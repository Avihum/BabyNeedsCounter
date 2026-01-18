# 📂 Project Structure Reference

Quick reference guide to the Baby Needs Counter project organization.

## 🗂️ Directory Layout

```
BabyNeedsCounter/
│
├── 📋 .github/                      # GitHub-specific files
│   ├── ISSUE_TEMPLATE/              # Issue templates
│   │   ├── bug_report.md           # Bug report template
│   │   └── feature_request.md      # Feature request template
│   └── PULL_REQUEST_TEMPLATE.md    # PR template
│
├── 📱 app/                          # Android application
│   └── src/
│       ├── main/                    # Main app code
│       │   ├── java/               # Kotlin source files
│       │   ├── res/                # Resources (layouts, drawables, etc.)
│       │   └── AndroidManifest.xml # App manifest
│       ├── test/                    # Unit tests
│       └── androidTest/             # Instrumentation tests
│
├── 🔧 backend/                      # Backend & deployment scripts
│   ├── GoogleAppsScript.js         # Main Google Apps Script
│   ├── GoogleAppsScript_ALTERNATIVE.js # Alternative implementation
│   ├── appsscript.json             # Apps Script config
│   ├── .clasp.json                 # Clasp CLI config
│   ├── .claspignore                # Clasp ignore rules
│   ├── deploy-script.sh            # Deployment automation
│   └── README.md                   # Backend documentation
│
├── 📚 docs/                         # All documentation
│   ├── 📦 archive/                 # Historical feature docs
│   │   ├── WIDGET_SPLIT_UPDATE.md
│   │   ├── CUSTOM_TIME_FEATURE.md
│   │   └── (+ 13 more archived docs)
│   ├── 🖼️ screenshots/             # App screenshots
│   │   └── .gitkeep
│   ├── BACKEND_SETUP.md            # Setup guide
│   ├── CHANGELOG.md                # Version history
│   ├── DEPLOYMENT_INSTRUCTIONS.md  # Deployment guide
│   ├── QUICK_START.md              # 5-min quick start
│   ├── SCREENSHOTS.md              # Screenshot guide
│   ├── TROUBLESHOOTING.md          # Common issues
│   └── README.md                   # Docs index
│
├── 🔨 gradle/                       # Gradle wrapper files
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── ⚙️ Configuration Files (root)
│   ├── .gitattributes              # Git file handling
│   ├── .gitignore                  # Git ignore rules
│   ├── build.gradle.kts            # Project build config
│   ├── gradle.properties           # Gradle properties
│   ├── gradlew                     # Gradle wrapper (Unix)
│   ├── gradlew.bat                 # Gradle wrapper (Windows)
│   └── settings.gradle.kts         # Project settings
│
├── 📄 Documentation (root)
│   ├── LICENSE                     # MIT License
│   ├── README.md                   # Main README
│   └── PROJECT_STRUCTURE.md        # This file
│
└── 🚫 Ignored Folders (not in git)
    ├── .gradle/                    # Gradle cache
    ├── .idea/                      # IDE settings
    ├── build/                      # Build outputs
    └── */build/                    # Module build folders
```

## 🎯 Quick Navigation

### For Users
- **Getting Started**: [`docs/QUICK_START.md`](docs/QUICK_START.md)
- **Setup Backend**: [`docs/BACKEND_SETUP.md`](docs/BACKEND_SETUP.md)
- **Help & Support**: [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md)

### For Developers
- **Main README**: [`README.md`](README.md)
- **Architecture**: See README → Architecture section
- **Backend API**: [`backend/README.md`](backend/README.md)
- **Contributing**: See README → Contributing section

### For Contributors
- **Documentation Index**: [`docs/README.md`](docs/README.md)
- **Changelog**: [`docs/CHANGELOG.md`](docs/CHANGELOG.md)
- **Screenshot Guide**: [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md)
- **Feature History**: [`docs/archive/`](docs/archive/)

## 📦 Key Components

### Android App (`app/`)
- **Activities**: `MainActivity.kt`
- **Services**: `BackendService.kt`
- **Widgets**: `BabyLoggingWidget.kt`, `BabyStatsWidget.kt`
- **Settings**: `SettingsManager.kt`, `SettingsScreen.kt`
- **Theme**: `ui/theme/`

### Backend (`backend/`)
- **Main Script**: `GoogleAppsScript.js` - Handles POST/GET requests
- **Deployment**: `deploy-script.sh` - Automated deployment via Clasp
- **Config**: `appsscript.json` - Apps Script configuration

### Documentation (`docs/`)
- **User Guides**: Setup, quick start, troubleshooting
- **Developer Docs**: Changelog, architecture
- **Assets**: Screenshots folder
- **Archive**: Historical feature documentation

## 🔍 Finding Files

### "Where is...?"

| Looking for | Location |
|------------|----------|
| Main app code | `app/src/main/java/com/example/babyneedscounter/` |
| Widget layouts | `app/src/main/res/layout/widget_*.xml` |
| App theme colors | `app/src/main/java/com/example/babyneedscounter/ui/theme/Color.kt` |
| Backend script | `backend/GoogleAppsScript.js` |
| Setup instructions | `docs/BACKEND_SETUP.md` |
| Version history | `docs/CHANGELOG.md` |
| Bug reports | `.github/ISSUE_TEMPLATE/bug_report.md` |
| Feature updates | `docs/archive/` |
| Screenshots | `docs/screenshots/` |

## 🎨 File Naming Conventions

- **Kotlin files**: PascalCase (`MainActivity.kt`)
- **Resource files**: snake_case (`widget_baby_logging.xml`)
- **Documentation**: SCREAMING_SNAKE_CASE (`.md` files)
- **Folders**: lowercase (`backend/`, `docs/`)

## 📊 File Organization Principles

1. **Separation of Concerns**: App code, backend, docs are separated
2. **Documentation Co-location**: Related docs live together in `docs/`
3. **Backend Isolation**: All backend code in `backend/`
4. **Clean Root**: Minimal files at root level (8 essential files only)
5. **Archived History**: Old docs preserved in `docs/archive/`

## 🚀 Common Tasks

### Adding a Screenshot
```bash
# Add image to docs/screenshots/
cp ~/screenshot.png docs/screenshots/home.png

# Update README.md to reference it
![Home Screen](docs/screenshots/home.png)
```

### Updating Backend
```bash
# Edit script
vim backend/GoogleAppsScript.js

# Deploy (if using Clasp)
cd backend && ./deploy-script.sh
```

### Adding Documentation
```bash
# Create new doc in docs/
touch docs/NEW_GUIDE.md

# Add link in docs/README.md
```

### Archiving Feature Docs
```bash
# Move old feature doc to archive
mv SOME_OLD_FEATURE.md docs/archive/

# Update CHANGELOG.md instead
```

## 💡 Best Practices

- ✅ **Keep root clean** - Only essential config files
- ✅ **Document in `docs/`** - All markdown files except root README
- ✅ **Backend in `backend/`** - All Google Apps Script code
- ✅ **Archive old docs** - Move to `docs/archive/`, update CHANGELOG
- ✅ **Use READMEs** - Add README.md to new folders for context
- ✅ **Reference properly** - Use relative paths in markdown links

---

**Last Updated**: January 2026  
**Maintained by**: Project contributors
