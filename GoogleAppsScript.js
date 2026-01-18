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
    
    // Append the data as a new row
    sheet.appendRow([
      data.timestamp || new Date().toLocaleString(),
      data.type || '',
      data.notes || ''
    ]);
    
    // Return success response
    return ContentService.createTextOutput(JSON.stringify({
      'status': 'success',
      'message': 'Event logged successfully'
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
      // Filter events by date
      todayEvents = data.slice(1).filter(row => {
        const timestamp = row[0];
        return timestamp.toString().includes(dateParam);
      });
    } else {
      // Return all events if no date specified
      todayEvents = data.slice(1);
    }
    
    // Calculate statistics
    const stats = {
      totalEvents: todayEvents.length,
      lastEventTime: todayEvents.length > 0 ? todayEvents[todayEvents.length - 1][0] : '—',
      poopPeeCount: todayEvents.filter(row => row[1] === 'poop_pee').length,
      peeCount: todayEvents.filter(row => row[1] === 'pee').length,
      feedCount: todayEvents.filter(row => row[1] === 'feed').length
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
