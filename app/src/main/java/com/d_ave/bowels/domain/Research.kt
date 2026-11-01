package com.d_ave.bowels.domain

import java.net.URLEncoder

/**
 * A study behind a piece of advice. Every entry was checked against PubMed (NCBI E-utilities)
 * when it was added: title, journal, year, first author and DOI.
 */
data class Paper(
    val authors: String,
    val year: Int,
    val title: String,
    val journal: String,
    /** What it found, in one plain sentence. */
    val finding: String,
    val doi: String? = null,
    val pmid: String,
) {
    val url get() = doi?.let { "https://doi.org/$it" } ?: "https://pubmed.ncbi.nlm.nih.gov/$pmid/"
    val citation get() = "$authors ($year). $journal."
}

object Research {
    val bristolTransit = Paper(
        "Lewis & Heaton", 1997, "Stool form scale as a useful guide to intestinal transit time", "Scand J Gastroenterol",
        "Stool type tracks how fast food moves through the gut: Types 1-2 mean slow transit, 6-7 fast.",
        "10.3109/00365529709011203", "9299672",
    )
    val bristolReliability = Paper(
        "Blake et al.", 2016, "Validity and reliability of the Bristol Stool Form Scale in healthy adults and patients with diarrhoea-predominant IBS", "Aliment Pharmacol Ther",
        "People rate their own stools on the Bristol scale reliably enough to track change over time.",
        "10.1111/apt.13746", "27492648",
    )
    val normalRange = Paper(
        "Mitsuhashi et al.", 2018, "Characterizing normal bowel frequency and consistency in a representative sample of adults in the United States (NHANES)", "Am J Gastroenterol",
        "Most adults go between 3 times a week and 3 times a day, and Types 3-4 are the usual form.",
        "10.1038/ajg.2017.213", "28762379",
    )
    val fiberConstipation = Paper(
        "Yang et al.", 2012, "Effect of dietary fiber on constipation: a meta analysis", "World J Gastroenterol",
        "More dietary fiber raised how often people with constipation went.",
        "10.3748/wjg.v18.i48.7378", "23326148",
    )
    val waterFiber = Paper(
        "Anti et al.", 1998, "Water supplementation enhances the effect of high-fiber diet on stool frequency and laxative consumption in adult patients with functional constipation", "Hepatogastroenterology",
        "Fiber worked better when people also drank 1.5-2 litres of water a day.",
        null, "9684123",
    )
    val prunes = Paper(
        "Attaluri et al.", 2011, "Randomised clinical trial: dried plums (prunes) vs. psyllium for constipation", "Aliment Pharmacol Ther",
        "Prunes (about 50 g twice a day) did as well as or better than psyllium for mild to moderate constipation.",
        "10.1111/j.1365-2036.2011.04594.x", "21323688",
    )
    val kiwi = Paper(
        "Chey et al.", 2021, "Exploratory comparative effectiveness trial of green kiwifruit, psyllium, or prunes in US patients with chronic constipation", "Am J Gastroenterol",
        "Two green kiwifruit a day helped about as much as prunes or psyllium, with less bloating.",
        "10.14309/ajg.0000000000001149", "34074830",
    )
    val fiberIbs = Paper(
        "Moayyedi et al.", 2014, "The effect of fiber supplementation on irritable bowel syndrome: a systematic review and meta-analysis", "Am J Gastroenterol",
        "Soluble fiber such as psyllium eased IBS symptoms; bran (insoluble fiber) did not.",
        "10.1038/ajg.2014.195", "25070054",
    )
    val posture = Paper(
        "Sikirov", 2003, "Comparison of straining during defecation in three positions", "Dig Dis Sci",
        "Squatting took less time and less straining than sitting on a standard toilet.",
        "10.1023/A:1024180319005", "12870773",
    )
    val footstool = Paper(
        "Modi et al.", 2019, "Implementation of a defecation posture modification device: impact on bowel movement patterns in healthy subjects", "J Clin Gastroenterol",
        "A footstool that raises the knees cut straining and time on the toilet.",
        "10.1097/MCG.0000000000001143", "30346317",
    )
    val phoneOnToilet = Paper(
        "Ramprasad et al.", 2025, "Smartphone use on the toilet and the risk of hemorrhoids", "PLoS One",
        "People who used their phone on the toilet sat longer and had a higher risk of hemorrhoids.",
        "10.1371/journal.pone.0329983", "40901789",
    )
    val exercise = Paper(
        "Gao et al.", 2019, "Exercise therapy in patients with constipation: a systematic review and meta-analysis of randomized controlled trials", "Scand J Gastroenterol",
        "Regular exercise improved constipation symptoms in randomised trials.",
        "10.1080/00365521.2019.1568544", "30843436",
    )
    val coffee = Paper(
        "Rao et al.", 1998, "Is coffee a colonic stimulant?", "Eur J Gastroenterol Hepatol",
        "Coffee set off colon activity within minutes in some people, about as strongly as a meal.",
        "10.1097/00042737-199802000-00003", "9581985",
    )
    val stress = Paper(
        "Mayer", 2000, "The neurobiology of stress and gastrointestinal disease", "Gut",
        "Stress acts on the gut through the brain-gut axis, changing motility and sensitivity.",
        "10.1136/gut.47.6.861", "11076888",
    )
    val sleep = Paper(
        "Buchanan et al.", 2014, "Sleep measures predict next-day symptoms in women with irritable bowel syndrome", "J Clin Sleep Med",
        "A worse night's sleep predicted more gut symptoms the next day.",
        "10.5664/jcsm.4038", "25142761",
    )
    val alcohol = Paper(
        "Reding et al.", 2013, "Relationship between patterns of alcohol consumption and gastrointestinal symptoms among patients with IBS", "Am J Gastroenterol",
        "Binge drinking was followed by more diarrhoea and other gut symptoms the next day.",
        "10.1038/ajg.2012.414", "23295280",
    )

    /** PubMed's free search, for "find more studies" (only the topic words leave the phone). */
    fun pubmedSearch(topic: String) = "https://pubmed.ncbi.nlm.nih.gov/?term=" + URLEncoder.encode(topic, "UTF-8")
}
