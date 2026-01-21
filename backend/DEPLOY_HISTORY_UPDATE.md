# Deploying History Feature Backend Update

## Quick Steps

### 1. Open Your Google Apps Script
1. Go to your Google Sheet
2. Click **Extensions** → **Apps Script**
3. You'll see your existing script

### 2. Update the Script
1. Select all existing code (Ctrl+A / Cmd+A)
2. Copy the updated `GoogleAppsScript.js` from this directory
3. Paste to replace all code
4. Click **Save** (disk icon)

### 3. Deploy New Version
1. Click **Deploy** → **Manage deployments**
2. Click the pencil icon (✏️) next to your active deployment
3. Under **Version**, select **New version**
4. Add description: "Added history management endpoints"
5. Click **Deploy**
6. Copy the new **Web App URL** (if changed)

### 4. Update App (If URL Changed)
1. Open Baby Needs Counter app
2. Go to Settings
3. Paste the new Web App URL
4. Click "Test API" to verify
5. Click "Save"

## What's New in the Backend

### New Functions Added
```javascript
getDetailedEvents()  // Fetch event list with row numbers
updateEvent()        // Update event time/type/notes
deleteEvents()       // Delete multiple events
```

### New API Endpoints
```javascript
// Get detailed events
GET ?action=getEvents&startTime=YYYY-MM-DD HH:mm

// Update event
PUT { action: "update", rowNumber, timestamp, type, notes }

// Delete events
PUT { action: "delete", rowNumbers: [1, 2, 3] }
```

## Testing the Update

### Test in Apps Script Editor
1. Click **Run** → Select `doGet`
2. Check execution log for errors
3. Click **Run** → Select `doPut`
4. Verify no syntax errors

### Test in App
1. Open app and add a test event
2. Go to History screen (List icon)
3. Try editing an event
4. Try deleting an event
5. Check Google Sheet to verify changes

## Troubleshooting

### "Script not found" Error
- Re-deploy the script
- Copy new Web App URL
- Update in app settings

### "Permission denied" Error
- In Apps Script: **Deploy** → **Manage deployments**
- Verify "Who has access" is set to **Anyone**
- Save changes

### Events Not Showing
- Check execution logs in Apps Script
- Verify startTime parameter format
- Check sheet has data in correct format

### Update/Delete Not Working
- Verify deployment is set to "Execute as: Me"
- Check Apps Script execution log for errors
- Ensure sheet permissions allow editing

## Rollback (If Needed)
1. **Deploy** → **Manage deployments**
2. Click pencil icon on active deployment
3. Under **Version**, select previous version
4. Click **Deploy**
5. The old version is now active

## Verification Checklist
- [ ] Script saved without errors
- [ ] New version deployed successfully
- [ ] Web App URL updated in app (if changed)
- [ ] Can view events in History screen
- [ ] Can edit event time
- [ ] Can change event action type
- [ ] Can edit event notes
- [ ] Can delete single event
- [ ] Can delete multiple events
- [ ] Changes reflect in Google Sheet
- [ ] No errors in Apps Script execution log

## Notes
- Keep your Web App URL secure
- The "Anyone" access only allows API calls, not sheet editing
- Execution logs are in Apps Script under **Executions**
- Each deployment version is preserved for rollback

## Support
If you encounter issues:
1. Check Apps Script execution logs
2. Verify sheet permissions
3. Test API endpoint in app settings
4. Check app logs in Android Studio

---

**Deployment Date**: January 21, 2026
**Script Version**: 2.0 (History Feature)
