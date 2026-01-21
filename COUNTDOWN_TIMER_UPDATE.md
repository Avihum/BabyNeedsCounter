# Countdown Timer Update - Next Feed Display

## Summary

Changed the feed time display from showing a static time "~13:44" to a dynamic countdown timer showing "in 2h 30m" for better at-a-glance understanding of when the next feeding is due.

## What Changed

### Before:
```
Previous Feed | Next Feed
   10:44      |  ~13:44
```

### After:
```
Previous Feed | Next Feed In
   10:44      |   2h 30m
```

## Display Logic

### Countdown Format:
- **Less than 1 hour**: Shows minutes only (e.g., "15m", "45m")
- **1+ hours**: Shows hours and minutes (e.g., "2h 30m", "1h 15m")
- **Exact hours**: Shows hours only (e.g., "2h", "3h")
- **Overdue**: Shows "Now!" when it's time to feed

### Examples:
| Time Until Feed | Display |
|----------------|---------|
| 2 hours 30 minutes | `2h 30m` |
| 1 hour 0 minutes | `1h` |
| 45 minutes | `45m` |
| 5 minutes | `5m` |
| 0 or negative | `Now!` |
| No data | `—` |

## Technical Changes

### 1. New Function in `BackendService.kt`

Added `getTimeUntilNextFeed()` function:

```kotlin
fun getTimeUntilNextFeed(): String {
    // Calculates time remaining until next feed (3 hours after last feed)
    // Returns formatted string like "2h 30m" or "45m"
    // Returns "Now!" if feeding time has passed
}
```

### 2. Updated `MainActivity.kt`

Changed feed times card:
- Label changed from "Next Feed" to "Next Feed In"
- Removed the "~" prefix
- Now calls `getTimeUntilNextFeed()` instead of `getNextFeedTime()`

### 3. Updated `BabyFeedTimesWidget.kt`

Widget now displays countdown:
- Shows time remaining until next feed
- Updates automatically with cached data
- Refreshes with live data in background

### 4. Updated `widget_feed_times.xml`

Widget layout updated:
- Changed icon from "~" to "⏱" (timer emoji) for better visual indication
- Label now implies countdown

## User Benefits

### Better UX:
1. **Instant Understanding**: "2h 30m" is clearer than "~13:44" (no mental math needed)
2. **At-a-glance**: Quick check to see if feeding is approaching
3. **Time-aware**: Shows "Now!" when feeding is due
4. **Consistent**: Same format in app and widget

### Real-world Usage:
- **Quick checks**: "Do I have time for X?" → "2h 30m, yes!"
- **Planning**: See countdown without calculating current time vs next feed time
- **Urgency**: "Now!" is more attention-grabbing than comparing times

## Technical Details

### Calculation:
```
Last Feed Time: 10:44
+ 3 hours = 13:44 (next feed time)
Current Time: 11:14
Difference: 2h 30m
Display: "2h 30m"
```

### Edge Cases Handled:
- **No last feed**: Shows "—"
- **Parse errors**: Shows "—"
- **Overdue feed**: Shows "Now!"
- **Less than 1 minute**: Shows "Now!"

### Auto-refresh:
The countdown automatically updates:
- In app: Every 15 seconds (existing periodic refresh)
- In widget: When widget updates (system schedule + cache refresh)

## Notes

- The original `getNextFeedTime()` function is still available if needed for other purposes
- Both app and widget now show consistent countdown format
- Countdown is calculated client-side, no backend changes needed
- Works with existing caching system for instant display

## Testing

To test the countdown display:

1. **In App**:
   - Log a feed event
   - See countdown start at "3h" (or close to it)
   - Watch it update every 15 seconds

2. **In Widget**:
   - Add feed times widget to home screen
   - See countdown displayed
   - Log a feed → countdown resets to ~3h

3. **Edge Cases**:
   - Wait past 3 hours → see "Now!"
   - Clear app data → see "—"
   - Go offline → see cached countdown

---

**Result**: Users can now instantly understand when the next feeding is due without having to calculate the time difference themselves. The countdown format is more intuitive and user-friendly! ⏱️
