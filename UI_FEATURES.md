# UI Features Added

## 1. Settings Icon (Cogwheel) ⚙️

**Location:** Top-right corner of the home screen

**Appearance:**
- Material Design 3 settings icon (cogwheel)
- Matches the app's color scheme
- Tappable icon button

**Behavior:**
- Tap to navigate to Settings screen
- Smooth navigation animation

---

## 2. Settings Screen

### Header
- **Back arrow** (←) on the left to return to home
- **"Settings" title** in bold

### Backend Configuration Section

**Title:** "Backend Configuration"
**Description:** "Connect your app to a Google Sheets backend"

**Input Field:**
- Multi-line text input (up to 3 lines)
- Label: "Google Sheets Web App URL"
- Placeholder: `https://script.google.com/macros/s/.../exec`
- Rounded corners (12dp)
- Material 3 outlined style

**Save Button:**
- Full-width button
- Rounded corners
- Shows loading spinner when saving
- Displays success message "✓ Settings saved successfully" after save

### Instructions Card

**Title:** "How to Set Up Google Sheets Backend"

**4 Numbered Steps:**
1. Create a Google Sheet with columns: Timestamp, Type, Notes
2. Go to Extensions → Apps Script in your Google Sheet
3. Create a doPost(e) function to handle incoming data
4. Deploy as Web App and copy the URL here

**Styling:**
- Each step has a numbered badge (1, 2, 3, 4)
- Color-coded badges with primary color
- Card with surface variant background
- Rounded corners (16dp)

---

## 3. Navigation Flow

```
Home Screen
    ↓ (tap ⚙️)
Settings Screen
    ↓ (tap ←)
Home Screen
```

---

## 4. Visual Hierarchy

### Home Screen
```
┌─────────────────────────────────┐
│                            [⚙️]  │ ← Settings icon added here
├─────────────────────────────────┤
│                                 │
│         Baby Needs              │
│   Track your baby's daily...    │
│                                 │
│   ┌──────────┐  ┌──────────┐   │
│   │  Today   │  │   Last   │   │
│   │    0     │  │    —     │   │
│   └──────────┘  └──────────┘   │
│                                 │
│   ┌────────────────────────┐   │
│   │ 💩 Diaper Change       │   │
│   │    Poop & Pee          │   │
│   └────────────────────────┘   │
│                                 │
│   ... more action cards ...     │
└─────────────────────────────────┘
```

### Settings Screen
```
┌─────────────────────────────────┐
│ ← Settings                      │
├─────────────────────────────────┤
│                                 │
│  Backend Configuration          │
│  Connect your app to a Google   │
│  Sheets backend                 │
│                                 │
│  ┌───────────────────────────┐ │
│  │ Google Sheets Web App URL │ │
│  │ https://script.google...  │ │
│  └───────────────────────────┘ │
│                                 │
│  ┌───────────────────────────┐ │
│  │     Save Settings         │ │
│  └───────────────────────────┘ │
│                                 │
│  ✓ Settings saved successfully  │
│                                 │
│  ┌───────────────────────────┐ │
│  │ How to Set Up Google...   │ │
│  │                           │ │
│  │  ① Create a Google Sheet  │ │
│  │  ② Go to Extensions...    │ │
│  │  ③ Create a doPost(e)...  │ │
│  │  ④ Deploy as Web App...   │ │
│  └───────────────────────────┘ │
└─────────────────────────────────┘
```

---

## 5. Color Scheme

All new UI elements follow Material Design 3 theming:
- **Primary color** for buttons and accents
- **Surface color** for cards
- **Surface variant** for instruction card
- **On-surface colors** for text
- **Outline color** for text field borders

---

## 6. Interaction States

### Settings Icon
- Normal: Default icon color
- Pressed: Ripple effect
- Hover: Slight opacity change (on supported devices)

### Save Button
- Enabled: Primary color background
- Disabled: Muted colors (while saving)
- Loading: Shows circular progress indicator
- Success: Returns to enabled state, shows success message

### Text Input
- Unfocused: Outline color border
- Focused: Primary color border
- Error: Would show error color (not implemented yet)

---

## 7. Responsive Design

- Full-width layouts on all screen sizes
- Scrollable content on smaller screens
- Proper padding and spacing (24dp)
- Touch targets meet minimum size requirements
- Text fields expand to accommodate longer URLs
