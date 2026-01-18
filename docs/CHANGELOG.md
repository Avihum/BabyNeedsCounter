# Changelog

All notable changes to the Baby Needs Counter project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Sleep tracking
- Medicine/supplement logging
- Multiple baby profiles
- Offline mode with sync queue

---

## [1.0.0] - 2026-01-18

### Added - Widget Split Update
- **Separated widgets into two focused widgets:**
  - **Baby Logging Widget**: Quick-access buttons for logging activities
  - **Baby Stats Widget**: Compact view of today's statistics
- Both widgets update automatically after app interactions
- Improved widget performance and reliability

### Added - Custom Time Feature
- **Custom time picker** for logging past events
- Vertical scroll wheel interface for hour and minute selection
- "Log from different time" toggle switch
- "Reset to now" button for convenience
- Proper timestamp handling in backend

### Added - Vertical Scroll Picker
- Smooth scrolling time picker with snap-to-center behavior
- Visual highlighting of selected values
- Optimized scroll detection to prevent UI jank
- LazyColumn-based implementation for better performance

### Added - Compact Widget Updates
- Streamlined widget layouts for better space efficiency
- Enhanced update lifecycle management
- Widget refreshes on app resume/pause events
- Proper broadcast handling for immediate updates

### Added - Time Picker and Sorting
- Advanced time selection with custom timestamp support
- Backend sorting to maintain chronological order
- Duplicate detection and merging logic
- ISO 8601 timestamp format support

### Added - Lifecycle Updates
- Automatic widget refresh when app goes to foreground/background
- Periodic 15-second refresh while app is active
- Lifecycle-aware stat fetching
- DisposableEffect for proper observer cleanup

### Added - Refresh Optimization
- Reduced periodic refresh interval from 30s to 15s
- Smart refresh triggers on screen resume
- Immediate widget updates after successful logging
- Efficient broadcast-based widget update mechanism

---

## [0.3.0] - Initial Backend Integration

### Added - Backend Sync
- Google Apps Script integration
- Real-time sync to Google Sheets
- BackendService with OkHttp client
- SettingsManager with DataStore
- Settings screen with web app URL configuration

### Added - Multi-Select Tracking
- Multiple activity selection (poop & pee, pee only, feeding)
- Combined event logging
- Emoji-based type system (💩💧🐄)
- Optional notes field

### Added - UI Improvements
- Quick stats cards (pee count, poop count, time since feeding)
- Selectable action cards with visual feedback
- Material Design 3 theming
- Smooth loading states and animations

---

## [0.2.0] - Widget Support

### Added
- Initial widget implementation
- Home screen widget with quick-access buttons
- RemoteViews-based widget layouts
- Widget button click handling

---

## [0.1.0] - Initial Release

### Added
- Basic activity tracking (diaper changes, feeding)
- Simple counting interface
- Material Design 3 UI
- Jetpack Compose implementation
- Dark mode support

---

## Feature History (Consolidated)

### Emoji Types Update
- Introduced emoji-based event types for visual clarity
- Backend support for mixed emoji strings (💩💧, 💧, 🐄)
- Improved type parsing and validation

### Widget Combo Update
- Combined stats and logging in single widget view
- Optimized widget refresh logic
- Enhanced error handling

### Recent Updates Summary
- Comprehensive custom time logging
- Widget architecture improvements
- Real-time statistics display
- Multi-caregiver sync support
- Enhanced UI/UX across all screens

---

## Migration Notes

### From 0.2.0 to 1.0.0
- Widget configuration: Users with existing widgets may need to re-add them due to widget split
- Old `BabyNeedsWidget` is now legacy; use `BabyLoggingWidget` and `BabyStatsWidget`
- No data migration needed; Google Sheets backend remains compatible

---

## Technical Improvements

### Performance
- Reduced unnecessary widget updates
- Optimized coroutine usage
- Efficient state management
- Smart refresh strategies

### Code Quality
- Better separation of concerns
- Improved error handling
- Enhanced logging for debugging
- Cleaner widget lifecycle management

### Architecture
- Navigation Compose implementation
- Repository pattern for backend
- Reactive UI with Flow/StateFlow
- MVVM architecture principles

---

**For detailed technical documentation of each feature, see the individual update files in the repository root.**
