# 🔧 Backend Scripts

This folder contains the Google Apps Script backend and deployment tools for Baby Needs Counter.

## 📁 Files

### Core Backend
- **`GoogleAppsScript.js`** - Main Google Apps Script for Google Sheets integration
  - Handles incoming event logging via POST requests
  - Fetches today's statistics via GET requests
  - Manages duplicate detection and sorting
  - Timezone-aware date handling

- **`GoogleAppsScript_ALTERNATIVE.js`** - Alternative implementation (if needed)

### Configuration Files
- **`appsscript.json`** - Apps Script project configuration
- **`.clasp.json`** - Clasp CLI configuration for deployment
- **`.claspignore`** - Files to ignore during deployment

### Deployment
- **`deploy-script.sh`** - Automated deployment script (requires clasp CLI)

## 🚀 Deployment Guide

See the main documentation: [BACKEND_SETUP.md](../docs/BACKEND_SETUP.md)

### Quick Deploy with Clasp

```bash
# Install clasp CLI (one-time setup)
npm install -g @google/clasp

# Login to Google
clasp login

# Deploy from this folder
cd backend
./deploy-script.sh
```

### Manual Deploy

1. Go to [Google Sheets](https://sheets.google.com)
2. Create a new spreadsheet with columns: `Timestamp | Type | Notes`
3. Go to **Extensions → Apps Script**
4. Copy all content from `GoogleAppsScript.js`
5. Paste and save
6. **Deploy → New deployment → Web app**
7. Set "Who has access" to **Anyone**
8. Copy the Web App URL

## 🔗 API Endpoints

### POST - Log Event
```json
POST https://script.google.com/macros/s/{SCRIPT_ID}/exec
Content-Type: application/json

{
  "timestamp": "2026-01-18 14:30",
  "type": "💩💧",
  "notes": "Optional notes"
}
```

### GET - Fetch Stats
```
GET https://script.google.com/macros/s/{SCRIPT_ID}/exec

Response:
{
  "peeCount": 5,
  "poopCount": 3,
  "lastFeedTimeISO": "2026-01-18T12:00:00.000Z"
}
```

## 📝 Event Types

- `💩💧` - Poop & Pee
- `💧` - Pee only
- `🐄` - Feeding/Breastmilk

## 🔧 Customization

The script can be modified to:
- Add new event types
- Change timezone handling
- Add data validation
- Implement authentication
- Send email notifications
- Create automated reports

After modifications, redeploy the script!

## 🐛 Troubleshooting

**Script not responding?**
- Verify deployment settings: "Who has access" = Anyone
- Check Apps Script execution logs
- Ensure Google Sheets has correct column headers

**Data not appearing?**
- Check Apps Script logs: **Executions** tab
- Verify column names: `Timestamp`, `Type`, `Notes`
- Ensure sheet is the active sheet (first tab)

## 📚 Related Documentation

- [Backend Setup Guide](../docs/BACKEND_SETUP.md)
- [Deployment Instructions](../docs/DEPLOYMENT_INSTRUCTIONS.md)
- [Troubleshooting](../docs/TROUBLESHOOTING.md)
