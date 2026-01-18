# Troubleshooting Guide

## "Nothing happens when I press buttons"

If you're not seeing any response when pressing the action buttons, follow these steps:

### 1. Check Logcat in Android Studio

Open the Logcat window (View → Tool Windows → Logcat) and filter for "BabyNeeds":

**Look for these log messages:**
- `Logging event: poop_pee` - Button was pressed
- `Using URL: https://...` - The URL being used
- `Sending JSON: {...}` - The data being sent
- `Successfully logged event` - Success message
- `Failed to log event` - Error message with status code

### 2. Use the Visual Feedback

The app now shows **Snackbar messages** at the bottom of the screen:

- **"⚠️ Please configure Google Sheets URL in Settings"** - You need to add a URL first
- **"Syncing..."** - Request is being sent
- **"✓ Logged successfully!"** - It worked!
- **"❌ Failed to sync"** - There's a problem with the URL or connection

### 3. Test Your Connection

In the Settings screen:
1. Enter your Google Sheets Web App URL
2. Click **"Test"** button (next to Save)
3. Check the result message:
   - **✓ Connection successful!** - Your backend is working!
   - **❌ Connection failed** - See steps below

### 4. Common Issues & Solutions

#### Issue: "No Google Sheets URL configured"
**Solution:** 
- Open Settings (⚙️ icon)
- Enter your Google Apps Script Web App URL
- Click "Save"

#### Issue: "Connection failed" or HTTP error codes
**Possible causes:**

**A. Backend not deployed:**
- Make sure you deployed your Apps Script as a **Web App** (not just saved it)
- Go to Apps Script → Deploy → Manage deployments
- Verify it says "Active" under Status

**B. Wrong URL:**
- URL should look like: `https://script.google.com/macros/s/AKfycby.../exec`
- Should end with `/exec`
- Copy it exactly from the deployment dialog

**C. Permissions not set:**
- In deployment settings, verify:
  - "Execute as" = **Me**
  - "Who has access" = **Anyone**

**D. No internet connection:**
- Check your device/emulator has internet access
- Try opening a website in the browser

#### Issue: Error 403 (Forbidden)
**Solution:**
- Re-deploy the Apps Script with correct permissions
- Make sure "Who has access" is set to "Anyone"

#### Issue: Error 404 (Not Found)
**Solution:**
- The URL is incorrect or the deployment was deleted
- Create a new deployment and update the URL in settings

#### Issue: "Script function not found"
**Solution:**
- Make sure your Apps Script has the `doPost(e)` function
- The function name must be exactly `doPost` (case-sensitive)

### 5. Debug Checklist

Run through this checklist:

- [ ] I created a Google Sheet
- [ ] I added the Apps Script code (from `GoogleAppsScript.js`)
- [ ] I saved the script (💾 icon)
- [ ] I deployed it as a Web App (Deploy → New deployment → Web app)
- [ ] "Who has access" is set to **Anyone**
- [ ] I copied the Web App URL (ends with `/exec`)
- [ ] I pasted the URL in the app's Settings
- [ ] I clicked "Save" in settings
- [ ] I clicked "Test" and saw success message
- [ ] My device/emulator has internet access

### 6. View Full Logs

To see detailed logs:

1. Open Android Studio
2. Go to View → Tool Windows → Logcat
3. Search for: `BabyNeeds` or `BackendService`
4. Look for error messages and stack traces

**Example of successful logs:**
```
D/BabyNeeds: Logging event: poop_pee
D/BabyNeeds: Google Sheet URL: https://script.google.com/macros/s/.../exec
D/BackendService: Using URL: https://script.google.com/macros/s/.../exec
D/BackendService: Sending JSON: {"timestamp":"2026-01-18 15:30:00","type":"poop_pee","notes":""}
D/BackendService: Making HTTP POST request...
D/BackendService: Successfully logged event: poop_pee
D/BackendService: Response: {"status":"success","message":"Event logged successfully"}
```

**Example of error logs:**
```
E/BackendService: Failed to log event. Status code: 403
E/BackendService: Response body: Authorization required
```

### 7. Test Without the App

You can test your backend directly using curl:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"timestamp":"2026-01-18 15:30:00","type":"test","notes":"curl test"}' \
  https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec
```

If this works, your backend is fine and the issue is in the app.

### 8. Still Not Working?

If you've tried everything above:

1. **Check the Google Sheet** - Does it have the correct columns? (Timestamp, Type, Notes)
2. **Re-deploy** - Delete the deployment and create a new one
3. **Try a simple test** - Can you access the URL in a browser?
4. **Check script execution logs** - In Apps Script, go to "Executions" tab to see if requests are reaching your script

### 9. Quick Test Script

To verify your backend independently, you can use this test:

Open your browser's Developer Console (F12) and paste:

```javascript
fetch('https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec', {
  method: 'POST',
  mode: 'no-cors',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    timestamp: new Date().toLocaleString(),
    type: 'browser_test',
    notes: 'Testing from browser'
  })
})
.then(() => console.log('Request sent'))
.catch(error => console.error('Error:', error));
```

Check your Google Sheet to see if a row was added.

## Need More Help?

If you're still stuck:
1. Share the Logcat output (search for "BabyNeeds")
2. Verify your Apps Script is deployed correctly
3. Test the URL with curl or browser first
4. Make sure you have internet connectivity
