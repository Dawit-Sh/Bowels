# Weekly Wrapped Improvements

## Fixed Issues

### 1. Text Cutoff in "Common" Card
**Problem**: "Smooth" was being cut off and showing as "Smoo th"

**Solution**:
- Reduced font size from 36 to 32
- Added `textAlign: "center"` for better centering
- Added `numberOfLines={2}` to allow wrapping if needed
- Added `adjustsFontSizeToFit` to automatically scale text

### 2. Added Save to Gallery Feature
**New Feature**: Users can now save their Weekly Wrapped image directly to their photo gallery

**Implementation**:
- Added expo-media-library dependency
- Requests permission before saving
- Saves as 1080x1920 PNG (Instagram Story size)
- Shows success/error alerts
- New "Save to Gallery" button with save icon

## Features

### Share to Instagram
- Captures the Weekly Wrapped view as PNG
- 1080x1920 resolution (Instagram Story size)
- Opens share dialog
- Works with Instagram and other apps

### Save to Gallery
- Saves directly to device photo library
- Requests permission first
- Same 1080x1920 resolution
- Shows confirmation when saved
- Accessible from Photos app

## Button Layout

1. **Share to Instagram** (Primary color)
   - Icon: share
   - Opens share dialog
   - Can share to any app

2. **Save to Gallery** (Secondary color)
   - Icon: save-alt
   - Saves to photo library
   - Requires permission

## Permissions

The app now requests:
- **Photo Library Access**: To save Weekly Wrapped images
- Permission message: "Allow Bowels to save your Weekly Wrapped images to your photo library."

## Export Quality

- Format: PNG
- Resolution: 1080x1920 (9:16 aspect ratio)
- Quality: 100% (maximum)
- Perfect for Instagram Stories

## Card Sizing

All cards now have proper sizing:
- **Hero Card**: 220px min height
- **Personality Card**: 32px vertical padding
- **Grid Cards**: 160px min height, equal width
- **Common Label**: 32px font, centered, auto-adjusts
- **Avg Duration**: 48px font, centered

## User Experience

1. User views their Weekly Wrapped
2. Taps "Share to Instagram" to share
3. OR taps "Save to Gallery" to save locally
4. Permission requested (first time only)
5. Image saved/shared successfully
6. Confirmation shown

## Technical Details

- Uses `react-native-view-shot` for capturing
- Uses `expo-sharing` for sharing
- Uses `expo-media-library` for saving
- Captures entire scrollable content
- Maintains aspect ratio and quality
