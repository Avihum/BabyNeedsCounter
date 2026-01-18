# 🚀 Quick Start Guide

Get up and running with Baby Needs Counter in 5 minutes!

## ⚡ TL;DR

1. Clone and build the app
2. Create a Google Sheet
3. Deploy the Apps Script
4. Add the URL to app settings
5. Start tracking!

---

## 📱 Step 1: Install the App (2 minutes)

### Option A: Build from Source
```bash
git clone https://github.com/Avihum/BabyNeedsCounter.git
cd BabyNeedsCounter
./gradlew assembleDebug
```

### Option B: Android Studio
1. Clone the repo
2. Open in Android Studio
3. Click Run (▶️)

---

## ☁️ Step 2: Set Up Google Sheets Backend (3 minutes)

### Create the Sheet
1. Go to [Google Sheets](https://sheets.google.com)
2. Create a new spreadsheet
3. Add headers: `Timestamp` | `Type` | `Notes`

### Deploy the Script
1. In Google Sheets: **Extensions → Apps Script**
2. Delete existing code
3. Copy all content from `GoogleAppsScript.js`
4. Paste and save
5. **Deploy → New deployment**
6. Choose **Web app**
7. Set "Who has access" to **Anyone**
8. Click **Deploy** and authorize
9. **Copy the Web App URL** (looks like `https://script.google.com/macros/s/.../exec`)

### Configure the App
1. Open Baby Needs Counter app
2. Tap **⚙️ Settings** (top-right)
3. Paste the Web App URL
4. Tap **Save Settings**
5. Go back to home screen

---

## ✅ Step 3: Test It Out

1. Tap any activity button (e.g., "Pee Only")
2. Check your Google Sheet
3. See the new row appear!

---

## 🎉 You're Ready!

### What You Can Do Now

✅ **Log activities** - Tap buttons to track diaper changes and feedings  
✅ **View stats** - See today's counts at the top of the home screen  
✅ **Add widgets** - Long-press home screen → Widgets → Baby Needs Counter  
✅ **Custom times** - Enable "Log from different time" to backdate entries  
✅ **Share data** - Give the Google Sheet URL to other caregivers  

### Next Steps

- 📖 Read the [full documentation](README.md)
- 🎨 Add [screenshots](SCREENSHOTS.md) to the README
- 🐛 Report any [issues](https://github.com/Avihum/BabyNeedsCounter/issues)
- ⭐ Star the repo if you find it useful!

---

## ❓ Common Questions

**Q: Do I need internet for tracking?**  
A: Yes, for now. Offline mode is on the roadmap!

**Q: Can multiple people use the same sheet?**  
A: Absolutely! Share the Web App URL with all caregivers.

**Q: Is my data private?**  
A: The data lives in YOUR Google Sheet. Keep the URL private!

**Q: Can I export the data?**  
A: Yes! Download your Google Sheet as CSV/Excel anytime.

**Q: What if sync fails?**  
A: Check the [Troubleshooting guide](TROUBLESHOOTING.md).

---

## 🆘 Need Help?

- 📘 [Full Setup Guide](BACKEND_SETUP.md)
- 🔧 [Troubleshooting](TROUBLESHOOTING.md)
- 💬 [Open an Issue](https://github.com/Avihum/BabyNeedsCounter/issues)

---

**Time to track**: ~5 minutes  
**Lines of code you need to write**: 0  
**Baby activities you can now easily track**: ∞
