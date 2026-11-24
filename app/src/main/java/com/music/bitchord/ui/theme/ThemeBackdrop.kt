package com.music.bitchord.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ThemeStyle
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything needed to paint the page behind the app: the flat colour, the
 * style's own decoration, and the user's wallpaper if they set one.
 *
 * For the original style with no wallpaper this paints exactly the flat
 * background colour the app always had.
 */
@Immutable
data class ThemeBackdropSpec(
    val style: ThemeStyle = ThemeStyle.BITCHORD,
    val dark: Boolean = true,
    val base: Color = Color.Black,
    val wallpaper: ImageBitmap? = null,
    /** How much of [base] is laid over the wallpaper, 0..1. */
    val dim: Float = 0f,
    val accent: Color = Color.White,
    val accent2: Color = Color.White,
)

val LocalThemeBackdrop = staticCompositionLocalOf { ThemeBackdropSpec() }

/** The backdrop for the theme in force; call it inside the app's theme. */
@Composable
fun rememberThemeBackdrop(): ThemeBackdropSpec {
    val extras = LocalThemeExtras.current
    val scheme = MaterialTheme.colorScheme
    val wallpaper = rememberWallpaperImage()
    val dim by AppSettings.wallpaperDim.collectAsStateWithLifecycle()
    return remember(extras.style, extras.dark, scheme.background, scheme.primary, scheme.secondary, wallpaper, dim) {
        ThemeBackdropSpec(
            style = extras.style,
            dark = extras.dark,
            base = scheme.background,
            wallpaper = wallpaper,
            dim = dim,
            accent = scheme.primary,
            accent2 = scheme.secondary,
        )
    }
}

fun DrawScope.drawThemeBackdrop(spec: ThemeBackdropSpec) {
    drawRect(spec.base)
    val wallpaper = spec.wallpaper
    if (wallpaper != null) {
        drawCover(wallpaper)
        if (spec.dim > 0f) drawRect(spec.base.copy(alpha = spec.dim))
        return
    }
    when (spec.style) {
        ThemeStyle.VAPORWAVE -> drawVaporwave(spec)
        ThemeStyle.SPACE -> drawSpace(spec)
        else -> Unit
    }
}

/** The image scaled to fill the window and cropped from the middle. */
private fun DrawScope.drawCover(image: ImageBitmap) {
    val iw = image.width
    val ih = image.height
    if (iw < 1 || ih < 1 || size.width < 1f || size.height < 1f) return
    val scale = max(size.width / iw, size.height / ih)
    val srcW = (size.width / scale).roundToInt().coerceIn(1, iw)
    val srcH = (size.height / scale).roundToInt().coerceIn(1, ih)
    drawImage(
        image = image,
        srcOffset = IntOffset((iw - srcW) / 2, (ih - srcH) / 2),
        srcSize = IntSize(srcW, srcH),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        filterQuality = FilterQuality.Medium,
    )
}

private fun DrawScope.drawVaporwave(spec: ThemeBackdropSpec) {
    val w = size.width
    val h = size.height
    val dark = spec.dark
    val top = if (dark) Color(0xFF12062B) else Color(0xFFFFD9F7)
    val mid = if (dark) Color(0xFF2B1160) else Color(0xFFE5D8FF)
    val bottom = if (dark) Color(0xFF5E1A7A) else Color(0xFFCFF3FF)
    drawRect(Brush.verticalGradient(listOf(top, mid, bottom)))

    val horizon = h * 0.82f
    // The sun.
    val radius = w * 0.30f
    val centre = Offset(w / 2f, horizon - radius * 0.15f)
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFE066).copy(alpha = if (dark) 0.35f else 0.55f),
                Color(0xFFFF4FB8).copy(alpha = if (dark) 0.40f else 0.55f),
            ),
            startY = centre.y - radius,
            endY = centre.y + radius,
        ),
        radius = radius,
        center = centre,
    )
    // The grid on the ground, in perspective.
    val line = spec.accent2.copy(alpha = if (dark) 0.28f else 0.35f)
    val stroke = 1.dp.toPx()
    drawRect(
        color = (if (dark) Color(0xFF12062B) else Color(0xFFF6E3FF)).copy(alpha = 0.55f),
        topLeft = Offset(0f, horizon),
        size = androidx.compose.ui.geometry.Size(w, h - horizon),
    )
    for (i in 0..8) {
        val t = i / 8f
        val y = horizon + (h - horizon) * t * t
        drawLine(line, Offset(0f, y), Offset(w, y), strokeWidth = stroke)
    }
    for (k in -9..9) {
        val bottomX = w / 2f + k * (w / 7f)
        val topX = w / 2f + k * (w / 40f)
        drawLine(line, Offset(topX, horizon), Offset(bottomX, h), strokeWidth = stroke)
    }
}

/** Star positions as fractions of the window, made once. */
private val STARS: List<Triple<Float, Float, Float>> = Random(2024).let { rnd ->
    List(80) { Triple(rnd.nextFloat(), rnd.nextFloat(), 0.4f + rnd.nextFloat() * 1.2f) }
}

private fun DrawScope.drawSpace(spec: ThemeBackdropSpec) {
    val w = size.width
    val h = size.height
    val dark = spec.dark
    drawRect(
        Brush.verticalGradient(
            if (dark) listOf(Color(0xFF02030A), Color(0xFF0A1330))
            else listOf(Color(0xFFF4F8FD), Color(0xFFE3ECF8)),
        ),
    )
    // Stars (faint dots on paper in the light version).
    val starColor = if (dark) Color.White else Color(0xFF0A1A33)
    val starAlpha = if (dark) 0.75f else 0.18f
    for ((fx, fy, r) in STARS) {
        drawCircle(starColor.copy(alpha = starAlpha), radius = r.dp.toPx(), center = Offset(fx * w, fy * h))
    }
    // Coordinate grid: small crosses where the lines would meet.
    val step = 56.dp.toPx()
    val tick = 3.dp.toPx()
    val c = spec.accent.copy(alpha = if (dark) 0.22f else 0.25f)
    val stroke = 1.dp.toPx()
    var y = step
    while (y < h) {
        var x = step
        while (x < w) {
            drawLine(c, Offset(x - tick, y), Offset(x + tick, y), strokeWidth = stroke)
            drawLine(c, Offset(x, y - tick), Offset(x, y + tick), strokeWidth = stroke)
            x += step
        }
        y += step
    }
}

/**
 * The user's wallpaper, kept as one JPEG in the app's private storage.
 * Importing copies it (downscaled), so it keeps working if the original is
 * deleted and never needs a storage permission afterwards.
 */
object WallpaperStore {
    private const val FILE_NAME = "wallpaper.jpg"
    private const val MAX_EDGE = 1600

    @Volatile private var cachedVersion = -1L
    @Volatile private var cachedBitmap: Bitmap? = null

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    /** @return whether the picture was readable and is now the wallpaper. */
    suspend fun import(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth < 1 || bounds.outHeight < 1) return@runCatching false

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@runCatching false

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val upright = if (degrees == 0f) decoded else Bitmap.createBitmap(
                decoded, 0, 0, decoded.width, decoded.height,
                Matrix().apply { postRotate(degrees) }, true,
            )

            file(context).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            cachedBitmap = null
            cachedVersion = -1L
            true
        }.getOrDefault(false)
    }

    fun delete(context: Context) {
        runCatching { file(context).delete() }
        cachedBitmap = null
        cachedVersion = -1L
    }

    /** The wallpaper, softened by [blur] (0 sharp .. 1 very soft), or null if there is none. */
    suspend fun load(context: Context, version: Long, blur: Float): ImageBitmap? = withContext(Dispatchers.Default) {
        runCatching {
            val cached = cachedBitmap?.takeIf { cachedVersion == version }
            val source: Bitmap = cached ?: run {
                val f = file(context)
                if (!f.exists()) return@runCatching null
                val decoded = BitmapFactory.decodeFile(f.absolutePath) ?: return@runCatching null
                cachedBitmap = decoded
                cachedVersion = version
                decoded
            }
            val shown = if (blur <= 0.01f) source else soften(source, blur)
            shown.asImageBitmap()
        }.getOrNull()
    }

    /**
     * A cheap blur: shrink in halves, which averages neighbouring pixels, then
     * let the draw scale it back up smoothly. Works on every Android version.
     */
    private fun soften(src: Bitmap, blur: Float): Bitmap {
        val factor = 1f + blur * 28f
        val targetW = max(8, (src.width / factor).roundToInt())
        val targetH = max(8, (src.height / factor).roundToInt())
        var current = src
        while (current.width / 2 >= targetW && current.height / 2 >= targetH) {
            current = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
        }
        return Bitmap.createScaledBitmap(current, targetW, targetH, true)
    }
}

/** The wallpaper as the settings currently describe it; null while there is none. */
@Composable
fun rememberWallpaperImage(): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val version by AppSettings.wallpaperVersion.collectAsStateWithLifecycle()
    val blur by AppSettings.wallpaperBlur.collectAsStateWithLifecycle()
    val image by produceState<ImageBitmap?>(null, version, blur) {
        value = if (version == 0L) null else WallpaperStore.load(context, version, blur)
    }
    // No wallpaper set: never show a stale one.
    return if (version == 0L) null else image
}
