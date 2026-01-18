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
        'message': 'Event logged successfully at row 2 (first event)'
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
      'message': 'Event logged successfully at row ' + insertPosition + ' (chronologically sorted)'
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
    
    // Find last feeding event (any event containing 🐄)
    const feedEvents = todayEvents.filter(row => {
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
    
    const stats = {
      peeCount: peeCount,
      poopCount: poopCount,
      lastFeedTimeISO: lastFeedTimeISO,
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
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'error',
      'message': error.toString()
    }))
    .setMimeType(ContentService.MimeType.JSON);
  }
}
