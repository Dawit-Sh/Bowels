package com.d_ave.bowels.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.data.DailyHealthInput
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.domain.Research
import com.d_ave.bowels.domain.SessionDetail
import com.d_ave.bowels.domain.healthLabel
import com.d_ave.bowels.domain.healthOptions
import com.d_ave.bowels.domain.stoolTypeMeta
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.ui.components.BCard
import com.d_ave.bowels.ui.components.BText
import com.d_ave.bowels.ui.components.PaperLink
import com.d_ave.bowels.ui.components.ScreenColumn
import com.d_ave.bowels.ui.components.SectionTitle
import com.d_ave.bowels.ui.theme.LocalPalette
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())

/** One session from History: what was logged, and how it compares with your usual. */
@Composable
fun SessionDetailScreen(detail: SessionDetail) {
    val p = LocalPalette.current
    val s = detail.session
    val bowel = s.session.kind == SessionKind.Bowel
    ScreenColumn {
        SectionTitle(if (bowel) "Bowel session" else "Urine session", dayFormat.format(s.session.startTime))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BText(detail.ordinal, p.onSurfaceVariant, 13.sp, FontWeight.Bold)
            if (s.answers[VitalsKeys.WATCH_ID] != null) {
                Icon(Icons.Filled.Watch, null, tint = p.outline, modifier = Modifier.size(14.dp))
                BText("Logged on your watch", p.onSurfaceVariant, 13.sp)
            }
        }

        val type = s.stoolType
        if (bowel && type != null) {
            val meta = stoolTypeMeta.getValue(type)
            BCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BText(meta.emoji, p.onSurface, 44.sp)
                    Column {
                        BText("Type $type · ${meta.short}", p.primary, 22.sp, FontWeight.ExtraBold)
                        BText(meta.description, p.onSurfaceVariant, 14.sp)
                    }
                }
                detail.stoolMeaning?.let { BText(it, p.onSurface, 14.sp, lineHeight = 20.sp) }
                PaperLink(Research.bristolTransit)
            }
        }

        BCard(Modifier.fillMaxWidth()) {
            BText("Against your usual", p.onSurface, 20.sp, FontWeight.Bold)
            detail.comparisons.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider(color = p.outline.copy(alpha = 0.13f))
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BText(c.label.uppercase(), p.onSurfaceVariant, 10.sp, FontWeight.Bold, letterSpacing = 1.sp)
                        if (c.note.isNotEmpty()) BText(c.note, p.onSurface, 14.sp, lineHeight = 19.sp)
                    }
                    BText(c.value, p.primary, 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }

        val answers = answerRows(s.answers, bowel)
        if (answers.isNotEmpty()) {
            BCard(Modifier.fillMaxWidth()) {
                BText("What you logged", p.onSurface, 20.sp, FontWeight.Bold)
                answers.forEach { (label, value) -> KeyValue(label, value) }
            }
        }

        val v = s.vitals
        if (!v.isEmpty) {
            BCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Favorite, null, tint = p.secondary)
                    BText("Heart & blood pressure", p.onSurface, 20.sp, FontWeight.Bold)
                }
                v.heartRateAvg?.let { KeyValue("Heart rate", "$it bpm ${sourceLabel(v.heartRateSource)}".trim()) }
                if (v.heartRateMin != null && v.heartRateMax != null) KeyValue("Range", "${v.heartRateMin}-${v.heartRateMax} bpm")
                if (v.hasBloodPressure) KeyValue("Blood pressure", "${v.systolic}/${v.diastolic} mmHg ${sourceLabel(v.bloodPressureSource)}".trim())
            }
        }

        if (detail.links.isNotEmpty()) {
            BCard(Modifier.fillMaxWidth()) {
                BText("What may have played a part", p.onSurface, 20.sp, FontWeight.Bold)
                detail.links.forEach { BText(it, p.onSurface, 14.sp, lineHeight = 20.sp) }
                BText("From your own history. A link isn't proof of cause, but it's worth noticing.", p.onSurfaceVariant, 12.sp, lineHeight = 17.sp)
            }
        }

        HealthDay("That day's health check", detail.dayHealth)
        HealthDay("The day before", detail.dayBeforeHealth)
    }
}

@Composable
private fun HealthDay(title: String, health: DailyHealthInput?) {
    val p = LocalPalette.current
    BCard(Modifier.fillMaxWidth()) {
        BText(title, p.onSurface, 20.sp, FontWeight.Bold)
        if (health == null) {
            BText("Not filled in.", p.onSurfaceVariant, 14.sp)
        } else {
            healthOptions.forEach { (field, _) -> KeyValue(healthLabel(field), health[field]) }
        }
    }
}

@Composable
private fun KeyValue(label: String, value: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 9.dp)) {
        BText(label, p.onSurfaceVariant, 13.sp, FontWeight.Medium, modifier = Modifier.weight(1f))
        BText(value, p.onSurface, 13.sp, FontWeight.Bold)
    }
}

private fun answerRows(answers: Map<String, String>, bowel: Boolean): List<Pair<String, String>> = buildList {
    fun cap(s: String) = s.replaceFirstChar { it.uppercase() }
    if (bowel) {
        answers["pain"]?.let { add("Pain" to cap(it)) }
        answers["blood"]?.let { add("Blood" to if (it == "true") "Yes" else "No") }
        answers["urgency"]?.let { add("Urgency" to "$it of 5") }
        answers["completion"]?.let { add("Completion" to cap(it)) }
        answers["adaptive_tags"]?.takeIf { it.isNotBlank() }?.let { add("Also" to it.split(",").joinToString(", ") { t -> cap(t.trim()) }) }
    } else {
        answers["urine_urgency"]?.let { add("Urgency" to cap(it)) }
        answers["urine_color"]?.let { add("Color" to cap(it)) }
    }
}
