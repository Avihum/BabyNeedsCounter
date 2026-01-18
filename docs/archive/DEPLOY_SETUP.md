# Auto-Deploy Google Apps Script Setup

## One-Time Setup (5 minutes)

### 1. Login to clasp
```bash
clasp login
```
This opens a browser - just authorize it with your Google account.

### 2. Get your Script ID
1. Open your Google Apps Script project: https://script.google.com
2. Click **Project Settings** ⚙️ (left sidebar)
3. Copy the **Script ID**

### 3. Link this project to your script
```bash
echo '{"scriptId":"YOUR_SCRIPT_ID_HERE"}' > .clasp.json
```
Replace `YOUR_SCRIPT_ID_HERE` with the actual Script ID from step 2.

### 4. Test it
```bash
clasp pull
```
This should download your current script. If it works, you're set up!

---

## Daily Use

After making changes to `GoogleAppsScript.js`:

### Option 1: Simple command
```bash
./deploy-script.sh
```

### Option 2: Manual
```bash
clasp push && clasp deploy
```

That's it! No more manual redeployment. 🎉

---

## What each file does:
- `.clasp.json` - Links your local project to your Google Apps Script
- `.claspignore` - Tells clasp to only upload GoogleAppsScript.js
- `deploy-script.sh` - Convenience script to push and deploy in one command
- `appsscript.json` - Apps Script project configuration

## Troubleshooting
- If `clasp login` fails, make sure you're using the same Google account as your Apps Script
- If push fails, run `clasp pull` first to sync
