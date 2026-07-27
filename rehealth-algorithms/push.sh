#!/bin/bash
# GitHub Push Script
# Usage: ./push.sh YOUR_GITHUB_TOKEN

if [ -z "$1" ]; then
    echo "Usage: ./push.sh YOUR_GITHUB_TOKEN"
    echo ""
    echo "To create a token:"
    echo "1. Go to https://github.com/settings/tokens"
    echo "2. Click 'Generate new token (classic)'"
    echo "3. Select 'repo' scope"
    echo "4. Copy the token"
    exit 1
fi

TOKEN=$1
REPO="csong8904-spec/ReHealthAI-HealthAgent"

echo "Setting remote URL with token..."
git remote set-url origin https://${TOKEN}@github.com/${REPO}.git

echo "Pushing to GitHub..."
git push origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Successfully pushed to GitHub!"
    echo "Repository: https://github.com/${REPO}"
else
    echo ""
    echo "❌ Push failed. Please check your token and try again."
fi
