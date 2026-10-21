package com.d_ave.bowels.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.d_ave.bowels.R
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.Milestone
import com.d_ave.bowels.domain.WeeklyWrappedSummary
import com.d_ave.bowels.domain.stoolTypeMeta
import com.d_ave.bowels.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

/**
 * Draws share images straight onto a 1080x1920 canvas (Instagram story size) instead of
 * screenshotting the screen. The old app captured the on-screen card and forced it to
 * 1080x1920, which stretched it to the wrong aspect ratio and upscaled it, so shared images
 * came out flattened and blurry. Here every pixel is drawn at full size, with real text.
 */
class ShareCards(private val context: Context) {
    private val width = 1080
    private val height = 1920
    private val margin = 72f

    private val regular = font(R.font.manrope_regular)
    private val bold = font(R.font.manrope_bold)
    private val extraBold = font(R.font.manrope_extrabold)

    /** Olive on near-black is too dim; dark images use the lighter tint of the accent. */
    private fun accentOf(p: Palette) = if (p.dark) p.primaryFixedDim else p.primary

    private fun font(id: Int): Typeface = ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT

    fun weeklyWrapped(w: WeeklyWrappedSummary, p: Palette): Bitmap = draw(p) { canvas ->
        var y = 120f
        y = label(canvas, "BOWELS", accentOf(p).toArgb(), 34f, y, letterSpacing = 0.3f)
        y = centered(canvas, "Weekly Wrapped", extraBold, 92f, p.onSurface.toArgb(), y + 18f)
        val range = "${w.weekStart.format(DAY)} – ${w.weekStart.plusDays(6).format(DAY)}"
        y = centered(canvas, range, bold, 34f, p.onSurfaceVariant.toArgb(), y + 10f)

        // Hero: visits this week.
        y += 40f
        val hero = RectF(margin, y, width - margin, y + 300f)
        card(canvas, hero, p.surfaceContainerLowest.toArgb())
        centered(canvas, w.totalVisits.toString(), extraBold, 150f, accentOf(p).toArgb(), hero.top + 18f)
        label(canvas, "TOTAL VISITS THIS WEEK", p.onSurfaceVariant.toArgb(), 28f, hero.bottom - 64f, letterSpacing = 0.15f)
        y = hero.bottom + 28f

        // Personality.
        val (type, desc) = Analytics.personality(w)
        val persona = RectF(margin, y, width - margin, y + 200f)
        card(canvas, persona, p.surfaceContainerLowest.toArgb())
        centered(canvas, "🧠", regular, 52f, 0xFF000000.toInt(), persona.top + 18f)
        centered(canvas, type, extraBold, 50f, p.onSurface.toArgb(), persona.top + 86f)
        centered(canvas, desc, bold, 28f, (if (p.dark) p.secondaryFixed else p.secondary).toArgb(), persona.top + 150f)
        y = persona.bottom + 28f

        // Two tiles, or three when the watch measured heart rate.
        val tiles = buildList {
            add("AVG TIME" to "${max(0, (w.averageDurationSeconds / 60.0).toInt())}m")
            add("COMMON" to (w.mostCommonStoolType?.let { stoolTypeMeta.getValue(it).let { m -> "${m.emoji} ${m.short}" } } ?: "—"))
            w.averageHeartRate?.let { add("AVG HEART" to "$it bpm") }
        }
        val gap = 28f
        val tileW = (width - 2 * margin - gap * (tiles.size - 1)) / tiles.size
        tiles.forEachIndexed { i, (title, value) ->
            val left = margin + i * (tileW + gap)
            val rect = RectF(left, y, left + tileW, y + 220f)
            card(canvas, rect, p.surfaceContainerLowest.toArgb())
            text(canvas, title, bold, 28f, p.onSurfaceVariant.toArgb(), rect.left + 36f, rect.top + 36f, letterSpacing = 0.12f)
            fitText(canvas, value, extraBold, if (tiles.size == 3) 62f else 76f, accentOf(p).toArgb(), rect, rect.top + 100f)
        }
        y += 220f + 28f

        // The week as bars.
        val chart = RectF(margin, y, width - margin, y + 280f)
        card(canvas, chart, p.surfaceContainerLowest.toArgb())
        text(canvas, "Daily rhythm", bold, 36f, p.onSurface.toArgb(), chart.left + 40f, chart.top + 30f)
        val maxCount = max(1, w.visitsPerDay.maxOfOrNull { it.count } ?: 1)
        val slot = (chart.width() - 80f) / 7f
        w.visitsPerDay.forEachIndexed { i, day ->
            val barH = 30f + 110f * day.count / maxCount
            val cx = chart.left + 40f + slot * i + slot / 2
            val bottom = chart.bottom - 60f
            val color = when {
                day == w.bestDay && day.count > 0 -> accentOf(p)
                day.count > 0 -> if (p.dark) p.primaryFixedDim.copy(alpha = 0.45f) else p.primaryFixedDim
                else -> p.surfaceContainerHigh
            }.toArgb()
            fill.color = color
            canvas.drawRoundRect(RectF(cx - 28f, bottom - barH, cx + 28f, bottom), 28f, 28f, fill)
            val dayLabel = day.day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase()
            centeredAt(canvas, dayLabel, bold, 24f, p.outline.toArgb(), cx, bottom + 16f)
        }
        y = chart.bottom + 28f

        // Quirky line.
        val quirk = RectF(margin, y, width - margin, height - 170f)
        card(canvas, quirk, p.surfaceContainerHigh.toArgb())
        val line = Analytics.quirkyInsight(w)
        // The "most productive day" line already names the best day's count.
        val best = w.bestDay?.takeIf { it.count > 0 && !line.startsWith("Your most productive") }
            ?.let { "Best day: ${it.day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}. " }.orEmpty()
        paragraph(canvas, "✨ $best$line", regular, 34f, p.onSurfaceVariant.toArgb(), quirk, center = true)

        footer(canvas, p)
    }

    fun milestone(m: Milestone, progressDays: Int, p: Palette): Bitmap = draw(p) { canvas ->
        label(canvas, "BOWELS", accentOf(p).toArgb(), 34f, 170f, letterSpacing = 0.3f)
        centered(canvas, "Milestone unlocked", bold, 44f, p.onSurfaceVariant.toArgb(), 240f)

        // A big gradient badge.
        val cx = width / 2f
        val cy = 720f
        val r = 300f
        val (top, bottom) = when (m.days) {
            1 -> p.primaryFixedDim to p.primaryContainer
            7 -> p.secondaryFixed to p.secondary
            else -> p.primaryFixedDim to p.primary
        }
        fill.shader = LinearGradient(cx, cy - r, cx, cy + r, top.toArgb(), bottom.toArgb(), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, fill)
        fill.shader = null
        stroke.color = 0x40FFFFFF
        stroke.strokeWidth = 16f
        canvas.drawCircle(cx, cy, r - 8f, stroke)
        centeredAt(canvas, milestoneEmoji(m.days), regular, 260f, 0xFF000000.toInt(), cx, cy - 175f)

        var y = cy + r + 90f
        y = centered(canvas, m.title, extraBold, 96f, p.onSurface.toArgb(), y)
        val pillText = "${m.days} DAY${if (m.days == 1) "" else "S"} ACHIEVED"
        val pill = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 34f; letterSpacing = 0.12f; color = accentOf(p).toArgb() }
        val pw = pill.measureText(pillText) + 96f
        val pillRect = RectF(cx - pw / 2, y + 36f, cx + pw / 2, y + 36f + 90f)
        fill.color = accentOf(p).copy(alpha = 0.14f).toArgb()
        canvas.drawRoundRect(pillRect, 45f, 45f, fill)
        canvas.drawText(pillText, cx - pill.measureText(pillText) / 2, pillRect.centerY() - (pill.descent() + pill.ascent()) / 2, pill)
        y = pillRect.bottom + 50f
        val bodyRect = RectF(margin, y, width - margin, y + 200f)
        paragraph(canvas, m.body, regular, 40f, p.onSurfaceVariant.toArgb(), bodyRect, center = true)
        val streakColor = if (p.dark) p.secondaryFixed else p.secondary
        centered(canvas, "$progressDays days of listening to my body", bold, 36f, streakColor.toArgb(), bodyRect.bottom + 40f)

        footer(canvas, p)
    }

    private fun milestoneEmoji(days: Int) = when (days) {
        1 -> "🌱"
        7 -> "🌊"
        30 -> "💚"
        100 -> "🏆"
        else -> "✨"
    }

    // Drawing helpers.

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private fun draw(p: Palette, block: (Canvas) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        fill.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), p.background.toArgb(), p.surfaceContainer.toArgb(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        fill.shader = null
        block(canvas)
        return bitmap
    }

    private fun card(canvas: Canvas, rect: RectF, color: Int) {
        fill.color = 0x14000000
        canvas.drawRoundRect(RectF(rect.left + 6f, rect.top + 12f, rect.right + 6f, rect.bottom + 12f), 56f, 56f, fill)
        fill.color = color
        canvas.drawRoundRect(rect, 56f, 56f, fill)
    }

    private fun paint(face: Typeface, size: Float, color: Int, letterSpacing: Float = 0f) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face; textSize = size; this.color = color; this.letterSpacing = letterSpacing }

    /** Draws a line of text with its top at [top]; returns the bottom. */
    private fun text(canvas: Canvas, s: String, face: Typeface, size: Float, color: Int, x: Float, top: Float, letterSpacing: Float = 0f): Float {
        val p = paint(face, size, color, letterSpacing)
        canvas.drawText(s, x, top - p.ascent(), p)
        return top + p.descent() - p.ascent()
    }

    private fun centeredAt(canvas: Canvas, s: String, face: Typeface, size: Float, color: Int, cx: Float, top: Float, letterSpacing: Float = 0f): Float {
        val p = paint(face, size, color, letterSpacing)
        canvas.drawText(s, cx - p.measureText(s) / 2, top - p.ascent(), p)
        return top + p.descent() - p.ascent()
    }

    private fun centered(canvas: Canvas, s: String, face: Typeface, size: Float, color: Int, top: Float) =
        centeredAt(canvas, s, face, size, color, width / 2f, top)

    private fun label(canvas: Canvas, s: String, color: Int, size: Float, top: Float, letterSpacing: Float) =
        centeredAt(canvas, s, extraBold, size, color, width / 2f, top, letterSpacing)

    /** Shrinks [s] until it fits inside [rect] with padding. */
    private fun fitText(canvas: Canvas, s: String, face: Typeface, size: Float, color: Int, rect: RectF, top: Float) {
        val p = paint(face, size, color)
        while (p.measureText(s) > rect.width() - 64f && p.textSize > 24f) p.textSize -= 2f
        canvas.drawText(s, rect.left + 36f, top - p.ascent(), p)
    }

    private fun paragraph(canvas: Canvas, s: String, face: Typeface, size: Float, color: Int, rect: RectF, center: Boolean) {
        val p = paint(face, size, color)
        val inner = (rect.width() - 96f).toInt()
        var layout = build(s, p, inner, center)
        while (layout.height > rect.height() - 64f && p.textSize > 24f) {
            p.textSize -= 2f
            layout = build(s, p, inner, center)
        }
        canvas.save()
        canvas.translate(rect.left + 48f, rect.top + (rect.height() - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun build(s: String, p: TextPaint, w: Int, center: Boolean) =
        StaticLayout.Builder.obtain(s, 0, s.length, p, w)
            .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.25f)
            .build()

    private fun footer(canvas: Canvas, p: Palette) {
        centered(canvas, "Tracked privately with Bowels", bold, 30f, p.outline.toArgb(), height - 120f)
    }

    // Output.

    private suspend fun writeTemp(bitmap: Bitmap, name: String): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun shareIntent(bitmap: Bitmap, name: String, title: String): Intent {
        val uri = writeTemp(bitmap, name)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title)
    }

    /** Saves into Pictures/Bowels. Needs no permission on Android 10+. */
    suspend fun saveToGallery(bitmap: Bitmap, name: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Bowels")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

    private companion object {
        val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    }
}
