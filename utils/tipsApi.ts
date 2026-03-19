const TIPS = [
  "Drink a glass of water first thing in the morning to wake up your digestive system.",
  "Chewing your food thoroughly reduces the workload on your stomach.",
  "A 15-minute walk after meals significantly speeds up gastric emptying.",
  "Aim for at least 25g to 30g of dietary fiber daily for steady gut mobility.",
  "Staying hydrated softens stools, making them easier to pass.",
  "Managing stress levels can reduce irritable bowel flare-ups.",
  "Probiotic-rich foods like yogurt and kimchi help maintain a balanced microbiome.",
  "Avoid heavy meals right before bedtime to prevent acid reflux.",
  "Peppermint or ginger tea can soothe an upset stomach naturally.",
  "Apples contain pectin, a soluble fiber that aids digestion.",
  "Regular exercise increases blood flow to the digestive tract.",
  "Incorporate prebiotics like garlic and onions to feed good gut bacteria.",
  "Eating at consistent times each day regulates your bowel clock.",
  "Excessive caffeine can irritate the digestive lining—monitor your intake.",
  "Elevating your head while sleeping can reduce nocturnal acid reflux.",
  "Don't ignore the urge to go; holding it in can lead to constipation.",
  "Squatting or using a footstool aligns your colon for an easier movement.",
  "Chewing gum can sometimes lead to swallowed air and bloating.",
  "Artichokes are prebiotic powerhouses for a diverse microbiome.",
  "Magnesium-rich foods like spinach help relax intestinal muscles.",
  "Over-the-counter NSAIDs can disrupt gut lining health if overused.",
  "A diverse diet of 30+ plant types a week maximizes gut biodiversity.",
  "Chronic sleep deprivation alters the microbiome negatively.",
  "Warm lemon water in the morning can stimulate bowel contractions.",
  "Fennel seeds are known to reduce gas and bloating post-meal.",
  "Hydration is key out of all factors: water turns fiber into effective bulk.",
  "Oats contain beta-glucan, which acts as a gel, soothing the intestinal tract.",
  "Kefir often contains more diverse probiotic strains than standard yogurt.",
  "Slow down your eating pace. It takes 20 minutes for satiety signals to register.",
  "Bone broth provides gelatin, which may help seal the gut lining.",
  "Avoid wearing tight belts which can compress the stomach and cause reflux.",
  "Deep diaphragmatic breathing activates the vagus nerve, calming digestion.",
  "Kiwi fruit is clinically proven to improve bowel consistency and frequency.",
  "Papaya contains papain, an enzyme that breaks down tough proteins.",
  "Not all fibers are equal: soluble fiber soothes, insoluble fiber scrubs.",
  "Artificial sweeteners like sucralose can alter insulin and gut flora.",
  "Laying on your left side aids digestion by aligning with stomach anatomy.",
  "Sitting at a desk all day? Set a timer to stand and stretch the core.",
  "Dark chocolate with 70%+ cocoa acts as a potent prebiotic.",
  "Flaxseeds provide both omega-3s and mucilage to lubricate the digestive tract.",
  "Your gut produces 90% of your body's serotonin. Happy gut, happy mood.",
  "Slippery elm can safely coat and soothe inflamed digestive tissues.",
  "Chew your food 20-30 times per bite to pre-digest carbohydrates in the mouth.",
  "Avoid excessive alcohol, which is a known neurotoxin to the enteric nervous system.",
  "Aloe vera juice can have a soothing, mild laxative effect.",
  "A sudden increase in fiber without water can paradoxically cause constipation.",
  "Cruciferous veggies (broccoli, cabbage) are great, but cook them if they cause gas.",
  "Processed meats containing nitrites are hard on your gastrointestinal tract.",
  "Eat mindfully. Distracted eating usually leads to swallowing air.",
  "The enteric nervous system has 500 million neurons—trust your 'gut feeling'."
];

export async function fetchDailyTip(): Promise<string> {
  // Simulate network delay to act as a lightweight API
  await new Promise(resolve => setTimeout(resolve, 800));
  
  // Deterministic random tip based on the day of the year
  const dayIndex = Math.floor(Date.now() / (1000 * 60 * 60 * 24));
  
  // The tips map continuously over the year without repeating in a short window
  return TIPS[dayIndex % TIPS.length];
}
