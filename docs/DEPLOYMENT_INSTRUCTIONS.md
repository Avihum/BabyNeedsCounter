# Google Apps Script Deployment Instructions

## Problem: Sheet is still growing top-down instead of LIFO (newest at top)

If your sheet is still adding rows at the bottom instead of at the top, follow these steps:

---

## Step 1: Update the Script

1. Open your Google Sheet
2. Go to **Extensions → Apps Script**
3. **Delete all existing code** in the editor
4. Copy and paste the code from **GoogleAppsScript.js** (or try GoogleAppsScript_ALTERNATIVE.js if the first doesn't work)
5. Click **Save** (disk icon)

---

## Step 2: Deploy as NEW Version (CRITICAL!)

This is the most important step - you MUST create a new version:

1. Click **Deploy** → **Manage deployments**
2. Click the **Edit** icon (pencil) next to your existing deployment
3. In the **Version** dropdown, select **"New version"** (NOT the existing version!)
4. Optionally add a description like "Added LIFO row insertion"
5. Click **Deploy**
6. **Copy the new Web App URL** (it should be the same as before, but now points to the new version)

---

## Step 3: Test the Behavior

### Option A: Test from the App
1. Open Baby Needs Counter app
2. Go to Settings and click **Test** button
3. Open your Google Sheet
4. Verify that the test event appears in **row 2** (right below the header)
5. Test again - the second test should now be in row 2, pushing the first test to row 3

### Option B: Test Manually in Sheet
1. In your Google Sheet with the script, add a test row manually at the bottom
2. Use the app to log an event
3. The new event should appear at **row 2**, NOT at the bottom

---

## Expected Behavior

**CORRECT (LIFO - Newest First):**
```
Row 1: Timestamp       | Type      | Notes     [HEADER]
Row 2: 2026-01-18 15:30 | pee      |           [NEWEST]
Row 3: 2026-01-18 14:15 | feed     |
Row 4: 2026-01-18 13:00 | poop_pee |           [OLDEST]
```

**WRONG (FIFO - Oldest First):**
```
Row 1: Timestamp       | Type      | Notes     [HEADER]
Row 2: 2026-01-18 13:00 | poop_pee |           [OLDEST]
Row 3: 2026-01-18 14:15 | feed     |
Row 4: 2026-01-18 15:30 | pee      |           [NEWEST - WRONG!]
```

---

## Troubleshooting

### Issue: Still adding rows at the bottom

**Solution 1: Check Version**
- Make sure you deployed as "New version" (not using old version)
- The version number should have incremented

**Solution 2: Clear Deployment and Redeploy**
1. Go to **Deploy → Manage deployments**
2. Click **Archive** on the old deployment
3. Click **Deploy → New deployment**
4. Choose type: **Web app**
5. Set "Execute as": **Me**
6. Set "Who has access": **Anyone**
7. Click **Deploy**
8. Copy the new URL
9. Update URL in app settings

**Solution 3: Try Alternative Script**
1. Use the code from **GoogleAppsScript_ALTERNATIVE.js**
2. This uses `insertRows(2, 1)` instead of `insertRowAfter(1)`
3. Follow deployment steps above

**Solution 4: Check Sheet Structure**
- Make sure row 1 is your header (Timestamp | Type | Notes)
- Try deleting all data rows and keeping only the header
- Log a new event and verify it goes to row 2

---

## How the Script Works

### insertRowAfter(1)
```javascript
sheet.insertRowAfter(1);  // Creates a new blank row after row 1
sheet.getRange(2, 1, 1, 3).setValues([...]);  // Fills the new row 2
```

### What happens:
1. Header is in row 1
2. New blank row is inserted after row 1 (becomes new row 2)
3. Old row 2 becomes row 3, old row 3 becomes row 4, etc.
4. New data is written to the new row 2
5. Result: Newest data is always in row 2

---

## Verification Checklist

- [ ] Script code updated with LIFO logic
- [ ] Saved the script
- [ ] Deployed as **NEW VERSION** (not reusing old version)
- [ ] Copied the Web App URL
- [ ] Updated URL in app settings (if it changed)
- [ ] Tested by logging an event
- [ ] Verified event appears in row 2
- [ ] Tested again and verified previous event moved to row 3

---

## Still Not Working?

1. **Check Apps Script Execution Log**:
   - In Apps Script editor, click **Executions** (clock icon on left)
   - Look for errors in recent executions
   
2. **Test the Script Directly**:
   - In Apps Script, add a test function:
   ```javascript
   function testInsert() {
     const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
     sheet.insertRowAfter(1);
     sheet.getRange(2, 1, 1, 3).setValues([["TEST", "test", ""]]);
   }
   ```
   - Run this function and see if it inserts at the top
   
3. **Check Permissions**:
   - Make sure "Execute as" is set to your Google account
   - Make sure you authorized the script to access your sheets

If none of this works, the alternative is to manually sort your sheet by timestamp in descending order (newest first) after logging events, or use a query to display data in reverse order.
