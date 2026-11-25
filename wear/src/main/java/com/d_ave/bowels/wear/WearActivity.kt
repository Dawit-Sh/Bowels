package com.d_ave.bowels.wear

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.d_ave.bowels.shared.WatchSessionPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.util.UUID

private val stoolTypes = listOf(
    1 to ("🪨" to "Hard"), 2 to ("🥔" to "Lumpy"), 3 to ("🌭" to "Firm"), 4 to ("🐍" to "Smooth"),
    5 to ("☁️" to "Soft"), 6 to ("🌊" to "Mushy"), 7 to ("💧" to "Liquid"),
)

private val Olive = Color(0xFF8A9A5B)
private val OliveDark = Color(0xFF56642B)
private val Cream = Color(0xFFF2F2D9)
private val Coral = Color(0xFFBF5B3F)

private val iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

private sealed interface Step {
    data object Home : Step
    data object Running : Step
    data class Stool(val ended: Ended) : Step
    data class Pain(val ended: Ended, val stool: Int) : Step
    data class Blood(val ended: Ended, val stool: Int, val pain: String) : Step
    data object Saved : Step
}

/** What a finished session carries through the questions. */
private data class Ended(val start: Instant, val end: Instant)

class WearActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Whatever was answered, the session is already running; show it on the watch face if we may.
        WatchSession.running.value?.let { SessionNotice.show(this, it.startTime.toEpochMilli()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchSession.restore(this)
        WatchSession.running.value?.let { SessionNotice.show(this, it.startTime.toEpochMilli()) }
        setContent { BowelsWatchApp(::startSession) }
    }

    /** Starts at once; the notification permission is asked the first time, in the same tap. */
    private fun startSession() {
        val session = WatchSession.start(this)
        val ask = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (ask) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else SessionNotice.show(this, session.startTime.toEpochMilli())
    }
}

@Composable
private fun BowelsWatchApp(onStart: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sync = remember { PhoneSync(context) }
    val scope = rememberCoroutineScope()
    val running by WatchSession.running.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf<Step>(if (WatchSession.running.value != null) Step.Running else Step.Home) }
    var pending by remember { mutableIntStateOf(0) }
    var phoneReady by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = sync.addListener { scope.launch { pending = sync.pendingCount() } }
        scope.launch {
            pending = sync.pendingCount()
            phoneReady = sync.phoneHasApp()
        }
        onDispose { sync.removeListener(listener) }
    }

    fun finish() {
        SessionNotice.hide(context)
        val session = WatchSession.stop(context) ?: return
        step = Step.Stool(Ended(session.startTime, Instant.now()))
    }

    fun save(ended: Ended, answers: Map<String, String>) {
        val payload = WatchSessionPayload(
            id = UUID.randomUUID().toString(),
            kind = "bowel",
            startTime = iso.format(ended.start),
            endTime = iso.format(ended.end),
            durationSeconds = maxOf(1, ended.end.epochSecond - ended.start.epochSecond),
            answers = answers,
        )
        step = Step.Saved
        scope.launch {
            runCatching { sync.send(payload) }
            pending = sync.pendingCount()
        }
    }

    BackHandler(enabled = step !is Step.Home && step !is Step.Running) { step = if (running != null) Step.Running else Step.Home }

    MaterialTheme(colors = Colors(primary = Olive, primaryVariant = OliveDark, secondary = Coral, onPrimary = Color.White)) {
        when (val s = step) {
            Step.Home -> HomeScreen(pending, phoneReady) { onStart(); step = Step.Running }
            Step.Running -> running?.let { RunningScreen(it, onFinish = ::finish, onCancel = {
                SessionNotice.hide(context)
                WatchSession.stop(context)
                step = Step.Home
            }) } ?: LaunchedEffect(Unit) { step = Step.Home }
            is Step.Stool -> Choice("How did it look?", stoolTypes.map { it.second }) { picked ->
                step = Step.Pain(s.ended, stoolTypes.first { it.second.second == picked }.first)
            }
            is Step.Pain -> Choice("Any pain?", listOf("😌" to "None", "🙂" to "Mild", "😣" to "Moderate", "😖" to "Severe")) { picked ->
                step = Step.Blood(s.ended, s.stool, picked.lowercase())
            }
            is Step.Blood -> Choice("Any blood?", listOf("✅" to "No", "🩸" to "Yes")) { picked ->
                save(s.ended, mapOf("stool_type" to s.stool.toString(), "pain" to s.pain, "blood" to (picked == "Yes").toString()))
            }
            Step.Saved -> SavedScreen(pending) { step = Step.Home }
        }
    }
}

@Composable
private fun HomeScreen(pending: Int, phoneReady: Boolean, onStart: () -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(listState) }) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            item { ListHeader { Text("Bowels", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Cream) } }
            item {
                Button(onClick = onStart, modifier = Modifier.size(ButtonDefaults.LargeButtonSize), colors = ButtonDefaults.primaryButtonColors()) {
                    Icon(Icons.Filled.PlayArrow, "Start session", modifier = Modifier.size(32.dp))
                }
            }
            item { Text("Start session", fontSize = 13.sp, color = Cream, modifier = Modifier.padding(top = 6.dp)) }
            item {
                val status = when {
                    pending > 0 && !phoneReady -> "$pending saved · syncs when your phone is near"
                    pending > 0 -> "$pending waiting for your phone"
                    !phoneReady -> "Syncs to Bowels on your phone"
                    else -> "All synced with your phone"
                }
                Text(status, fontSize = 11.sp, color = Cream.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 24.dp, end = 24.dp))
            }
            // Room to scroll the last line off the round edge.
            item { Box(Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun RunningScreen(session: RunningSession, onFinish: () -> Unit, onCancel: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.startTime) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val elapsed = maxOf(0, (now - session.startTime.toEpochMilli()) / 1000)
    Scaffold(timeText = { TimeText() }) {
        Column(Modifier.fillMaxSize().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("%02d:%02d".format(elapsed / 60, elapsed % 60), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Cream)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onCancel, colors = ButtonDefaults.secondaryButtonColors(), modifier = Modifier.size(ButtonDefaults.SmallButtonSize)) {
                    Icon(Icons.Filled.Close, "Discard")
                }
                Button(onClick = onFinish, modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)) {
                    Icon(Icons.Filled.Check, "Finish session")
                }
            }
        }
    }
}

@Composable
private fun Choice(title: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(listState) }) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { ListHeader { Text(title, color = Cream, fontWeight = FontWeight.Bold) } }
            options(options, onPick)
        }
    }
}

private fun ScalingLazyListScope.options(options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    options.forEach { (emoji, label) ->
        item {
            Chip(
                onClick = { onPick(label) },
                label = { Text(label) },
                icon = { Text(emoji, fontSize = 18.sp) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SavedScreen(pending: Int, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onDone()
    }
    Scaffold(timeText = { TimeText() }) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Check, null, tint = Olive, modifier = Modifier.size(36.dp))
            Text("Saved", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Cream)
            Text(if (pending > 0) "Syncs when your phone is near" else "Sent to your phone", fontSize = 11.sp, color = Cream.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}
