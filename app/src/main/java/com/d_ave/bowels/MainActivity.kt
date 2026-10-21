package com.d_ave.bowels

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.d_ave.bowels.data.Screen
import com.d_ave.bowels.data.ThemeMode
import com.d_ave.bowels.domain.Milestone
import com.d_ave.bowels.health.HealthConnectManager
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.share.ShareCards
import com.d_ave.bowels.ui.AppViewModel
import com.d_ave.bowels.ui.UiState
import com.d_ave.bowels.ui.components.BText
import com.d_ave.bowels.ui.components.BottomNav
import com.d_ave.bowels.ui.components.ScreenHeader
import com.d_ave.bowels.ui.screens.ActiveSessionScreen
import com.d_ave.bowels.ui.screens.AnalyticsScreen
import com.d_ave.bowels.ui.screens.BadgesScreen
import com.d_ave.bowels.ui.screens.HealthInfoScreen
import com.d_ave.bowels.ui.screens.HistoryScreen
import com.d_ave.bowels.ui.screens.HomeActions
import com.d_ave.bowels.ui.screens.HomeScreen
import com.d_ave.bowels.ui.screens.OnboardingScreen
import com.d_ave.bowels.ui.screens.QuestionActions
import com.d_ave.bowels.ui.screens.QuestionsScreen
import com.d_ave.bowels.ui.screens.SettingsActions
import com.d_ave.bowels.ui.screens.SettingsScreen
import com.d_ave.bowels.ui.screens.WeeklyWrappedScreen
import com.d_ave.bowels.ui.theme.BowelsTheme
import com.d_ave.bowels.ui.theme.LocalPalette
import com.d_ave.bowels.ui.theme.Palette
import com.d_ave.bowels.ui.theme.buildPalette
import com.d_ave.bowels.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private val app get() = application as BowelsApp
    private val shareCards by lazy { ShareCards(this) }

    private var afterNotificationPermission: (() -> Unit)? = null
    private var afterHealthPermission: (() -> Unit)? = null
    private var dialog by mutableStateOf<Pair<String, String>?>(null)
    private var updateOffer by mutableStateOf<UpdateChecker.Release?>(null)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        afterNotificationPermission?.invoke()
        afterNotificationPermission = null
    }

    private val healthPermission = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        app.prefs.healthConnectAsked = true
        viewModel.refreshConnections()
        if (viewModel.state.value.screen == Screen.Questions) viewModel.fetchDraftVitals()
        afterHealthPermission?.invoke()
        afterHealthPermission = null
    }

    private val legacyStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveWeekly()
    }

    private val importDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::importFrom) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.openDestination(intent?.getStringExtra(Notifications.EXTRA_DESTINATION))

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dark = when (state.settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
            }
            val palette = remember(dark, state.settings.accent) { buildPalette(dark, state.settings.accent) }
            LaunchedEffect(dark) {
                val transparent = AndroidColor.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                )
            }
            BowelsTheme(palette) { Shell(state) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.openDestination(intent.getStringExtra(Notifications.EXTRA_DESTINATION))
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshConnections()
    }

    // Permissions, asked in context so nobody has to visit system settings.

    private fun withNotificationPermission(then: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(this)) {
            afterNotificationPermission = then
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else then()
    }

    private fun connectHealth(then: () -> Unit = {}) {
        when (app.healthConnect.status()) {
            HealthConnectManager.Status.Available -> {
                afterHealthPermission = then
                healthPermission.launch(app.healthConnect.permissions)
            }
            HealthConnectManager.Status.NeedsInstall, HealthConnectManager.Status.NeedsUpdate -> {
                runCatching { startActivity(app.healthConnect.installIntent()) }
                    .onFailure { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${HealthConnectManager.PROVIDER}"))) }
                then()
            }
            HealthConnectManager.Status.Unsupported -> then()
        }
    }

    /** Begin: notifications, then Health Connect (when it's already on the phone), then home. */
    private fun beginOnboarding() {
        withNotificationPermission {
            if (app.healthConnect.status() == HealthConnectManager.Status.Available) connectHealth { viewModel.completeOnboarding() }
            else viewModel.completeOnboarding()
        }
    }

    // Import / export.

    private fun exportData() {
        lifecycleScope.launch {
            runCatching {
                val json = viewModel.exportJson()
                val uri = withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "shared").apply { mkdirs() }
                    val file = File(dir, "bowels-export-${System.currentTimeMillis()}.json").apply { writeText(json) }
                    FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
                }
                val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(send, "Save your Bowels backup"))
            }.onFailure { dialog = "Export failed" to "Unable to create a backup right now." }
        }
    }

    private fun importFrom(uri: Uri) {
        lifecycleScope.launch {
            runCatching {
                val content = withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }
                viewModel.importJson(content)
            }.onSuccess { s ->
                dialog = "Import complete" to "${s.importedSessions} sessions imported, ${s.skippedSessions} duplicates skipped, ${s.importedHealthDays} health days merged."
            }.onFailure { dialog = "Import failed" to "The selected file is not a valid Bowels archive." }
        }
    }

    // Sharing.

    private fun currentPalette(): Palette {
        val s = viewModel.state.value.settings
        val dark = when (s.themeMode) {
            ThemeMode.System -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
        }
        return buildPalette(dark, s.accent)
    }

    private fun shareWeekly() {
        val weekly = viewModel.state.value.weekly ?: return
        lifecycleScope.launch {
            runCatching {
                val bitmap = withContext(Dispatchers.Default) { shareCards.weeklyWrapped(weekly, currentPalette()) }
                startActivity(shareCards.shareIntent(bitmap, "bowels-weekly-wrapped", "Share your Weekly Wrapped"))
            }.onFailure { dialog = "Share failed" to "Unable to share your Weekly Wrapped." }
        }
    }

    private fun saveWeekly() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        val weekly = viewModel.state.value.weekly ?: return
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) { shareCards.weeklyWrapped(weekly, currentPalette()) }
            val ok = shareCards.saveToGallery(bitmap, "bowels-weekly-${weekly.weekStart}")
            Toast.makeText(this@MainActivity, if (ok) "Saved to Pictures/Bowels" else "Unable to save to gallery.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareMilestone(m: Milestone) {
        val days = viewModel.state.value.analytics?.milestoneProgressDays ?: m.days
        lifecycleScope.launch {
            runCatching {
                val bitmap = withContext(Dispatchers.Default) { shareCards.milestone(m, days, currentPalette()) }
                startActivity(shareCards.shareIntent(bitmap, "bowels-milestone-${m.days}", "Share your milestone"))
            }.onFailure { dialog = "Share failed" to "Unable to share this milestone." }
        }
    }

    private fun checkUpdates() {
        lifecycleScope.launch {
            when (val result = UpdateChecker.check(BuildConfig.VERSION_NAME)) {
                is UpdateChecker.Result.Available -> updateOffer = result.release
                UpdateChecker.Result.UpToDate -> dialog = "Up to Date" to "You're running the latest version!"
                UpdateChecker.Result.NoApk -> dialog = "No APK Found" to "No APK file found in the latest release."
                UpdateChecker.Result.Failed -> dialog = "Check Failed" to "Unable to check for updates. Please check your internet connection."
            }
        }
    }

    @Composable
    private fun Shell(state: UiState) {
        val p = LocalPalette.current
        if (state.loading) {
            Box(Modifier.fillMaxSize().background(p.background))
            return
        }
        val screen = state.screen
        val offTab = screen in setOf(Screen.Weekly, Screen.Health, Screen.Settings, Screen.Questions, Screen.Badges)
        BackHandler(enabled = screen != Screen.Home && screen != Screen.Onboarding) {
            viewModel.setScreen(if (screen == Screen.Questions) Screen.Active else Screen.Home)
        }

        Box(Modifier.fillMaxSize().background(p.background)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                if (screen != Screen.Onboarding) {
                    val title = when (screen) {
                        Screen.History -> "Data & Privacy"
                        Screen.Analytics -> "Insights"
                        Screen.Badges -> "Milestones"
                        else -> "Bowels"
                    }
                    ScreenHeader(title, back = offTab) { viewModel.setScreen(if (offTab) Screen.Home else Screen.Settings) }
                }
                Box(Modifier.weight(1f)) { Content(state) }
            }
            if (screen != Screen.Onboarding) {
                BottomNav(if (screen == Screen.Questions) Screen.Active else screen, viewModel::setScreen, Modifier.align(Alignment.BottomCenter))
            }
        }

        dialog?.let { (title, body) ->
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { BText(title, p.onSurface, weight = FontWeight.Bold) },
                text = { BText(body, p.onSurfaceVariant) },
                confirmButton = { TextButton(onClick = { dialog = null }) { BText("OK", p.primary, weight = FontWeight.Bold) } },
            )
        }
        updateOffer?.let { release ->
            AlertDialog(
                onDismissRequest = { updateOffer = null },
                title = { BText("Update Available", p.onSurface, weight = FontWeight.Bold) },
                text = { BText("Version ${release.tag} is available. You're on ${BuildConfig.VERSION_NAME}.\n\nWould you like to download it?", p.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = {
                        updateOffer = null
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl))) }
                    }) { BText("Download", p.primary, weight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { updateOffer = null }) { BText("Cancel", p.onSurfaceVariant, weight = FontWeight.Bold) } },
            )
        }
    }

    @Composable
    private fun Content(state: UiState) {
        val p = LocalPalette.current
        when (state.screen) {
            Screen.Onboarding -> OnboardingScreen(onBegin = ::beginOnboarding, onImport = { importDocument.launch(arrayOf("application/json", "text/json", "text/plain", "*/*")) })
            Screen.Home -> HomeScreen(
                state,
                HomeActions(
                    setScreen = viewModel::setScreen,
                    startSession = { withNotificationPermission(viewModel::startSession) },
                    quickLog = viewModel::quickLog,
                    saveHealth = viewModel::saveDailyHealth,
                    connectHealth = { connectHealth() },
                    installOnWatch = viewModel::installOnWatch,
                    dismissWatch = viewModel::dismissWatchPrompt,
                ),
            )
            Screen.Active -> ActiveSessionScreen(state.draft, viewModel::updateDraft, onStart = { withNotificationPermission(viewModel::startSession) }, onFinish = viewModel::finishSession, onCancel = viewModel::cancelSession)
            Screen.Questions -> QuestionsScreen(
                state.draft, state.ownHealth, state.ownFilledToday, state.connections,
                QuestionActions(viewModel::updateDraft, viewModel::setKind, viewModel::setManualVitals, { connectHealth() }, viewModel::saveQuestions),
            )
            Screen.History -> HistoryScreen(state.sessions, onExport = ::exportData, onImport = { importDocument.launch(arrayOf("application/json", "text/json", "text/plain", "*/*")) })
            Screen.Analytics -> state.analytics?.let { AnalyticsScreen(it, state.insights) }
            Screen.Weekly -> state.weekly?.let { weekly ->
                val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, weekly, p) {
                    value = withContext(Dispatchers.Default) { shareCards.weeklyWrapped(weekly, p).asImageBitmap() }
                }
                WeeklyWrappedScreen(weekly, preview, onShare = ::shareWeekly, onSave = ::saveWeekly)
            }
            Screen.Badges -> BadgesScreen(state.analytics?.milestoneProgressDays ?: 0, onShare = ::shareMilestone)
            Screen.Health -> HealthInfoScreen()
            Screen.Settings -> SettingsScreen(
                state.settings, state.connections, BuildConfig.VERSION_NAME,
                SettingsActions(viewModel::setThemeMode, viewModel::setAccent, viewModel::setReminderHour, { connectHealth() }, viewModel::installOnWatch, ::checkUpdates),
            )
        }
    }
}
