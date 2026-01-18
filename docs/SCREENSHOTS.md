# Screenshots Guide

This guide will help you capture and add screenshots to showcase your app in the README.

## 📸 Required Screenshots

For a professional showcase, capture the following screens:

### 1. Home Screen (`home.png`)
- Main activity tracking interface
- Show the three quick stat cards at top
- Display all three action cards (Poop & Pee, Pee Only, Feeding)
- Include the settings icon in top-right
- Show the notes input field

### 2. Widgets (`widgets.png`)
- Android home screen showing both widgets
- **Baby Logging Widget** with the three action buttons
- **Baby Stats Widget** with today's statistics
- Clean background, preferably with other app icons visible

### 3. Settings Screen (`settings.png`)
- Settings interface with URL input field
- Show the setup instructions
- Display "Save Settings" button
- Include back navigation

### 4. Custom Time Picker (`time-picker.png`)
- Home screen with "Log from different time" enabled
- Vertical scroll wheel time picker visible
- Show hour and minute selection

### 5. Multi-Select Active (`multi-select.png`)
- Home screen with 2-3 activities selected
- Show the selected state (checkmarks, colored borders)
- Display the "Track (2)" or "Track (3)" button

## 🎬 Recording a Demo GIF

### Option 1: Android Studio
1. Open Android Studio
2. Run your app on an emulator
3. Click the camera icon in the emulator controls
4. Select "Record Screen"
5. Perform key actions:
   - Open app
   - Select multiple activities
   - Add a note
   - Click Track button
   - Show success message
   - Navigate to settings
   - Return and view updated stats
6. Stop recording
7. Convert the video to GIF using [ezgif.com](https://ezgif.com/video-to-gif)

### Option 2: Physical Device (ADB)
```bash
# Start recording (max 180 seconds)
adb shell screenrecord /sdcard/demo.mp4

# Stop with Ctrl+C, then pull the file
adb pull /sdcard/demo.mp4

# Convert to GIF using ffmpeg
ffmpeg -i demo.mp4 -vf "fps=10,scale=320:-1:flags=lanczos" -c:v gif demo.gif
```

### Option 3: Screen Recording Apps
- **Recommended**: [AZ Screen Recorder](https://play.google.com/store/apps/details?id=com.hecorat.screenrecorder.free)
- **Alternative**: [Screen Recorder - XRecorder](https://play.google.com/store/apps/details?id=videoeditor.videorecorder.screenrecorder)

## 📱 Best Practices

### Device Setup
- **Clean up**: Remove notification icons, set full battery, clean status bar
- **Time**: Set to a clean time like 10:00 or 2:00
- **Brightness**: Maximum brightness for clear screenshots
- **Orientation**: Portrait mode
- **DND**: Enable Do Not Disturb to avoid notifications

### Screenshot Settings
- **Resolution**: Native device resolution (1080p or higher)
- **Format**: PNG for screenshots, GIF for demos
- **Framing**: Include status bar and navigation bar
- **Content**: Use realistic but clean data

### Taking Screenshots

#### On Android Device
1. Press **Power + Volume Down** simultaneously
2. Screenshots save to `Pictures/Screenshots/`

#### Using ADB
```bash
# Take screenshot
adb exec-out screencap -p > screenshot.png

# Or use Android Studio's screenshot tool (camera icon in Device File Explorer)
```

## 🖼️ Organizing Screenshots

Place all screenshots in the `docs/screenshots/` folder:

```
docs/
└── screenshots/
    ├── home.png
    ├── widgets.png
    ├── settings.png
    ├── time-picker.png
    ├── multi-select.png
    └── demo.gif
```

## 🎨 Optional: Add Device Frames

Make screenshots look more professional by adding device frames:

### Using MockUPhone
1. Go to [mockuphone.com](https://mockuphone.com)
2. Select Android device (e.g., Pixel 8)
3. Upload your screenshot
4. Download the framed version

### Using Figma
1. Import screenshot into Figma
2. Use device mockup templates
3. Export as PNG

### Using Shot.pizza
1. Go to [shot.pizza](https://shot.pizza)
2. Upload screenshot
3. Choose device frame and background
4. Download result

## ✂️ Optimizing File Sizes

Before committing, optimize images:

```bash
# Install ImageOptim (macOS)
brew install imageoptim

# Or use online tools:
# - TinyPNG: https://tinypng.com
# - Squoosh: https://squoosh.app
```

Aim for:
- **Screenshots**: < 500KB each
- **Demo GIF**: < 5MB (max 10MB for GitHub)

## 📝 Updating README

After adding screenshots, verify they display correctly in `README.md`:

```markdown
![Home Screen](docs/screenshots/home.png)
![Demo](docs/demo.gif)
```

Preview the README on GitHub to ensure images load properly.

---

**Tip**: Capture screenshots in both light and dark modes if your app supports both!
