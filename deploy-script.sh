#!/bin/bash

# Deploy Google Apps Script
# Usage: ./deploy-script.sh

# Use full path to clasp in case it's not in PATH
CLASP="/Users/avihu.marco/.nvm/versions/node/v22.21.0/bin/clasp"

echo "📤 Pushing changes to Google Apps Script..."
$CLASP push

if [ $? -eq 0 ]; then
    echo "✅ Push successful!"
    echo "🚀 Creating new deployment..."
    $CLASP deploy --description "Auto-deploy $(date '+%Y-%m-%d %H:%M')"
    
    if [ $? -eq 0 ]; then
        echo "✅ Deployment successful!"
    else
        echo "❌ Deployment failed"
        exit 1
    fi
else
    echo "❌ Push failed"
    exit 1
fi
