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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.data.SessionDraft
import com.d_ave.bowels.domain.stoolTypeMeta
import com.d_ave.bowels.ui.components.BCard
import com.d_ave.bowels.ui.components.BText
import com.d_ave.bowels.ui.components.ChipRow
import com.d_ave.bowels.ui.components.FieldLabel
import com.d_ave.bowels.ui.components.GradientButton
import com.d_ave.bowels.ui.components.ScreenColumn
import com.d_ave.bowels.ui.components.SectionTitle
import com.d_ave.bowels.ui.theme.LocalPalette
import kotlinx.coroutines.delay

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
                            .clickable { onUpdate { it.copy(stoolType = type) } }
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
    val save: () -> Unit,
)

/** After a visit: the stool type, pain and blood. Everything else about the day is logged in your dashboard app. */
@Composable
fun QuestionsScreen(draft: SessionDraft, actions: QuestionActions) {
    ScreenColumn {
        SectionTitle("Session complete", "Duration ${draft.durationSeconds / 60}:${"%02d".format(draft.durationSeconds % 60)}")

        BCard(Modifier.fillMaxWidth()) {
            FieldLabel("Stool Type")
            ChipRow((1..7).toList(), { draft.stoolType == it }, { stoolTypeMeta.getValue(it).let { m -> "${m.emoji} ${m.short}" } }) { v -> actions.update { it.copy(stoolType = v) } }
            FieldLabel("Pain")
            ChipRow(listOf("none", "mild", "moderate", "severe"), { draft.pain == it }) { v -> actions.update { it.copy(pain = v) } }
            FieldLabel("Blood")
            ChipRow(listOf("No", "Yes"), { draft.blood == (it == "Yes") }) { v -> actions.update { it.copy(blood = v == "Yes") } }
        }

        GradientButton("Save to Journal", Icons.Filled.Save, onClick = actions.save)
    }
}
