# New Features

## Summary of Updates

### 1. View Progress Link
- Added a "View Progress" button in the main screen that opens the configured Google Sheet
- This allows you to quickly access your baby's activity log directly from the app
- The button only appears when a Google Sheet URL is configured

### 2. Working Today and Last Counters
- **Today**: Now shows the actual count of events logged today (refreshes after each log)
- **Last**: Shows how long ago the last event was logged (e.g., "2h ago", "15m ago", "Just now")
- Stats are automatically fetched from your Google Sheet
- Data refreshes automatically when you log a new event

### 3. Combo Event Buttons
Added two new combo event types:
- **Pee + Feed** 💧🐄: For when baby pees and feeds at the same time
- **Poop + Feed** 💩🐄: For when baby poops and feeds at the same time

These combo buttons log a single event with a combined type, making it easier to track multiple activities that happen together.

### 4. LIFO Order (Newest First)
- Events are now inserted at the **top** of your Google Sheet (right after the header row)
- This means the most recent events appear first when you open your sheet
- Makes it much easier to see the latest activities without scrolling

## How to Update Your Google Apps Script

To enable these new features, you need to update your Google Apps Script code:

1. Open your Google Sheet
2. Go to **Extensions → Apps Script**
3. Replace the existing code with the updated code from `GoogleAppsScript.js` in this project
4. Click **Deploy → Manage deployments**
5. Click the **Edit** icon (pencil) on your existing deployment
6. Select **New version** from the Version dropdown
7. Click **Deploy**
8. Your app will now work with all the new features!

## Event Types

The app now supports these event types:
- `poop_pee` - Poop & Pee diaper change
- `pee` - Pee only diaper change
- `feed` - Feeding (breastmilk)
- `pee_feed` - Pee + Feed combo ✨ NEW
- `poop_feed` - Poop + Feed combo ✨ NEW
- `test` - Test event (when you use the Test button in settings)

## Google Sheet Format

Your Google Sheet should have these columns:
1. **Timestamp** - When the event occurred (YYYY-MM-DD HH:MM:SS)
2. **Type** - The event type (see list above)
3. **Notes** - Optional notes (currently not used, but available for future features)

Events will now appear with the newest at the top (row 2, right after the header).
