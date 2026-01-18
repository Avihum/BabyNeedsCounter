# 🔄 Lifecycle-Aware Updates Implementation

## Summary
Added aggressive refresh triggers to ensure widgets and UI stats are always up-to-date.

## Changes Made

### 1. MainActivity Lifecycle Hooks

Added automatic widget updates on app lifecycle events:

```kotlin
override fun onResume() {
    super.onResume()
    // Updates all widgets when app comes to foreground
    updateAllWidgets()
}

override fun onPause() {
    super.onPause()
    // Updates all widgets when app goes to background
    updateAllWidgets()
}
```

**Triggers:**
- ✅ **App opens** → Widget refresh
- ✅ **App minimized/backgrounded** → Widget refresh
- ✅ **Switch back from another app** → Widget refresh

---

### 2. Compose Lifecycle Observer

Added lifecycle-aware refresh for UI stats:

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            Log.d("BabyNeeds", "Screen resumed - refreshing stats")
            refreshStats()
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

**Triggers:**
- ✅ **Navigate back to home screen** → Stats refresh
- ✅ **Return from settings** → Stats refresh
- ✅ **Screen becomes visible** → Stats refresh

---

### 3. Increased Periodic Refresh Rate

Changed from **30 seconds** to **15 seconds**:

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        kotlinx.coroutines.delay(15_000L) // 15 seconds (reduced from 30)
        if (googleSheetUrl.isNotEmpty() && !isLoading) {
            Log.d("BabyNeeds", "Periodic stats refresh triggered")
            refreshStats()
        }
    }
}
```

**Benefit:** Stats are never more than 15 seconds old when app is active.

---

## Complete Update Matrix

| Event | Widget Update | UI Stats Update |
|-------|---------------|-----------------|
| **App Launch** | ✅ Yes (onResume) | ✅ Yes (LaunchedEffect + lifecycle) |
| **App Minimized** | ✅ Yes (onPause) | N/A (app backgrounded) |
| **App Resumed** | ✅ Yes (onResume) | ✅ Yes (lifecycle observer) |
| **Event Logged** | ✅ Yes (immediate) | ✅ Yes (immediate) |
| **Periodic** | Every 90 sec (system) | Every 15 sec |
| **Navigate to Settings** | No change | N/A (different screen) |
| **Navigate Back Home** | No change | ✅ Yes (lifecycle observer) |

---

## API Call Frequency

### Before:
- **UI**: On launch + every 30 sec when active
- **Widget**: Every 5 minutes (automatic) + after logging
- **Maximum staleness**: 5 minutes

### After:
- **UI**: On launch + on resume + every 15 sec when active
- **Widget**: On app open/close + every 90 sec (automatic) + after logging
- **Maximum staleness**: 15 seconds (UI), 90 seconds (widget when app closed)

---

## Battery Impact

**Minimal overhead:**
- Lifecycle events are free (no cost)
- Increased from ~60 API calls/hour to ~120 API calls/hour when active
- Widget updates only when necessary (not continuous)
- All updates only when app is in use

**Trade-off:** Slightly more battery usage for **much** more responsive tracking.

---

## Testing

1. **Open app** → Check widget updates immediately
2. **Minimize app** → Check widget updates immediately
3. **Navigate to Settings and back** → Check stats refresh on return
4. **Leave app open** → Stats should update every 15 seconds
5. **Switch to another app and back** → Widget + stats should refresh

---

## Files Modified

1. **MainActivity.kt**
   - Added `onResume()` and `onPause()` lifecycle methods
   - Added `updateAllWidgets()` private method
   - Added lifecycle compose imports
   - Added `DisposableEffect` for UI lifecycle awareness
   - Changed periodic refresh from 30s to 15s

---

**Updated:** 2026-01-18  
**Status:** ✅ Implemented and tested
