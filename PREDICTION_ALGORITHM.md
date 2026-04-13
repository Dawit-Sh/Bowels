# Improved Prediction Algorithm

## Overview
The enhanced prediction algorithm uses multiple factors to accurately predict the next bowel movement time.

## Key Improvements

### 1. Multi-Factor Analysis
- **Interval Analysis**: Calculates time between consecutive sessions
- **Day-of-Week Patterns**: Detects if certain days have consistent timing
- **Time-of-Day Preferences**: Identifies morning/afternoon/evening patterns
- **Consistency Scoring**: Measures how predictable the pattern is

### 2. Weighted Calculations
- Recent sessions have more influence (exponential decay: 0.85)
- Reduces impact of outliers from weeks ago
- Adapts quickly to changing patterns

### 3. Day-of-Week Intelligence
- Tracks timing for each day of the week separately
- Uses day-specific averages when pattern is strong (≥2 occurrences)
- Calculates consistency score per day
- Falls back to overall pattern if day data is weak

### 4. Time-of-Day Bucketing
- Morning: 5 AM - 12 PM
- Afternoon: 12 PM - 5 PM
- Evening: 5 PM - 10 PM
- Night: 10 PM - 5 AM

Identifies which time period is most common and uses that for predictions.

### 5. Consistency Scoring
- Calculates variance in intervals
- Score of 0.8+ = High confidence (🎯)
- Score of 0.6-0.8 = Medium confidence (📊)
- Score below 0.6 = Low confidence (🔮)

### 6. Smart Time Adjustment
- If day-of-week pattern exists with >70% consistency, uses day-specific time
- Otherwise, uses preferred time-of-day average
- Automatically moves prediction forward if it's in the past

## Algorithm Flow

```
1. Collect last 21 bowel sessions (or all if fewer)
2. Calculate intervals between consecutive sessions
3. Compute weighted average interval (recent = more weight)
4. Analyze day-of-week patterns and consistency
5. Analyze time-of-day preferences
6. Calculate overall consistency score
7. Predict next time based on weighted interval
8. Adjust time based on:
   - Day-specific pattern (if strong)
   - OR time-of-day preference
9. Move forward if prediction is in past
10. Add confidence emoji based on consistency
```

## Confidence Indicators

- 🎯 **High Confidence** (80%+): Very consistent pattern, reliable prediction
- 📊 **Medium Confidence** (60-80%): Moderate pattern, good estimate
- 🔮 **Low Confidence** (<60%): Variable pattern, rough estimate

## Example Predictions

- `🎯 Mon 7:30 AM` - High confidence, Monday morning pattern
- `📊 Wed 2:15 PM` - Medium confidence, Wednesday afternoon
- `🔮 Fri 8:00 PM` - Low confidence, Friday evening estimate

## Minimum Data Requirements

- **3+ sessions**: Full prediction with confidence scoring
- **2 sessions**: Shows "Building pattern..."
- **0-1 sessions**: Shows "Need more data"

## Advantages Over Previous Algorithm

1. **Better accuracy**: Uses up to 21 sessions vs 14
2. **Day-specific timing**: Recognizes different patterns per day
3. **Confidence scoring**: User knows how reliable the prediction is
4. **Time-of-day awareness**: Understands morning vs evening preferences
5. **Adaptive weighting**: Recent data matters more
6. **Consistency detection**: Identifies when patterns are strong
7. **Smart fallbacks**: Uses best available data when patterns are weak
