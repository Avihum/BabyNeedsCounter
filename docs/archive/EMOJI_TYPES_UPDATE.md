# Emoji Types Update

## What Changed

Event types are now logged as **emojis** instead of text for faster visual processing when viewing your Google Sheet!

## Emoji Type Mapping

| Event | Old Type | New Type | Display |
|-------|----------|----------|---------|
| Poop & Pee | `poop_pee` | `💩💧` | Poop & Pee |
| Pee Only | `pee` | `💧` | Pee |
| Feeding | `feed` | `🐄` | Feed |
| Pee + Feed | `pee_feed` | `💧🐄` | Pee + Feed |
| Poop + Feed | `poop_feed` | `💩🐄` | Poop + Feed |
| Test Event | `test` | `🧪` | Test |

## Your Google Sheet

Now when you open your Google Sheet, the **Type** column will show emojis:

```
Timestamp           | Type  | Notes
2026-01-18 15:30:00 | 💧🐄  |
2026-01-18 14:15:00 | 💧    |
2026-01-18 13:00:00 | 💩💧  |
2026-01-18 12:30:00 | 🐄    |
2026-01-18 11:45:00 | 💩🐄  |
```

## Benefits

✅ **Faster visual scanning** - Your brain processes emojis faster than text
✅ **Easier pattern recognition** - Quickly spot what types of events are most common
✅ **More compact** - Emojis take less space than "poop_pee"
✅ **Universal** - Works regardless of language

## How to Update

### 1. Update Google Apps Script

You **MUST** update your Google Apps Script to recognize emoji types:

1. Open your Google Sheet
2. Go to **Extensions → Apps Script**
3. Replace the code with the updated **GoogleAppsScript.js**
4. **Deploy as NEW VERSION**:
   - Deploy → Manage deployments
   - Edit → New version
   - Deploy

### 2. Existing Data

Your old data with text types (`poop_pee`, `pee`, etc.) will remain as-is. New events will use emoji types.

If you want to convert old data:
- Option 1: Leave it as-is (both types work fine)
- Option 2: Manually find & replace in your sheet:
  - `poop_pee` → `💩💧`
  - `pee` → `💧`
  - `feed` → `🐄`
  - `pee_feed` → `💧🐄`
  - `poop_feed` → `💩🐄`
  - `test` → `🧪`

### 3. Stats Still Work

The stats in the app (Today count, Last event) now look for emoji types. If you have mixed old text types and new emoji types:
- Old events with text types won't be counted in stats
- Only emoji types will be counted
- Solution: Convert old data or just continue - new events will be counted correctly

## Example Sheet

Your sheet will look like this:

```
Timestamp           | Type  | Notes
-------------------|-------|-------
2026-01-18 16:45   | 💧🐄  |       ← Pee + Feed
2026-01-18 15:30   | 💧    |       ← Pee only
2026-01-18 14:15   | 🐄    |       ← Feed
2026-01-18 13:00   | 💩💧  |       ← Poop & Pee
2026-01-18 12:00   | 💩🐄  |       ← Poop + Feed
2026-01-18 11:00   | 🧪    |       ← Test
```

Much easier to scan at a glance! 👀

## Mobile View

Emojis are especially helpful when viewing your sheet on mobile - you can quickly see patterns without zooming in to read text.

---

**Note**: Make sure your Google Sheets has emoji support enabled (it should by default). If emojis don't display correctly, your browser/device might need to be updated.
