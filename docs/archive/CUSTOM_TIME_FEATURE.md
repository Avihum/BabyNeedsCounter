# Custom Time Logging Feature

## Overview

Added the ability to log events from a different time - useful when you forget to log something in real-time and want to backdate it.

---

## How It Works

### UI Components Added:

1. **Toggle Switch**
   - Located below the Notes field
   - Label: "Log from different time"
   - Enable/disable custom time mode

2. **Time Picker** (shown when enabled)
   - **Hour picker**: +/- buttons (24-hour format)
   - **Minute picker**: +/- buttons (increments by 5 minutes)
   - **Reset button**: "Reset to now" - sets time to current time
   - Format display: `HH:MM` (e.g., "14:35")

---

## User Flow

### Normal Logging (Real-time):
1. Select event(s) (Pee, Poop, Feed)
2. Add notes (optional)
3. Click "Track"
4. Event logged with current timestamp ✅

### Custom Time Logging (Backdating):
1. Select event(s)
2. Add notes (optional)
3. **Toggle "Log from different time" ON**
4. **Adjust time** using +/- buttons
   - Hour: Cycles 0-23
   - Minutes: Increments by 5 (0, 5, 10, 15...)
5. Click "Track"
6. Event logged with custom timestamp ✅
7. Toggle automatically resets to OFF

---

## Technical Details

### State Management:
```kotlin
var useCustomTime by remember { mutableStateOf(false) }
var customHour by remember { mutableStateOf(currentHour) }
var customMinute by remember { mutableStateOf(currentMinute) }
```

### Timestamp Generation:
```kotlin
val timestamp = if (useCustomTime) {
    val customCal = Calendar.getInstance()
    customCal.set(HOUR_OF_DAY, customHour)
    customCal.set(MINUTE, customMinute)
    customCal.set(SECOND, 0)
    SimpleDateFormat("yyyy-MM-dd HH:mm").format(customCal.time)
} else {
    BackendService.getCurrentTimestamp() // Current time
}
```

### Auto-Reset:
After successful logging:
- `useCustomTime` → false
- `customHour` → current hour
- `customMinute` → current minute

---

## UI Design

```
┌─────────────────────────────────┐
│ Notes (optional)                │
│ [                             ] │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ Log from different time    [⚫] │ ← Toggle OFF
└─────────────────────────────────┘

When toggle is ON:
┌─────────────────────────────────┐
│ Log from different time    [🟢] │ ← Toggle ON
└─────────────────────────────────┘

┌─────────────────────────────────┐
│     [−]  14  [+]  :  [−]  35  [+]│ ← Time picker
│          Reset to now            │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│       Track (1)                 │ ← Track button
└─────────────────────────────────┘
```

---

## Use Cases

1. **Forgot to log during night feeding**
   - Toggle custom time
   - Set to 3:00 AM
   - Log the feed event

2. **Batch logging from memory**
   - Remember pee at 10:15
   - Set time to 10:15
   - Log event
   - Remember poop at 11:30
   - Set time to 11:30
   - Log event

3. **Correcting mistakes**
   - Realized you forgot morning poop
   - Set time to morning hour
   - Log backdated event

---

## Files Modified

- `MainActivity.kt`
  - Added state variables for custom time
  - Added UI components (Switch, Time picker buttons)
  - Modified `logEvent` function to use custom timestamp
  - Added auto-reset after logging

---

## Future Enhancements (Optional)

- Date picker (for logging from previous days)
- Time presets ("1 hour ago", "30 min ago")
- Calendar view for selecting date/time
- Show timezone in picker
- Validation (prevent future timestamps)

---

## Testing

1. Enable custom time toggle
2. Adjust time using +/- buttons
3. Log an event
4. Check Google Sheet - timestamp should match custom time
5. Toggle should auto-reset to OFF
6. Log another event (should use current time)

---

## Benefits

✅ Never lose track of events  
✅ More accurate logging  
✅ Flexibility for different workflows  
✅ Simple, intuitive UI  
✅ Auto-resets to prevent confusion
