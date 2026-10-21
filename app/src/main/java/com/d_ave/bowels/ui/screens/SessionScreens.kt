package com.d_ave.bowels.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.data.DailyHealthInput
import com.d_ave.bowels.data.SessionDraft
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.Vitals
import com.d_ave.bowels.domain.stoolTypeMeta
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.ui.Connections
import com.d_ave.bowels.ui.components.BCard
import com.d_ave.bowels.ui.components.BText
import com.d_ave.bowels.ui.components.ChipRow
import com.d_ave.bowels.ui.components.FieldLabel
import com.d_ave.bowels.ui.components.GradientButton
import com.d_ave.bowels.ui.components.ScreenColumn
import com.d_ave.bowels.ui.components.SectionTitle
import com.d_ave.bowels.ui.theme.LocalPalette
import com.d_ave.bowels.ui.theme.Manrope
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatTimer(seconds: Long) = "%02d:%02d".format(seconds / 60, seconds % 60)

@Composable
fun ActiveSessionScreen(draft: SessionDraft, onUpdate: ((SessionDraft) -> SessionDraft) -> Unit, onStart: () -> Unit, onFinish: () -> Unit, onCancel: () -> Unit) {
    val p = LocalPalette.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(draft.startTime) {
        while (draft.startTime != null) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val elapsed = draft.startTime?.let { maxOf(0, (now - it.toEpochMilli()) / 1000) } ?: draft.durationSeconds

    ScreenColumn(gap = 24.dp) {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BText("ACTIVE SESSION", p.onSurfaceVariant, 11.sp, FontWeight.Bold, letterSpacing = 2.sp)
            BText(formatTimer(elapsed), p.primary, 68.sp, FontWeight.ExtraBold, letterSpacing = (-2).sp)
            BText(if (draft.startTime != null) "Finding your rhythm..." else "Ready when you are.", p.onSurfaceVariant)
        }
        Column(Modifier.padding(horizontal = 22.dp)) {
            BText("How does it feel", p.onSurface, 28.sp, FontWeight.ExtraBold, lineHeight = 32.sp)
            BText("right now?", p.tertiary, 28.sp, FontWeight.ExtraBold, lineHeight = 32.sp)
        }
        (1..7).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { type ->
                    val meta = stoolTypeMeta.getValue(type)
                    val active = draft.stoolType == type
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (active) p.surfaceContainerLowest else p.surfaceContainerHigh)
                            .border(BorderStroke(2.dp, if (active) p.primary.copy(alpha = 0.33f) else Color.Transparent), RoundedCornerShape(24.dp))
                            .clickable { onUpdate { it.copy(kind = SessionKind.Bowel, stoolType = type) } }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BText(meta.emoji, p.onSurface, 28.sp)
                        BText(meta.short, if (active) p.primary else p.onSurface, 16.sp, FontWeight.Bold)
                        BText(meta.description, p.onSurfaceVariant, 12.sp, lineHeight = 17.sp)
                    }
                }
                if (row.size == 1) Column(Modifier.weight(1f)) {}
            }
        }
        BCard(Modifier.fillMaxWidth()) {
            BText("This uses plain-language Bristol descriptions so you don't need to memorize numbered stool types.", p.onSurfaceVariant, lineHeight = 20.sp)
        }
        if (draft.startTime != null) {
            GradientButton("Finish Session", Icons.Filled.CheckCircle, onClick = onFinish)
            BText("Cancel and discard", p.onSurfaceVariant, 14.sp, FontWeight.Bold, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable(onClick = onCancel).padding(8.dp))
        } else {
            GradientButton("Start Session", Icons.Filled.PlayArrow, onClick = onStart)
        }
    }
}

class QuestionActions(
    val update: ((SessionDraft) -> SessionDraft) -> Unit,
    val setKind: (SessionKind) -> Unit,
    val setManualVitals: (Int?, Int?, Int?) -> Unit,
    val connectHealth: () -> Unit,
    val save: (DailyHealthInput?) -> Unit,
)

@Composable
fun QuestionsScreen(draft: SessionDraft, health: DailyHealthInput, filledToday: Boolean, connections: Connections, actions: QuestionActions) {
    val p = LocalPalette.current
    var localHealth by remember { mutableStateOf(health) }
    var askHealth by remember { mutableStateOf(false) }

    ScreenColumn {
        SectionTitle("Session complete", "Duration ${draft.durationSeconds / 60}:${"%02d".format(draft.durationSeconds % 60)}")

        BCard(Modifier.fillMaxWidth()) {
            FieldLabel("Type")
            ChipRow(SessionKind.entries, { draft.kind == it }, { it.key }) { actions.setKind(it) }
        }

        if (draft.kind == SessionKind.Urine) {
            BCard(Modifier.fillMaxWidth()) {
                FieldLabel("Urgency")
                ChipRow(listOf("low", "medium", "high"), { draft.urineUrgency == it }) { v -> actions.update { it.copy(urineUrgency = v) } }
                FieldLabel("Color")
                ChipRow(listOf("light", "normal", "dark"), { draft.urineColor == it }) { v -> actions.update { it.copy(urineColor = v) } }
            }
        } else {
            BCard(Modifier.fillMaxWidth()) {
                FieldLabel("Stool Type")
                ChipRow((1..7).toList(), { draft.stoolType == it }, { stoolTypeMeta.getValue(it).let { m -> "${m.emoji} ${m.short}" } }) { v -> actions.update { it.copy(stoolType = v) } }
                FieldLabel("Pain")
                ChipRow(listOf("none", "mild", "moderate", "severe"), { draft.pain == it }) { v -> actions.update { it.copy(pain = v) } }
                FieldLabel("Blood")
                ChipRow(listOf("No", "Yes"), { draft.blood == (it == "Yes") }) { v -> actions.update { it.copy(blood = v == "Yes") } }
                FieldLabel("Urgency")
                ChipRow((1..5).toList(), { draft.urgency == it }) { v -> actions.update { it.copy(urgency = v) } }
                FieldLabel("Completion")
                ChipRow(listOf("incomplete", "normal", "urgent"), { draft.completion == it }) { v -> actions.update { it.copy(completion = v) } }
                FieldLabel("Adaptive tags")
                ChipRow(listOf("bloating", "straining", "gas"), { it in draft.adaptiveTags }) { tag ->
                    actions.update { d -> d.copy(adaptiveTags = if (tag in d.adaptiveTags) d.adaptiveTags - tag else d.adaptiveTags + tag) }
                }
            }
        }

        VitalsCard(draft.vitals, connections, actions)

        BCard(Modifier.fillMaxWidth()) {
            BText("Quick Health Check", p.primary, 18.sp, FontWeight.Bold)
            BText(if (filledToday) "Already filled today ✓" else "Help us provide better insights", p.onSurfaceVariant, 13.sp)
            FieldLabel("Last Night's Sleep")
            ChipRow(listOf("Poor", "Fair", "Great"), { localHealth.sleep == it }) { localHealth = localHealth.copy(sleep = it) }
            FieldLabel("Today's Fiber Intake")
            ChipRow(listOf("Low", "Medium", "High"), { localHealth.fiber == it }) { localHealth = localHealth.copy(fiber = it) }
            FieldLabel("Water Intake Today")
            ChipRow(listOf("Low", "Okay", "Great"), { localHealth.water == it }) { localHealth = localHealth.copy(water = it) }
        }

        GradientButton("Save to Journal", Icons.Filled.Save) {
            if (filledToday) actions.save(if (localHealth != health) localHealth else null) else askHealth = true
        }
    }

    if (askHealth) {
        AlertDialog(
            onDismissRequest = { askHealth = false },
            title = { BText("Daily Health Check", p.onSurface, 20.sp, FontWeight.Bold) },
            text = { BText("Have you updated your daily health check (sleep, fiber, water)?", p.onSurfaceVariant) },
            confirmButton = { TextButton(onClick = { askHealth = false; actions.save(localHealth) }) { BText("Yes, Save All", p.primary, weight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { askHealth = false; actions.save(null) }) { BText("Skip for Now", p.onSurfaceVariant, weight = FontWeight.Bold) } },
        )
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())

fun sourceLabel(source: String?) = when (source) {
    VitalsKeys.SOURCE_WATCH -> "from your watch"
    VitalsKeys.SOURCE_HEALTH_CONNECT -> "from Health Connect"
    VitalsKeys.SOURCE_MANUAL -> "entered by you"
    else -> ""
}

@Composable
private fun VitalsCard(vitals: Vitals, connections: Connections, actions: QuestionActions) {
    val p = LocalPalette.current
    var hr by rememberSaveable { mutableStateOf(vitals.heartRateAvg?.toString().orEmpty()) }
    var sys by rememberSaveable { mutableStateOf(vitals.systolic?.toString().orEmpty()) }
    var dia by rememberSaveable { mutableStateOf(vitals.diastolic?.toString().orEmpty()) }

    // Readings fetched from Health Connect fill the boxes, unless you already typed something.
    LaunchedEffect(vitals.heartRateAvg, vitals.systolic, vitals.diastolic) {
        if (hr.isEmpty()) vitals.heartRateAvg?.let { hr = it.toString() }
        if (sys.isEmpty() && dia.isEmpty() && vitals.hasBloodPressure) {
            sys = vitals.systolic.toString()
            dia = vitals.diastolic.toString()
        }
    }
    fun push() = actions.setManualVitals(hr.toIntOrNull()?.takeIf { it in 25..250 }, sys.toIntOrNull()?.takeIf { it in 60..260 }, dia.toIntOrNull()?.takeIf { it in 30..180 })

    BCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Favorite, null, tint = p.secondary)
            BText("Heart & blood pressure", p.primary, 18.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
            if (connections.fetchingVitals) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = p.primary)
        }
        val note = buildList {
            if (vitals.hasHeartRate && vitals.heartRateSource != VitalsKeys.SOURCE_MANUAL) {
                add("Heart rate ${vitals.heartRateAvg} bpm ${sourceLabel(vitals.heartRateSource)}" + (vitals.heartRateMax?.let { " (peak $it)" } ?: ""))
            }
            if (vitals.hasBloodPressure && vitals.bloodPressureSource != VitalsKeys.SOURCE_MANUAL) {
                add("Blood pressure ${vitals.systolic}/${vitals.diastolic} ${sourceLabel(vitals.bloodPressureSource)}" + (vitals.bloodPressureTime?.let { " at ${timeFormat.format(it)}" } ?: ""))
            }
        }
        BText(
            when {
                note.isNotEmpty() -> note.joinToString("\n")
                connections.healthConnectGranted -> "Nothing recorded around this session yet. Type a reading in if you took one."
                else -> "Optional. Type a reading in, or connect Health Connect so your watch or cuff fills this in."
            },
            p.onSurfaceVariant, 13.sp, lineHeight = 19.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField("Heart rate", hr, "bpm", Modifier.weight(1f)) { hr = it; push() }
            NumberField("Systolic", sys, "mmHg", Modifier.weight(1f)) { sys = it; push() }
            NumberField("Diastolic", dia, "mmHg", Modifier.weight(1f)) { dia = it; push() }
        }
        if (!connections.healthConnectGranted && connections.healthConnect != com.d_ave.bowels.health.HealthConnectManager.Status.Unsupported) {
            BText("Connect Health Connect", p.primary, 14.sp, FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = actions.connectHealth).padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, unit: String, modifier: Modifier, onChange: (String) -> Unit) {
    val p = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
        modifier = modifier,
        label = { BText(label, p.onSurfaceVariant, 11.sp, maxLines = 1) },
        suffix = { BText(unit, p.outline, 10.sp) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = p.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.primary, unfocusedBorderColor = p.outline.copy(alpha = 0.4f), cursorColor = p.primary),
    )
}

