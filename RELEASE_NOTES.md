# Bowels Release 1.1.0

We're thrilled to announce a massive v1.1.0 update to Bowels, packed with long-term progression metrics, new insights, intelligent forecasting, and structural refinements!

## What's New 🚀

### 1. The Predictions Engine 🔮
- **Circadian Gut Forecasting:** A brand new **Predict** tab automatically models your bodily rhythm based on historical tracking and estimates your most probable upcoming session window, guiding you on maintaining physiological alignment.

### 2. Gamified Tracking 🏅
- **Multi-Tiered Badges (1 Year):** Tracking consistency now pays off. Discover deeply integrated local badges unlocking exclusively at major milestones—ranging from the **"First Drop"** (Day 1), up to **"Equilibrium"** (Day 270), and all the way to absolute **"365 Mastery"** for maintaining a full year of tracking! Look for the stunning new Tab layout dedicated to your achievements.

### 3. Medical Strategies & Wisdom 💡
- **Dynamic Tips:** Our updated engine now contains over **50 unique daily strategies**, factoids, and deep medical insights regarding your gut flora, hydration tactics, and physiological processes. They reliably cycle sequentially day-by-day directly onto your Home Screen.

### 4. Wrapped: The Grand Overview 📊
- **Welcome to the Yearly Wrapped!** Joining the original Weekly Wrapped is an intense macroscopic view aggregating *all* statistics, visit densities, and timings across the entire current 365-day year window. 

### 5. Seamless Native Updates 📲
- **Auto-Updater Check:** Inside the Settings page under Developer Tools, we've configured a robust native hook connecting directly to the central GitHub repository. On compatible Android setups, this enables seamless, in-app APK downloading and instant installations.

## Bug Fixes & Refinements 🔧
- **Visual Stool Guidance**: Revamped the `HealthInfo` Medical screen offering dynamic color coding and specific ideal states (Types 3 & 4) across the Bristol scale natively.
- **Expo Navigation Tearing**: Mitigated a white flash artifact consistently experienced by users navigating the router stack under dark-mode constraints by locking the root React Native `SystemUI` bridge.
- **Background Processes**: Rewrote headless background triggers optimizing background notifications cancellation mechanisms.
