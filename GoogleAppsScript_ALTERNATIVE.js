// Google Apps Script for Baby Needs Counter Backend - ALTERNATIVE VERSION
// Use this version if the standard version doesn't insert rows at the top
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
    
    // LIFO (Last In First Out) - Insert new events at the TOP
    // Alternative method using insertRows instead of insertRowAfter
    sheet.insertRows(2, 1);  // Insert 1 new row at position 2
    
    // Set values in the newly inserted row
    sheet.getRange(2, 1).setValue(data.timestamp || new Date().toLocaleString());
    sheet.getRange(2, 2).setValue(data.type || '');
    sheet.getRange(2, 3).setValue(data.notes || '');
    
    // Return success response
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'success',
      'message': 'Event logged successfully at top'
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
    
    // Get date parameter (optional)
    const dateParam = e.parameter.date;
    
    let todayEvents = [];
    if (dateParam) {
      // Filter events by date (skip header row 1)
      todayEvents = data.slice(1).filter(row => {
        const timestamp = row[0];
        return timestamp.toString().includes(dateParam);
      });
    } else {
      // Return all events if no date specified (skip header row 1)
      todayEvents = data.slice(1);
    }
    
    // Calculate statistics
    // Since rows are now in LIFO order (newest first), the first event is the most recent
    // Types are now emojis for faster visual processing
    const stats = {
      totalEvents: todayEvents.length,
      lastEventTime: todayEvents.length > 0 ? todayEvents[0][0].toString() : '—',
      poopPeeCount: todayEvents.filter(row => row[1] === '💩💧').length,
      peeCount: todayEvents.filter(row => row[1] === '💧').length,
      feedCount: todayEvents.filter(row => row[1] === '🐄').length,
      peeFeedCount: todayEvents.filter(row => row[1] === '💧🐄').length,
      poopFeedCount: todayEvents.filter(row => row[1] === '💩🐄').length
    };
    
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
