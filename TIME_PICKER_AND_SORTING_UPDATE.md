# Time Picker & Chronological Sorting Update

## Changes Made

### 1. ✅ Replaced +/- Buttons with Native Time Picker (Scroll Wheels)

**Before:**
```
[−] 14 [+]  :  [−] 35 [+]
```

**After:**
```
Material 3 TimePicker with scroll wheels
    ┌─────────────┐
    │   14 : 35   │  ← Interactive scroll wheels
    │  ⚫  ⚫  ⚫   │
    └─────────────┘
```

**Benefits:**
- Native Android Material 3 time picker
- Better UX with scroll wheels
- Familiar interface
- Easier to use
- Supports 24-hour format

**Implementation:**
```kotlin
val timePickerState = rememberTimePickerState(
    initialHour = customHour,
    initialMinute = customMinute,
    is24Hour = true
)

TimePicker(state = timePickerState)
```

---

### 2. ✅ Chronological Sorting in Google Sheets

**Before:**
- Always inserted at row 2 (LIFO - Last In First Out)
- Backdated events appeared at top (wrong order)
- Sheet not sorted by time

**After:**
- Finds correct position based on timestamp
- Inserts event in chronological order (newest first)
- Backdated events go to correct position
- Sheet always sorted by timestamp

**Example:**

| Timestamp | Type | Notes |
|-----------|------|-------|
| 2026-01-18 14:30 | 💧 | Most recent |
| 2026-01-18 12:00 | 🐄 | Older |
| 2026-01-18 10:00 | 💩💧 | Even older |

If you log an event at 13:00, it will be inserted between 14:30 and 12:00:

| Timestamp | Type | Notes |
|-----------|------|-------|
| 2026-01-18 14:30 | 💧 | Most recent |
| **2026-01-18 13:00** | **🐄** | **← Inserted here** |
| 2026-01-18 12:00 | 🐄 | Older |
| 2026-01-18 10:00 | 💩💧 | Even older |

---

## Technical Details

### Android App Changes:
**File:** `MainActivity.kt`

**Added:**
```kotlin
// Material 3 Time Picker State
val timePickerState = rememberTimePickerState(
    initialHour = customHour,
    initialMinute = customMinute,
    is24Hour = true
)

// Sync state when picker changes
LaunchedEffect(timePickerState.hour, timePickerState.minute) {
    customHour = timePickerState.hour
    customMinute = timePickerState.minute
}

// Render the time picker
TimePicker(
    state = timePickerState,
    modifier = Modifier.padding(horizontal = 16.dp)
)
```

---

### Google Apps Script Changes:
**File:** `GoogleAppsScript.js`

**Algorithm:**
1. Parse incoming timestamp
2. Get all existing rows
3. Loop through rows (newest first)
4. Compare timestamps
5. Find correct insertion position
6. Insert row at that position
7. Maintain chronological order

**Key Logic:**
```javascript
for (let row = 2; row <= lastRow; row++) {
  const existingTime = new Date(existingTimestamp);
  
  // If new event is newer, insert before it
  if (newEventTime > existingTime) {
    insertPosition = row;
    break;
  }
}

// Insert at correct position
sheet.insertRowBefore(insertPosition);
sheet.getRange(insertPosition, 1, 1, 3).setValues([...]);
```

---

## Use Cases

### Before (Broken):
1. Log pee at 14:00 → Row 2
2. Log feed at 10:00 (backdated) → Row 2 (WRONG!)
3. Sheet order: 10:00, 14:00 ❌

### After (Fixed):
1. Log pee at 14:00 → Row 2
2. Log feed at 10:00 (backdated) → Row 3 (CORRECT!)
3. Sheet order: 14:00, 10:00 ✅

---

## User Flow

1. Enable "Log from different time"
2. Material 3 time picker appears
3. **Scroll wheels** to select time (much easier!)
4. Click "Track"
5. Event inserted in **correct chronological position**
6. Sheet maintains proper order

---

## Testing

1. **Test 1: Normal logging**
   - Log current event
   - Should appear at top (row 2)

2. **Test 2: Backdated event**
   - Log event from 2 hours ago
   - Should appear below more recent events

3. **Test 3: Future event** (edge case)
   - Log event from 1 hour in future
   - Should appear at top (most recent)

4. **Test 4: Multiple backdated events**
   - Log 10:00, then 12:00, then 11:00
   - Final order: 12:00, 11:00, 10:00

---

## Files Modified

1. **MainActivity.kt**
   - Replaced +/- buttons with TimePicker
   - Added Material 3 time picker state
   - Cleaner UI, better UX

2. **GoogleAppsScript.js**
   - Complete rewrite of doPost() insertion logic
   - Now finds correct position based on timestamp
   - Maintains chronological order

---

## Benefits

✅ **Better UX:** Native scroll wheel time picker  
✅ **Accurate data:** Events in correct chronological order  
✅ **No manual sorting:** Sheet auto-sorts on insert  
✅ **Flexible:** Can backdate any event  
✅ **Intuitive:** Familiar Material Design time picker

---

## Next Steps

1. **Redeploy Google Apps Script** (critical!)
   ```bash
   ./deploy-script.sh
   ```

2. **Rebuild Android app**
   - Build → Build APK(s)

3. **Test with real data**
   - Log current event
   - Log backdated event
   - Verify sheet order

---

## Notes

- TimePicker requires Material 3 (already using it)
- Sheet sorting is automatic on each insert
- Performance: O(n) where n = number of rows (acceptable)
- Empty sheet handling: First event goes to row 2
- Timezone: Uses device timezone for comparison
