# Widget Combo Functionality Update

## What's New

The widget now has **combo/rendezvous functionality** with 5 buttons instead of 3!

### Widget Layout

**Top Row (Basic Events):**
- 💩 **Poop** - Logs poop & pee diaper change
- 💧 **Pee** - Logs pee only diaper change
- 🐄 **Feed** - Logs feeding

**Bottom Row (Combo Events):**
- 💧🐄 **Pee+Feed** - Logs both pee and feeding together
- 💩🐄 **Poop+Feed** - Logs both poop and feeding together

### How It Works

Unlike the main app where you select multiple events and then tap "Log", the widget uses **dedicated combo buttons** because widgets have limited interactivity.

Each combo button logs a single event with a specific combo type:
- Pee+Feed logs as `"pee_feed"`
- Poop+Feed logs as `"poop_feed"`

### Event Types Logged

- `poop_pee` - Poop & Pee
- `pee` - Pee only
- `feed` - Feed only
- `pee_feed` - Pee + Feed combo ✨
- `poop_feed` - Poop + Feed combo ✨

These match what the main app logs when you select multiple events together.

### Widget Size

The widget now has 2 rows of buttons:
- **Status area** at the top (shows sync status)
- **Row 1**: 3 basic action buttons
- **Row 2**: 2 combo action buttons

Make sure your widget is sized appropriately to show both rows. If you see buttons cut off, resize the widget to be taller.

### Updating Your Widget

If you already have the widget on your home screen:

1. **Remove the old widget** from your home screen (long press → remove)
2. **Rebuild the app** (the layout has changed)
3. **Add the widget again** from the widget picker
4. The new 5-button layout should appear

Or simply rebuild and the widget should update automatically on next refresh.

### Why This Approach?

Android widgets have limitations:
- Can't have toggle states that persist visually
- Limited interaction patterns
- Must use PendingIntents for clicks

So instead of the app's multi-select approach, we use **dedicated combo buttons** which is more widget-friendly and actually faster to use - one tap instead of selecting then logging.

---

## Common Combos Covered

The two combo buttons cover the most common scenarios:
1. **Pee + Feed**: Baby peed during or right before feeding
2. **Poop + Feed**: Baby pooped during or right before feeding

If you need other combos (like Poop + Pee + Feed), you can:
- Use the main app's multi-select feature
- Or we can add more combo buttons to the widget (though it gets crowded)

---

Enjoy your updated widget! 🎉
