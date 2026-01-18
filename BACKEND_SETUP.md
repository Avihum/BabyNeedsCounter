# Backend Setup Guide

This guide will help you set up a Google Sheets backend for the Baby Needs Counter app.

## Overview

The app syncs baby activity data (diaper changes, feedings) to a shared Google Sheet, allowing multiple caregivers to track and view the baby's needs in real-time.

## Setup Steps

### 1. Create a Google Sheet

1. Go to [Google Sheets](https://sheets.google.com)
2. Create a new spreadsheet
3. Name it something like "Baby Needs Log"
4. Set up the following columns in Row 1:
   - Column A: **Timestamp**
   - Column B: **Type**
   - Column C: **Notes**

### 2. Create the Apps Script

1. In your Google Sheet, go to **Extensions → Apps Script**
2. Delete any existing code in the editor
3. Copy the entire contents of `GoogleAppsScript.js` from this repository
4. Paste it into the Apps Script editor
5. Click the **Save** icon (💾) and give your project a name (e.g., "Baby Needs Backend")

### 3. Deploy as Web App

1. In the Apps Script editor, click **Deploy → New deployment**
2. Click the gear icon (⚙️) next to "Select type" and choose **Web app**
3. Configure the deployment:
   - **Description**: "Baby Needs API v1"
   - **Execute as**: Select **Me** (your Google account)
   - **Who has access**: Select **Anyone** (required for the app to work)
4. Click **Deploy**
5. You may need to authorize the script:
   - Click **Authorize access**
   - Choose your Google account
   - Click **Advanced** if you see a warning
   - Click **Go to [Your Project Name] (unsafe)**
   - Click **Allow**
6. Copy the **Web app URL** - it should look like:
   ```
   https://script.google.com/macros/s/AKfycby.../exec
   ```

### 4. Configure the App

1. Open the Baby Needs Counter app on your Android device
2. Tap the **⚙️ Settings** icon (cogwheel) in the top-right corner
3. Paste the Web app URL into the **Google Sheets Web App URL** field
4. Tap **Save Settings**

### 5. Test the Integration

1. Go back to the main screen
2. Tap any of the action buttons (Poop & Pee, Pee Only, or Feeding)
3. Check your Google Sheet - you should see a new row with:
   - The current timestamp
   - The event type (e.g., "poop_pee", "pee", "feed")
   - Any notes (currently empty)

## Data Format

The app sends data in the following format:

```json
{
  "timestamp": "2026-01-18 14:30:00",
  "type": "poop_pee",
  "notes": ""
}
```

### Event Types

- `poop_pee` - Diaper change with both poop and pee
- `pee` - Diaper change with pee only
- `feed` - Feeding (breastmilk)

## Sharing with Other Caregivers

To share the log with other caregivers:

1. Share the Google Sheet with them (Share button in Google Sheets)
2. Share the Web App URL with them
3. They can add the URL to their Baby Needs Counter app

Everyone will see the same data in real-time!

## Troubleshooting

### "Failed to log event" error

- Make sure you deployed the script as a **Web app** (not just saved it)
- Verify "Who has access" is set to **Anyone**
- Check that the URL in the app settings is correct
- Make sure you have internet connectivity

### Data not appearing in the sheet

- Check that your Google Sheet has the correct columns
- Look at the Apps Script execution logs: **Executions** tab in Apps Script editor
- Verify the script was saved after pasting the code

### Authorization issues

- You may need to re-authorize if you make changes to the script
- Go to **Deploy → Manage deployments** and create a new version

## Privacy & Security

- The Web App URL is like a password - keep it private
- Anyone with the URL can add data to your sheet
- Consider using Google Sheet's built-in sharing permissions to control who can view/edit the data
- The app stores the URL locally on your device only

## Advanced Customization

You can modify the `GoogleAppsScript.js` file to:
- Add more data fields
- Implement data validation
- Send notifications
- Create automated reports
- Add authentication

Redeploy after making changes!
