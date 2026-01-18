# Updated Features - Baby Needs Counter

## Overview of Changes

All requested features have been implemented! Here's what's new:

---

## 1. ✅ Fixed Sheet URL Link

**Problem**: The "View Progress" button was trying to open the script URL instead of the actual Google Sheet.

**Solution**: 
- Added a separate field in Settings for the **Google Sheet View URL**
- Now you have two URL fields:
  - **Web App URL**: Used for logging events (API calls)
  - **View URL**: Used for viewing your data in the browser
  
**How to use**:
1. Go to Settings
2. Enter your Web App URL in the first field (the one you deployed from Apps Script)
3. Enter your actual Google Sheet URL in the second field (the one you see in your browser when viewing the sheet)
4. Save both URLs
5. The "View Progress" button will now open your actual sheet!

---

## 2. ✅ Working Today and Last Counters

**Problem**: The badges weren't showing real data from the backend.

**Solution**:
- Added automatic data fetching from Google Sheets when the app opens
- Stats refresh automatically after each event is logged
- Added debug logging to help troubleshoot any issues

**Features**:
- **Today**: Shows actual count of events logged today
- **Last**: Shows time since last event (e.g., "15m ago", "2h ago", "Just now")
- Auto-refreshes after every logged event

**Note**: Make sure your Google Apps Script is updated (see GoogleAppsScript.js) for this to work properly.

---

## 3. ✅ Flexible Multi-Event Logging

**Problem**: Having separate buttons for each combo was inflexible.

**Solution**: Implemented a **multi-select mechanism** where you can:
- Tap any combination of events (Poop, Pee, Feed)
- Select multiple events before logging
- A "Log Events" button appears when you have selections
- Shows count of selected events on the button
- Events are logged together with their types joined

**How it works**:
1. Tap the events you want to log (cards highlight when selected)
2. A large "Log Events (X)" button appears at the bottom
3. Tap to log all selected events at once
4. Events are sent to the backend as comma-separated types (e.g., "pee,feed")
5. Selections clear after logging

**Visual feedback**:
- Selected cards have a colored border
- Background changes to a tinted version of the event color
- Checkmark appears on selected cards
- Cards have elevation effect when selected

---

## 4. ✅ Bigger Everything!

**Changes to App**:
- **Header**: 36sp (from 28sp)
- **Subtitle**: 18sp (from 14sp)
- **View Progress button**: Larger icon (20sp) and text (titleMedium weight)
- **Stat Cards**: 
  - Increased from 150x100 to 165x120
  - Value text: 36sp (huge and bold)
  - Label text: 15sp
  - More padding (20dp)
- **Action Cards**:
  - Height increased from 90dp to 110dp
  - Icon size: 64dp (from 50dp)
  - Icon emoji: 32sp (from 24sp)
  - Title: 20sp, bold
  - Subtitle: 16sp
  - More padding throughout
- **Log Button**: 64dp height with 20sp text
- **Settings**: All text inputs and buttons increased to 16-18sp

**Changes to Widget**:
- **Status text**: 14sp (from 10sp) with medium font weight
- **Last event indicator**: 20sp (from 16sp)
- **Button emojis**: 32sp (from 24sp)
- **Button labels**: 13sp (from 9sp) with medium font weight
- **More padding**: 8dp throughout (from 4dp)

---

## How to Update

### 1. Update Your Google Apps Script

The script now needs to handle:
- LIFO insertion (new rows at top)
- Multiple event types in stats
- Proper timestamp parsing

**Steps**:
1. Open your Google Sheet
2. Go to **Extensions → Apps Script**
3. Replace the code with the updated `GoogleAppsScript.js`
4. Deploy as new version:
   - **Deploy → Manage deployments**
   - Click **Edit** icon
   - Select **New version**
   - Click **Deploy**

### 2. Configure Two URLs in App

1. Open the app and go to Settings
2. Enter your **Web App URL** (the one that ends with `/exec`)
3. Enter your **Sheet View URL** (the regular Google Sheets URL from your browser)
4. Click **Save**
5. Click **Test** to verify the connection

### 3. Using the New Multi-Select Feature

1. From the home screen, tap one or more event cards
2. Watch them highlight with colored borders
3. Tap the "Log Events" button that appears
4. Events are logged together and selections clear

---

## Supported Event Types

The app now tracks these combinations:
- `poop_pee` - Poop & Pee diaper change
- `pee` - Pee only diaper change  
- `feed` - Feeding (breastmilk)
- `pee,feed` - When you select both Pee and Feed
- `poop_pee,feed` - When you select both Poop & Pee and Feed
- Any other combination you select!

---

## Troubleshooting

### Today/Last counters show 0 or "—"
- Make sure you've updated the Google Apps Script
- Check that both URLs are configured in Settings
- Verify you deployed the script as a **new version**
- Check the app logs for any error messages

### View Progress button doesn't appear
- Make sure you've entered the **Sheet View URL** in Settings
- Save the settings after entering the URL

### Multi-select doesn't work
- Make sure you're tapping the cards (not long-pressing)
- Selected cards should show a colored border and checkmark
- The Log button appears below the cards when items are selected

---

## Technical Details

### Data Flow
1. You select events in the app
2. Events are joined as comma-separated types
3. Sent to Google Apps Script as POST request
4. Script inserts row at position 2 (LIFO - newest first)
5. App fetches updated stats from script via GET request
6. UI updates with new counts and timestamps

### File Changes
- `MainActivity.kt` - Multi-select UI, bigger fonts, dual URL support
- `SettingsScreen.kt` - Added View URL field, bigger text
- `SettingsManager.kt` - Storage for both URLs
- `BackendService.kt` - Updated stats parsing
- `widget_baby_needs.xml` - Bigger fonts and elements
- `GoogleAppsScript.js` - LIFO insertion, better stats

Enjoy your updated Baby Needs Counter! 🍼
