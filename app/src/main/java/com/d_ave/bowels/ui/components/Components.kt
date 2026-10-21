package com.d_ave.bowels.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.data.Screen
import com.d_ave.bowels.ui.theme.LocalPalette
import com.d_ave.bowels.ui.theme.Manrope

@Composable
fun BText(
    text: String,
    color: Color,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) = Text(
    text = text,
    modifier = modifier,
    maxLines = maxLines,
    style = TextStyle(fontFamily = Manrope, color = color, fontSize = size, fontWeight = weight, textAlign = align ?: TextAlign.Unspecified, lineHeight = lineHeight, letterSpacing = letterSpacing),
)

/** The scrolling body every tab uses, with room for the bottom bar. */
@Composable
fun ScreenColumn(gap: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 140.dp)),
    verticalArrangement = Arrangement.spacedBy(gap),
    content = content,
)

@Composable
fun BCard(modifier: Modifier = Modifier, color: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x339FA28F), spotColor = Color(0x339FA28F))
            .background(color ?: p.surfaceContainerLowest, RoundedCornerShape(32.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun GradientButton(label: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(p.primary, p.primaryContainer)))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BText(label, Color.White, 16.sp, FontWeight.Bold)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, null, tint = p.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SecondaryButton(label: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.secondary)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = p.onPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        BText(label, p.onPrimary, 16.sp, FontWeight.Bold)
    }
}

@Composable
fun OptionChip(label: String, active: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) p.primary else p.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BText(label.replaceFirstChar { it.uppercase() }, if (active) p.onPrimary else p.onSurface, 13.sp, FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipRow(options: List<T>, selected: (T) -> Boolean, label: (T) -> String = { it.toString() }, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> OptionChip(label(option), selected(option)) { onSelect(option) } }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BText(title, p.onSurface, 28.sp, FontWeight.ExtraBold, lineHeight = 34.sp)
        if (subtitle != null) BText(subtitle, p.onSurfaceVariant, 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun FieldLabel(text: String) {
    val p = LocalPalette.current
    BText(text, p.onSurface, 14.sp, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun ScreenHeader(title: String, back: Boolean, onLeft: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onLeft), contentAlignment = Alignment.Center) {
            Icon(if (back) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Settings, if (back) "Back" else "Settings", tint = p.primaryContainer)
        }
        BText(title, p.primary, 24.sp, FontWeight.ExtraBold)
        Spacer(Modifier.size(40.dp))
    }
}

private val navItems = listOf(
    Triple(Screen.Home, "Home", Icons.AutoMirrored.Filled.MenuBook),
    Triple(Screen.Active, "Rhythm", Icons.Filled.BakeryDining),
    Triple(Screen.Analytics, "Insights", Icons.Filled.Insights),
    Triple(Screen.History, "History", Icons.Filled.History),
)

@Composable
fun BottomNav(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
            .background(if (p.dark) Color(0xEB252815) else p.surfaceContainerLow.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        navItems.forEach { (screen, label, icon) ->
            val active = current == screen
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) (if (p.dark) p.surfaceContainerHigh else p.surfaceContainerLowest) else Color.Transparent)
                    .clickable { onSelect(screen) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(icon, label, tint = if (active) p.primary else p.onSurfaceVariant, modifier = Modifier.size(22.dp))
                BText(label.uppercase(), if (active) p.primary else p.onSurfaceVariant, 10.sp, FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

/** A small, dismissable card that offers one tap to connect something. */
@Composable
fun ConnectCard(icon: ImageVector, title: String, body: String, action: String, onAction: () -> Unit, onDismiss: (() -> Unit)? = null) {
    val p = LocalPalette.current
    BCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).background(p.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = p.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BText(title, p.onSurface, 16.sp, FontWeight.Bold)
                BText(body, p.onSurfaceVariant, 13.sp, lineHeight = 18.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GradientButton(action, modifier = Modifier.weight(1f), onClick = onAction)
            if (onDismiss != null) {
                BText("Not now", p.onSurfaceVariant, 14.sp, FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(12.dp))
            }
        }
    }
}

@Composable
fun Pill(text: String, fg: Color, bg: Color) {
    Box(Modifier.background(bg, CircleShape).padding(horizontal = 12.dp, vertical = 8.dp)) {
        BText(text, fg, 12.sp, FontWeight.Bold)
    }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
