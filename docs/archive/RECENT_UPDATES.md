# Recent Updates

## ✅ Fixed Widget Refresh Issue

**Problem:** Widget showed different counts than the main app (e.g., 2 vs 4 pee)

**Solution:**
- Changed widget update interval from 30 minutes to 5 minutes
- Widget now refreshes every 5 minutes automatically
- Widget also refreshes after each logged event

**File changed:** `app/src/main/res/xml/widget_info.xml`
- `updatePeriodMillis`: 1800000 (30 min) → 300000 (5 min)

---

## ✅ Added Notes/Comments Input

**Problem:** Google Sheet has a "Notes" column but no way to add notes from the app

**Solution:**
- Added "Notes (optional)" text field above the Track button
- Notes are now saved to the Google Sheet
- Notes field clears after logging
- Always visible for easy access

**Features:**
- Single-line input
- Placeholder text: "e.g., yellow, runny, fussy"
- Disabled when loading
- Automatically clears after tracking

**Files changed:**
- `MainActivity.kt` - Added notes state, TextField UI, and updated logEvent function

---

## How to Test

1. **Widget refresh:**
   - Rebuild the app
   - Remove and re-add the widget
   - Log an event and wait ~5 minutes
   - Widget should update automatically

2. **Notes input:**
   - Select an event (Pee, Poop, or Feed)
   - Type a note in the "Notes (optional)" field
   - Click Track
   - Check your Google Sheet - the note should appear in the Notes column

---

## Notes

- Widget updates happen every 5 minutes in the background
- The main app always shows real-time data (fetches on load and after logging)
- Notes are optional - leave blank if not needed
