# Photo-Based Menu Extraction Design

## Problem

The WebView screenshot approach failed because:
- Age verification popups block menu content
- Cross-origin iframes prevent JavaScript interaction
- Site-specific workarounds don't scale

## Solution

Replace URL-based scraping with photo capture. User photographs the menu directly (physical menu board, printed menu, or screen showing website).

## Why This Works

- No age gates - user captures what they already see
- No website-specific logic - works with any format
- Handles both use cases: physical menus at dispensary, screenshots of online menus
- Existing VisionMenuExtractor works unchanged

## User Flow

1. Open app
2. Tap "Take Photo" or "Choose Photo"
3. Capture/select menu image
4. App extracts strains via vision AI
5. App resolves terpene profiles
6. App shows matching results

## Architecture

```
┌─────────────────┐
│   HomeScreen    │
│  [Take Photo]   │
│ [Choose Photo]  │
└────────┬────────┘
         │ image bytes
         ▼
┌─────────────────┐
│  ScanScreen     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│VisionMenuExtractor│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│TerpeneResolver  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ResultsScreen   │
└─────────────────┘
```

## Changes Required

| Component | Current | New |
|-----------|---------|-----|
| HomeScreen | URL field + Scan | Take Photo + Choose Photo buttons |
| ScreenshotCapture | WebView screenshot | Remove entirely |
| ImageCapture | N/A | New: Camera + Gallery picker |
| DefaultMenuParser | Takes URL, captures screenshot | Takes image bytes directly |
| Navigation | Passes URL | Passes image bytes (base64) |

## Platform Implementation

**Android:**
- Camera: CameraX or ACTION_IMAGE_CAPTURE intent
- Gallery: ACTION_PICK intent
- Image processing: Scale down large images before sending to API

**iOS (future):**
- UIImagePickerController for both camera and gallery
