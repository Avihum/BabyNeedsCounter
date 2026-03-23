// Google Apps Script for Baby Needs Counter Backend
// 
// Instructions:
// 1. Create a new Google Sheet with columns: Timestamp | Type | Notes
// 2. Go to Extensions → Apps Script
// 3. Copy this code into the script editor
// 4. Deploy as Web App (Deploy → New deployment)
// 5. Set "Execute as" to your account
// 6. Set "Who has access" to "Anyone" 
// 7. Copy the Web App URL and paste it in the app settings

function doPost(e) {
  try {
    // Parse the incoming JSON data
    const data = JSON.parse(e.postData.contents);
    
    // Check if this is an update or delete action
    const action = data.action;
    
    if (action === 'update') {
      return updateEvent(data);
    } else if (action === 'delete') {
      return deleteEvents(data);
    }
    
    // Otherwise, it's a regular event logging
    // Get the active spreadsheet
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    
    const newTimestamp = data.timestamp || new Date().toLocaleString();
    const newType = data.type || '';
    const newNotes = data.notes || '';
    
    // Parse the new event timestamp with robust parsing
    let newEventTime = new Date(newTimestamp);
    
    // If parsing failed, try manual parsing for "YYYY-MM-DD HH:MM" format
    if (isNaN(newEventTime.getTime())) {
      const match = newTimestamp.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{1,2}):(\d{2})$/);
      if (match) {
        newEventTime = new Date(
          parseInt(match[1]), // year
          parseInt(match[2]) - 1, // month (0-indexed)
          parseInt(match[3]), // day
          parseInt(match[4]), // hour
          parseInt(match[5])  // minute
        );
        Logger.log('Manually parsed timestamp: ' + newEventTime.toISOString());
      }
    }
    
    // Validate the new event time
    if (isNaN(newEventTime.getTime())) {
      return ContentService.createTextOutput(JSON.stringify({
        'status': 'error',
        'message': 'Invalid timestamp format: ' + newTimestamp
      }))
      .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Get all existing data (excluding header)
    const lastRow = sheet.getLastRow();
    
    if (lastRow === 1) {
      // Sheet is empty (only header), insert at row 2
      sheet.insertRowAfter(1);
      sheet.getRange(2, 1, 1, 3).setValues([[newEventTime, newType, newNotes]]);
      
      // Format the timestamp column as "yyyy-MM-dd HH:mm"
      sheet.getRange(2, 1).setNumberFormat('yyyy-MM-dd HH:mm');
      
      return ContentService.createTextOutput(JSON.stringify({
        'status': 'success',
        'message': 'Event logged successfully at row 2 (first event)',
        'rowNumber': 2
      }))
      .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Find the correct position to insert (sorted by timestamp, newest first)
    let insertPosition = 2; // Start after header
    
    Logger.log('New event time: ' + newEventTime.toISOString() + ' (parsed from: ' + newTimestamp + ')');
    
    for (let row = 2; row <= lastRow; row++) {
      const existingTimestamp = sheet.getRange(row, 1).getValue();
      
      if (!existingTimestamp) {
        Logger.log('Row ' + row + ': empty, inserting here');
        insertPosition = row;
        break;
      }
      
      // Parse existing timestamp - handle both Date objects and strings
      let existingTime;
      if (existingTimestamp instanceof Date) {
        existingTime = existingTimestamp;
      } else {
        existingTime = new Date(existingTimestamp);
      }
      
      // Validate the parsed date
      if (isNaN(existingTime.getTime())) {
        Logger.log('Row ' + row + ': WARNING - Invalid date parsed from: ' + existingTimestamp);
        // If we can't parse it, treat as very old and continue
        continue;
      }
      
      Logger.log('Row ' + row + ': ' + existingTime.toISOString() + ' | Comparing: ' + newEventTime.getTime() + ' > ' + existingTime.getTime() + ' = ' + (newEventTime > existingTime));
      
      // If new event is newer than existing, insert before it
      if (newEventTime > existingTime) {
        Logger.log('New event is newer, inserting at row ' + row);
        insertPosition = row;
        break;
      }
      
      // If we've reached the last row and new event is older, insert after
      if (row === lastRow) {
        Logger.log('Reached last row, new event is oldest, inserting after at row ' + (lastRow + 1));
        insertPosition = lastRow + 1;
      }
    }
    
    Logger.log('Final insert position: ' + insertPosition);
    
    // Insert the row at the correct position
    if (insertPosition <= lastRow) {
      sheet.insertRowBefore(insertPosition);
    } else {
      sheet.insertRowAfter(lastRow);
    }
    
    // Set the values in the new row (store as Date object for consistency)
    sheet.getRange(insertPosition, 1, 1, 3).setValues([[newEventTime, newType, newNotes]]);
    
    // Format the timestamp column as "yyyy-MM-dd HH:mm"
    sheet.getRange(insertPosition, 1).setNumberFormat('yyyy-MM-dd HH:mm');
    
    // Return success response
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'success',
      'message': 'Event logged successfully at row ' + insertPosition + ' (chronologically sorted)',
      'rowNumber': insertPosition
    }))
    .setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    // Return error response
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'error',
      'message': error.toString()
    }))
    .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  try {
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    const data = sheet.getDataRange().getValues();
    
    // Check for action parameter to determine which operation to perform
    const action = e.parameter.action || 'stats';
    
    if (action === 'getEvents') {
      // Return detailed event list with row numbers
      return getDetailedEvents(sheet, data, e);
    } else if (action === 'stats') {
      // Return summary statistics (default behavior)
      return getStats(sheet, data, e);
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'error',
      'message': 'Unknown action: ' + action
    }))
    .setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'error',
      'message': error.toString()
    }))
    .setMimeType(ContentService.MimeType.JSON);
  }
}

function getDetailedEvents(sheet, data, e) {
  const startTimeParam = e.parameter.startTime;
  const endTimeParam = e.parameter.endTime;
  
  Logger.log('GetDetailedEvents - StartTime parameter: ' + startTimeParam);
  Logger.log('GetDetailedEvents - EndTime parameter: ' + endTimeParam);
  Logger.log('Total rows in sheet: ' + data.length);
  
  let events = [];
  
  // Start from row 2 (skip header at row 1)
  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    if (!row[0]) continue; // Skip empty rows
    
    let eventTime;
    const timestamp = row[0];
    
    // Handle both Date objects and string timestamps
    if (timestamp instanceof Date) {
      eventTime = timestamp;
    } else {
      eventTime = new Date(timestamp);
    }
    
    // Validate the date
    if (isNaN(eventTime.getTime())) continue;
    
    // Filter by startTime if provided
    if (startTimeParam) {
      const startTime = new Date(startTimeParam);
      if (eventTime < startTime) continue;
    }
    
    // Filter by endTime if provided (exclusive upper bound)
    if (endTimeParam) {
      const endTime = new Date(endTimeParam);
      if (eventTime >= endTime) continue;
    }
    
    // Add event with row number (i + 1 because sheets are 1-indexed)
    events.push({
      rowNumber: i + 1,
      timestamp: eventTime.toISOString(),
      type: row[1] || '',
      notes: row[2] || ''
    });
  }
  
  Logger.log('Returning ' + events.length + ' events');
  
  return ContentService.createTextOutput(JSON.stringify({
    status: 'success',
    events: events
  }))
  .setMimeType(ContentService.MimeType.JSON);
}

function getStats(sheet, data, e) {
  // Get startTime parameter (7am-7am baby day)
  // Baby days run from 7am to 7am (not midnight to midnight)
  // This makes more sense for tracking baby activities
  const startTimeParam = e.parameter.startTime;
  
  Logger.log('StartTime parameter received: ' + startTimeParam);
  Logger.log('Total rows in sheet: ' + data.length);
  
  let todayEvents = [];
  if (startTimeParam) {
    // Parse the start time (format: "YYYY-MM-DD HH:mm")
    const startTime = new Date(startTimeParam);
    Logger.log('Parsed startTime: ' + startTime);
    
    // Filter events from startTime onwards (skip header row)
    todayEvents = data.slice(1).filter(row => {
      if (!row[0]) return false;
      
      let eventTime;
      const timestamp = row[0];
      
      // Handle both Date objects and string timestamps
      if (timestamp instanceof Date) {
        eventTime = timestamp;
      } else {
        // Try to parse string timestamp
        eventTime = new Date(timestamp);
      }
      
      // Include events from startTime onwards
      const isAfterStart = eventTime >= startTime;
      return isAfterStart;
    });
  } else {
    // Return all events if no startTime specified (skip header row)
    todayEvents = data.slice(1);
  }
  
  Logger.log('Filtered events count: ' + todayEvents.length);
  
  // Calculate statistics
  // Since rows are now in LIFO order (newest first), the first event is the most recent
  // Types are now emojis for faster visual processing
  
  // Count pee events (any event containing 💧)
  const peeCount = todayEvents.filter(row => {
    const type = row[1];
    return type && type.includes('💧');
  }).length;
  
  // Count poop events (any event containing 💩)
  const poopCount = todayEvents.filter(row => {
    const type = row[1];
    return type && type.includes('💩');
  }).length;

  const feedCount = todayEvents.filter(row => {
    const type = row[1];
    return type && String(type).includes('🐄');
  }).length;

  function cellToIso(cell) {
    if (!cell) return null;
    if (cell instanceof Date) {
      return cell.toISOString();
    }
    const parsed = new Date(cell);
    if (!isNaN(parsed.getTime())) {
      return parsed.toISOString();
    }
    return null;
  }

  // todayEvents is newest-first: first matching row is most recent today
  let lastPeeTimeISO = null;
  let lastPoopTimeISO = null;
  for (let i = 0; i < todayEvents.length; i++) {
    const row = todayEvents[i];
    if (!row[0] || !row[1]) continue;
    const type = String(row[1]);
    if (lastPeeTimeISO === null && type.includes('💧')) {
      lastPeeTimeISO = cellToIso(row[0]);
    }
    if (lastPoopTimeISO === null && type.includes('💩')) {
      lastPoopTimeISO = cellToIso(row[0]);
    }
    if (lastPeeTimeISO && lastPoopTimeISO) break;
  }
  
  // Find last feeding event (any event containing 🐄)
  // IMPORTANT: Look at ALL events (not just today) because next feed time
  // is independent of the 7am-7am baby day logic
  const allEvents = data.slice(1); // Skip header row
  const feedEvents = allEvents.filter(row => {
    if (!row[0]) return false;
    const type = row[1];
    return type && type.includes('🐄');
  });
  
  // Return the raw timestamp - let the client calculate time difference
  let lastFeedTimeISO = null;
  if (feedEvents.length > 0) {
    const feedTime = feedEvents[0][0];
    if (feedTime instanceof Date) {
      lastFeedTimeISO = feedTime.toISOString();
    } else {
      // Try to parse string timestamp
      const parsed = new Date(feedTime);
      if (!isNaN(parsed.getTime())) {
        lastFeedTimeISO = parsed.toISOString();
      }
    }
  }
  
  // Latest sleep event (sheet order: newest first — same as allEvents[0])
  // Column B: 😴 = fell asleep, ☀️ = woke up (emoji-only). Legacy text rows still supported.
  const SHEET_SLEEP_STARTED = '😴';
  const SHEET_SLEEP_ENDED = '☀️';
  let lastSleepEventType = null;
  let lastSleepEventTimeISO = null;
  for (let i = 0; i < allEvents.length; i++) {
    const row = allEvents[i];
    if (!row[0]) continue;
    const type = row[1];
    if (type === SHEET_SLEEP_STARTED || type === SHEET_SLEEP_ENDED ||
        type === 'sleep_started' || type === 'sleep_ended') {
      lastSleepEventType = type;
      const t = row[0];
      let eventTime;
      if (t instanceof Date) {
        eventTime = t;
      } else {
        eventTime = new Date(t);
      }
      if (!isNaN(eventTime.getTime())) {
        lastSleepEventTimeISO = eventTime.toISOString();
      }
      break;
    }
  }

  /** Completed wake windows in baby day: ☀️ (woke) → 😴 (fell asleep). Last one is "previous" when baby is awake now. */
  function formatDurationMs(ms) {
    const m = Math.floor(ms / 60000);
    if (m < 60) return m + 'm';
    const h = Math.floor(m / 60);
    const mm = m % 60;
    return mm > 0 ? (h + 'h ' + mm + 'm') : (h + 'h');
  }

  function isWakeMarker(type) {
    return type === SHEET_SLEEP_ENDED || type === 'sleep_ended';
  }
  function isSleepMarker(type) {
    return type === SHEET_SLEEP_STARTED || type === 'sleep_started';
  }

  let previousWakeWindowLabel = null;
  const sleepMarkers = [];
  for (let i = 0; i < todayEvents.length; i++) {
    const row = todayEvents[i];
    if (!row[0] || !row[1]) continue;
    const type = String(row[1]);
    if (!isWakeMarker(type) && !isSleepMarker(type)) continue;
    let eventTime;
    const ts = row[0];
    if (ts instanceof Date) {
      eventTime = ts;
    } else {
      eventTime = new Date(ts);
    }
    if (isNaN(eventTime.getTime())) continue;
    sleepMarkers.push({ t: eventTime.getTime(), type: type });
  }
  sleepMarkers.sort(function (a, b) { return a.t - b.t; });
  const completedWakeMs = [];
  for (let i = 0; i < sleepMarkers.length - 1; i++) {
    if (isWakeMarker(sleepMarkers[i].type) && isSleepMarker(sleepMarkers[i + 1].type)) {
      const dur = sleepMarkers[i + 1].t - sleepMarkers[i].t;
      if (dur > 0) completedWakeMs.push(dur);
    }
  }
  if (sleepMarkers.length > 0 && isWakeMarker(sleepMarkers[sleepMarkers.length - 1].type) && completedWakeMs.length > 0) {
    previousWakeWindowLabel = formatDurationMs(completedWakeMs[completedWakeMs.length - 1]);
  }
  
  const stats = {
    peeCount: peeCount,
    poopCount: poopCount,
    feedCount: feedCount,
    previousWakeWindowLabel: previousWakeWindowLabel,
    lastPeeTimeISO: lastPeeTimeISO,
    lastPoopTimeISO: lastPoopTimeISO,
    lastFeedTimeISO: lastFeedTimeISO,
    lastSleepEventType: lastSleepEventType,
    lastSleepEventTimeISO: lastSleepEventTimeISO,
    // Debug info
    debug: {
      startTime: startTimeParam,
      totalEvents: todayEvents.length,
      allEventsCount: data.length - 1,
      sampleTimestamp: data.length > 1 ? data[1][0].toString() : 'no data',
      serverTime: new Date().toISOString()
    }
  };
  
  Logger.log('Returning stats: ' + JSON.stringify(stats));
  
  return ContentService.createTextOutput(JSON.stringify(stats))
    .setMimeType(ContentService.MimeType.JSON);
}


function updateEvent(data) {
  try {
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    const rowNumber = data.rowNumber;
    const newTimestamp = data.timestamp;
    const newType = data.type;
    const newNotes = data.notes;
    
    Logger.log('Updating row ' + rowNumber + ' with timestamp: ' + newTimestamp);
    
    // Parse the new timestamp
    let newEventTime = new Date(newTimestamp);
    
    // Manual parsing for "YYYY-MM-DD HH:MM" format if needed
    if (isNaN(newEventTime.getTime())) {
      const match = newTimestamp.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{1,2}):(\d{2})$/);
      if (match) {
        newEventTime = new Date(
          parseInt(match[1]),
          parseInt(match[2]) - 1,
          parseInt(match[3]),
          parseInt(match[4]),
          parseInt(match[5])
        );
      }
    }
    
    if (isNaN(newEventTime.getTime())) {
      return ContentService.createTextOutput(JSON.stringify({
        'status': 'error',
        'message': 'Invalid timestamp format'
      }))
      .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Update the row
    sheet.getRange(rowNumber, 1).setValue(newEventTime);
    sheet.getRange(rowNumber, 1).setNumberFormat('yyyy-MM-dd HH:mm');
    sheet.getRange(rowNumber, 2).setValue(newType);
    sheet.getRange(rowNumber, 3).setValue(newNotes);
    
    Logger.log('Successfully updated row ' + rowNumber);
    
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'success',
      'message': 'Event updated successfully'
    }))
    .setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'error',
      'message': error.toString()
    }))
    .setMimeType(ContentService.MimeType.JSON);
  }
}

function deleteEvents(data) {
  try {
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    const rowNumbers = data.rowNumbers; // Array of row numbers to delete
    
    if (!rowNumbers || rowNumbers.length === 0) {
      return ContentService.createTextOutput(JSON.stringify({
        'status': 'error',
        'message': 'No row numbers provided'
      }))
      .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Sort row numbers in descending order to delete from bottom to top
    // This prevents row number shifting issues
    const sortedRows = rowNumbers.sort((a, b) => b - a);
    
    Logger.log('Deleting rows: ' + sortedRows.join(', '));
    
    for (let i = 0; i < sortedRows.length; i++) {
      const rowNumber = sortedRows[i];
      sheet.deleteRow(rowNumber);
      Logger.log('Deleted row ' + rowNumber);
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'success',
      'message': 'Deleted ' + sortedRows.length + ' event(s) successfully'
    }))
    .setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'error',
      'message': error.toString()
    }))
    .setMimeType(ContentService.MimeType.JSON);
  }
}
