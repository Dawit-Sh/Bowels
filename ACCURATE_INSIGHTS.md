# Accurate Data-Driven Insights

## Dynamic Status Messages

The HomeScreen now displays accurate, real-time status messages based on actual user data.

### Status Message Logic

#### No Data State
- **Message**: "Start tracking your bowel health"
- **Subtitle**: "Begin your journey to better digestive health with quick logging and insights."
- **When**: User has 0 sessions

#### Activity-Based Messages

**No activity in 2+ days**
- Triggers when last session was >48 hours ago
- Alerts user to potential constipation or tracking gap

**No logs today yet**
- Shows when today has 0 sessions but last session was <24 hours ago
- Encourages daily tracking

**Your rhythm is steady today**
- Shows when today's count matches 7-day average
- Indicates consistent pattern

**More active than usual today**
- Shows when today's count exceeds 7-day average
- Acknowledges increased activity

**Less active than usual today**
- Shows when today's count is below 7-day average
- Neutral observation, not alarming

**Consistent daily rhythm**
- Shows when 7-day total is 6-8 sessions (near daily)
- Positive reinforcement

### Dynamic Subtitles

Subtitles provide context based on tracking consistency:

**Excellent tracking (90%+ consistency)**
- "X sessions this week. Excellent tracking consistency!"
- Celebrates strong habits

**Good tracking (70-89% consistency)**
- "X sessions this week. Good tracking habits."
- Positive reinforcement

**Building habits (50-69% consistency)**
- "X sessions this week. Keep building the habit."
- Encouraging tone

**Low tracking (<50% consistency)**
- "X session(s) this week. Track daily for better insights."
- Gentle nudge to improve

## Insight Card Accuracy

### Data Sources

All insights are generated from:
1. **Session data**: Actual bowel movement records
2. **Answer data**: Stool type, pain, urgency, blood, tags
3. **Daily health data**: Sleep, fiber, water, stress, caffeine, alcohol, mood
4. **Time-based analysis**: Gaps, patterns, frequencies

### Insight Categories

#### High Severity (Red Border)
- No bowel log in 3+ days
- Pain pattern detected (2+ moderate/severe in last 5)
- Blood detected in recent sessions

#### Warning Severity (Orange Border)
- Low fiber pattern (Type 1-2 stools)
- Hydration issues (Type 6-7 stools)
- Frequent high urgency (3+ in last 7)
- Frequent straining (3+ in last 7)
- Poor sleep pattern (4+ poor nights in last 7)

#### Info Severity (Blue Border)
- Long sessions recurring (2+ sessions >12 min)
- Recurring bloating (3+ in last 7)
- Caffeine correlation with loose stools
- Stress correlation with urgency
- Alcohol correlation with irregularity
- Negative mood patterns (4+ days in last 7)
- Default positive: "Rhythm looks steady"

### Correlation Detection

The system detects correlations between:

**Caffeine ↔ Loose Stools**
- Requires: 3+ high caffeine days AND 2+ loose stools (Type 6-7)
- Insight: Suggests reducing caffeine

**Stress ↔ Urgency**
- Requires: 3+ high stress days AND 2+ high urgency sessions
- Insight: Recommends stress management

**Sleep ↔ Digestive Health**
- Requires: 4+ poor sleep nights
- Insight: Encourages better sleep habits

**Alcohol ↔ Irregularity**
- Requires: 2+ alcohol days AND 2+ irregular stools (Type ≤2 or ≥6)
- Insight: Suggests moderating alcohol

**Mood ↔ Wellness**
- Requires: 4+ negative mood days
- Insight: Recommends stress management or professional help

### Accuracy Guarantees

✅ **All data is sourced from user's actual records**
- No hardcoded assumptions
- No fake patterns
- No placeholder data

✅ **Thresholds are evidence-based**
- 3-day gap = medically significant
- Type 1-2 = constipation indicators
- Type 6-7 = diarrhea indicators
- 12+ minutes = prolonged session

✅ **Correlations require minimum evidence**
- Multiple occurrences needed (2-4 depending on severity)
- Recent data weighted more (last 5-7 sessions/days)
- Patterns must be statistically meaningful

✅ **Messages are actionable**
- Each insight includes specific guidance
- Severity levels help prioritize actions
- Positive reinforcement when patterns are healthy

## Example Scenarios

### Scenario 1: New User
- Status: "Start tracking your bowel health"
- Subtitle: "Begin your journey..."
- Insight: None (no data yet)

### Scenario 2: Consistent Tracker
- Status: "Your rhythm is steady today"
- Subtitle: "7 sessions this week. Excellent tracking consistency!"
- Insight: "Rhythm looks steady" (no issues detected)

### Scenario 3: Constipation Pattern
- Status: "Less active than usual today"
- Subtitle: "3 sessions this week. Keep building the habit."
- Insight: "Pattern suggests low fiber" (Type 1-2 detected)

### Scenario 4: Stress Impact
- Status: "More active than usual today"
- Subtitle: "9 sessions this week. Excellent tracking consistency!"
- Insight: "Stress may be impacting digestion" (high stress + urgency correlation)

## Benefits

1. **User Trust**: Accurate data builds confidence in the app
2. **Actionable**: Users know exactly what to do
3. **Personalized**: Every message is unique to the user
4. **Educational**: Users learn about their patterns
5. **Motivating**: Positive reinforcement for good habits
