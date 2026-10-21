package com.d_ave.bowels.domain

data class StoolMeta(val short: String, val label: String, val description: String, val emoji: String)

val stoolTypeMeta: Map<Int, StoolMeta> = mapOf(
    1 to StoolMeta("Hard", "Type 1", "Hard pellets, difficult to pass", "🪨"),
    2 to StoolMeta("Lumpy", "Type 2", "Lumpy and compact", "🥔"),
    3 to StoolMeta("Firm", "Type 3", "Sausage-like with surface cracks", "🌭"),
    4 to StoolMeta("Smooth", "Type 4", "Smooth, soft, ideal", "🐍"),
    5 to StoolMeta("Soft", "Type 5", "Soft blobs, easy to pass", "☁️"),
    6 to StoolMeta("Mushy", "Type 6", "Mushy, fluffy pieces", "🌊"),
    7 to StoolMeta("Liquid", "Type 7", "Watery, no solid pieces", "💧"),
)

val quickLogPresets = listOf(1 to "Hard", 3 to "Firm", 4 to "Smooth", 6 to "Mushy")

enum class MilestoneTone { Primary, Secondary, Locked }

data class Milestone(val days: Int, val title: String, val body: String, val tone: MilestoneTone)

val milestones = listOf(
    Milestone(1, "First Rhythm", "The beginning of a conscious path to harmony.", MilestoneTone.Primary),
    Milestone(7, "Steady Flow", "Finding balance through consistency and care.", MilestoneTone.Secondary),
    Milestone(30, "Deep Connection", "Listening to the internal whispers of the body.", MilestoneTone.Locked),
    Milestone(100, "Master of Self", "Complete alignment of mind, habit, and health.", MilestoneTone.Locked),
)

val finalMilestone = Milestone(365, "Eternal Sanctuary", "The ultimate milestone. A year of dedicated wellness and listening to your inner rhythm.", MilestoneTone.Locked)

fun nextMilestone(progressDays: Int) = milestones.firstOrNull { progressDays < it.days } ?: finalMilestone

val healthOptions: List<Pair<String, List<String>>> = listOf(
    "water" to listOf("Low", "Okay", "Great"),
    "fiber" to listOf("Low", "Medium", "High"),
    "meals" to listOf("Light", "Balanced", "Heavy"),
    "stress" to listOf("Low", "Medium", "High"),
    "sleep" to listOf("Poor", "Fair", "Great"),
    "exercise" to listOf("None", "Light", "Active"),
    "caffeine" to listOf("None", "Low", "Medium", "High"),
    "alcohol" to listOf("None", "Light", "Moderate", "Heavy"),
    "medication" to listOf("None", "Prescribed", "OTC", "Both"),
    "mood" to listOf("Happy", "Neutral", "Sad", "Anxious", "Stressed"),
)

data class HealthTip(val id: String, val title: String, val summary: String, val body: String)

val healthInfo = listOf(
    HealthTip("hydration", "Hydration Consistency", "Water intake spread across the day tends to support smoother bowel movements.", "Aim for steady hydration instead of catching up late in the evening. Pair water with meals and fiber-heavy foods. Target 8-10 glasses daily, adjusting for activity level and climate."),
    HealthTip("fiber", "Fiber Balance", "A mix of soluble and insoluble fiber can improve stool consistency and regularity.", "Add fiber gradually, especially if you are increasing legumes, oats, greens, or seeds. Too much too quickly can increase gas. Soluble fiber (oats, apples) softens stool, while insoluble fiber (whole grains, vegetables) adds bulk."),
    HealthTip("routine", "Routine Helps", "Digestive rhythm often improves when meals, sleep, and bathroom timing stay predictable.", "A regular morning routine, especially after breakfast or warm fluids, can reinforce healthy bowel timing. Your gut has its own circadian rhythm that responds to consistency."),
    HealthTip("sleep", "Sleep Quality Matters", "Poor sleep disrupts gut motility and can lead to irregular bowel patterns.", "Aim for 7-9 hours of quality sleep. Your digestive system repairs and regulates itself during sleep. Chronic sleep deprivation can slow digestion and increase inflammation."),
    HealthTip("stress", "Stress Management", "The gut-brain connection means stress directly impacts digestive health.", "High stress can cause urgency, cramping, or constipation. Practice deep breathing, meditation, or gentle exercise. Your enteric nervous system (gut brain) has more neurons than your spinal cord."),
    HealthTip("movement", "Physical Activity", "Regular movement stimulates intestinal contractions and promotes regularity.", "Even 20-30 minutes of walking daily can improve bowel function. Exercise increases blood flow to digestive organs and helps food move through your system more efficiently."),
    HealthTip("probiotics", "Gut Microbiome Health", "A diverse gut microbiome supports digestion, immunity, and mental health.", "Include fermented foods like yogurt, kefir, sauerkraut, or kimchi. Prebiotics (garlic, onions, bananas) feed beneficial bacteria. A healthy microbiome produces vitamins and protects against pathogens."),
    HealthTip("meal-timing", "Meal Timing & Portions", "When and how much you eat affects digestive comfort and regularity.", "Eating at consistent times trains your digestive system. Large meals can cause bloating and discomfort. Consider smaller, more frequent meals if you experience digestive issues."),
    HealthTip("caffeine", "Caffeine Effects", "Caffeine stimulates bowel movements but can cause urgency or loose stools.", "Coffee increases colon activity within 4 minutes of consumption. While helpful for constipation, excessive caffeine can lead to dehydration and diarrhea. Monitor your tolerance."),
    HealthTip("alcohol", "Alcohol Impact", "Alcohol disrupts gut bacteria, increases inflammation, and affects absorption.", "Even moderate drinking can alter stool consistency and frequency. Alcohol speeds up digestion, potentially causing diarrhea, and damages the intestinal lining over time."),
    HealthTip("medications", "Medication Awareness", "Many medications affect digestive function as a side effect.", "Antibiotics kill beneficial gut bacteria. Pain relievers can cause constipation or bleeding. Antacids affect nutrient absorption. Always discuss digestive side effects with your healthcare provider."),
    HealthTip("straining-bp", "Straining and Blood Pressure", "Straining briefly raises blood pressure and changes heart rate.", "Bearing down holds your breath against a closed airway, which pushes blood pressure up and then down within seconds. If your watch shows big heart-rate swings during sessions, more fiber, water and not rushing can help. Talk to a doctor if you feel faint or dizzy on the toilet."),
    HealthTip("red-flags", "When to See a Doctor", "Certain symptoms require professional medical evaluation.", "Seek medical attention for: persistent blood in stool, unexplained weight loss, severe abdominal pain, chronic diarrhea or constipation lasting 2+ weeks, black or tarry stools, or sudden changes in bowel habits after age 50."),
    HealthTip("bristol-scale", "Understanding the Bristol Stool Scale", "Types 3-4 are considered ideal, indicating healthy transit time.", "Type 1-2 suggests constipation (slow transit). Type 6-7 indicates diarrhea (too fast transit). Consistency matters more than frequency. Normal range is 3 times daily to 3 times weekly."),
    HealthTip("food-triggers", "Identifying Food Triggers", "Common trigger foods vary by individual but patterns often emerge.", "Common triggers include: dairy (lactose intolerance), gluten (celiac or sensitivity), FODMAPs (fermentable carbs), spicy foods, artificial sweeteners, and high-fat foods. Keep a food diary to identify your personal triggers."),
    HealthTip("mindful-eating", "Mindful Eating Practices", "How you eat is as important as what you eat for digestive health.", "Chew thoroughly (20-30 times per bite), eat slowly, avoid distractions while eating, and don't eat when stressed. Proper chewing releases digestive enzymes and reduces gas and bloating."),
)
