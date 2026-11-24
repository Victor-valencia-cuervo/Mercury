package com.music.bitchord.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.settings.ThemeStyle

/**
 * What a style adds on top of Material's colours: the line weights, shadows and
 * bevels that make Neobrutalism look hard-edged and Windows 95 look 3D.
 *
 * Read through [LocalThemeExtras]. For [ThemeStyle.BITCHORD] everything here is
 * zero, and every modifier below returns its receiver unchanged, so the
 * original look is exactly what it was before styles existed.
 */
@Immutable
data class ThemeExtras(
    val style: ThemeStyle = ThemeStyle.BITCHORD,
    val dark: Boolean = true,
    val borderWidth: Dp = 0.dp,
    val borderColor: Color = Color.Unspecified,
    /** Neobrutalism's hard, un-blurred shadow. */
    val shadowOffset: Dp = 0.dp,
    val shadowColor: Color = Color.Unspecified,
    /** Neon glow colour (Vaporwave) / bracket colour (Space). */
    val glowColor: Color = Color.Unspecified,
    val glowColor2: Color = Color.Unspecified,
    /** Windows 95 bevel: outer light/dark edges, then inner light/dark. */
    val bevelOuterLight: Color = Color.Unspecified,
    val bevelOuterDark: Color = Color.Unspecified,
    val bevelInnerLight: Color = Color.Unspecified,
    val bevelInnerDark: Color = Color.Unspecified,
) {
    val isDefault: Boolean get() = style == ThemeStyle.BITCHORD
}

val LocalThemeExtras = staticCompositionLocalOf { ThemeExtras() }

internal class StylePalette(val scheme: ColorScheme, val extras: ThemeExtras)

/** Black or white, whichever reads on [c]. */
internal fun contentOn(c: Color): Color = if (c.luminance() > 0.5f) Color.Black else Color.White

private fun buildScheme(
    dark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    outline: Color,
    error: Color,
): ColorScheme {
    val onPrimary = contentOn(primary)
    val primaryContainer = lerp(surface, primary, 0.28f)
    val secondaryContainer = lerp(surface, secondary, 0.28f)
    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onSurface,
            secondary = secondary,
            onSecondary = contentOn(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSurface,
            tertiary = tertiary,
            onTertiary = contentOn(tertiary),
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outline,
            error = error,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = surfaceVariant,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onSurface,
            secondary = secondary,
            onSecondary = contentOn(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSurface,
            tertiary = tertiary,
            onTertiary = contentOn(tertiary),
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outline,
            error = error,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = surfaceVariant,
        )
    }
}

/**
 * The colour scheme and extras of [style] in light or dark, with the user's
 * [accent] (if any) taking over `primary`.
 *
 * [original] is the BitChord scheme for this mode, handed in because it is
 * private to Theme.kt.
 */
internal fun stylePalette(
    style: ThemeStyle,
    dark: Boolean,
    accent: Color?,
    original: ColorScheme,
): StylePalette {
    val palette = when (style) {
        ThemeStyle.BITCHORD -> StylePalette(original, ThemeExtras(style, dark))

        ThemeStyle.NEOBRUTALISM -> if (dark) {
            val primary = Color(0xFFFFD23F)
            StylePalette(
                buildScheme(
                    dark = true,
                    primary = primary,
                    secondary = Color(0xFFFF6B9D),
                    tertiary = Color(0xFF4ADE80),
                    background = Color(0xFF17151F),
                    surface = Color(0xFF23202E),
                    surfaceVariant = Color(0xFF2E2A3D),
                    onSurface = Color(0xFFFFF8E7),
                    onSurfaceVariant = Color(0xFFC4BCAA),
                    outline = Color(0xFFFFF8E7),
                    error = Color(0xFFFF5C5C),
                ),
                ThemeExtras(
                    style = style, dark = true,
                    borderWidth = 2.5.dp, borderColor = Color(0xFFFFF8E7),
                    shadowOffset = 4.dp, shadowColor = primary,
                ),
            )
        } else {
            StylePalette(
                buildScheme(
                    dark = false,
                    primary = Color(0xFFD6246E),
                    secondary = Color(0xFF2F6FED),
                    tertiary = Color(0xFF00946A),
                    background = Color(0xFFFFF1D0),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFFFDF8A),
                    onSurface = Color(0xFF000000),
                    onSurfaceVariant = Color(0xFF3D3A33),
                    outline = Color(0xFF000000),
                    error = Color(0xFFD62828),
                ),
                ThemeExtras(
                    style = style, dark = false,
                    borderWidth = 2.5.dp, borderColor = Color.Black,
                    shadowOffset = 4.dp, shadowColor = Color.Black,
                ),
            )
        }

        ThemeStyle.WINDOWS95 -> if (dark) {
            StylePalette(
                buildScheme(
                    dark = true,
                    primary = Color(0xFF4FC3C3),
                    secondary = Color(0xFF8AA4FF),
                    tertiary = Color(0xFFD08CFF),
                    background = Color(0xFF383838),
                    surface = Color(0xFF2A2A2A),
                    surfaceVariant = Color(0xFF4A4A4A),
                    onSurface = Color(0xFFEDEDED),
                    onSurfaceVariant = Color(0xFFB4B4B4),
                    outline = Color(0xFF808080),
                    error = Color(0xFFFF6B6B),
                ),
                ThemeExtras(
                    style = style, dark = true,
                    borderWidth = 2.dp,
                    bevelOuterLight = Color(0xFF7A7A7A), bevelOuterDark = Color(0xFF0A0A0A),
                    bevelInnerLight = Color(0xFF5C5C5C), bevelInnerDark = Color(0xFF1C1C1C),
                ),
            )
        } else {
            StylePalette(
                buildScheme(
                    dark = false,
                    primary = Color(0xFF000080),
                    secondary = Color(0xFF008080),
                    tertiary = Color(0xFF800080),
                    background = Color(0xFFC0C0C0),
                    surface = Color(0xFFDFDFDF),
                    surfaceVariant = Color(0xFFD0D0D0),
                    onSurface = Color(0xFF000000),
                    onSurfaceVariant = Color(0xFF404040),
                    outline = Color(0xFF808080),
                    error = Color(0xFFB00000),
                ),
                ThemeExtras(
                    style = style, dark = false,
                    borderWidth = 2.dp,
                    bevelOuterLight = Color(0xFFFFFFFF), bevelOuterDark = Color(0xFF0A0A0A),
                    bevelInnerLight = Color(0xFFDFDFDF), bevelInnerDark = Color(0xFF808080),
                ),
            )
        }

        ThemeStyle.VAPORWAVE -> if (dark) {
            val pink = Color(0xFFFF71CE)
            val cyan = Color(0xFF01CDFE)
            StylePalette(
                buildScheme(
                    dark = true,
                    primary = pink,
                    secondary = cyan,
                    tertiary = Color(0xFFB967FF),
                    background = Color(0xFF1A0B2E),
                    surface = Color(0xFF261049),
                    surfaceVariant = Color(0xFF34195F),
                    onSurface = Color(0xFFFDF2FF),
                    onSurfaceVariant = Color(0xFFC9A9E8),
                    outline = Color(0xFF5B3A8C),
                    error = Color(0xFFFF6B8B),
                ),
                ThemeExtras(
                    style = style, dark = true,
                    borderWidth = 1.5.dp, borderColor = cyan.copy(alpha = 0.75f),
                    glowColor = pink, glowColor2 = cyan,
                ),
            )
        } else {
            val pink = Color(0xFFC2189B)
            val cyan = Color(0xFF0097B8)
            StylePalette(
                buildScheme(
                    dark = false,
                    primary = pink,
                    secondary = cyan,
                    tertiary = Color(0xFF7B2FF7),
                    background = Color(0xFFFBE6FF),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFF1DDFF),
                    onSurface = Color(0xFF2B1055),
                    onSurfaceVariant = Color(0xFF6B4C93),
                    outline = Color(0xFFD4B3F0),
                    error = Color(0xFFC62848),
                ),
                ThemeExtras(
                    style = style, dark = false,
                    borderWidth = 1.5.dp, borderColor = pink.copy(alpha = 0.6f),
                    glowColor = pink, glowColor2 = cyan,
                ),
            )
        }

        ThemeStyle.SPACE -> if (dark) {
            val cyan = Color(0xFF4DE1FF)
            StylePalette(
                buildScheme(
                    dark = true,
                    primary = cyan,
                    secondary = Color(0xFF7CFFB2),
                    tertiary = Color(0xFFFFB454),
                    background = Color(0xFF04060F),
                    surface = Color(0xFF0B1226),
                    surfaceVariant = Color(0xFF141D3A),
                    onSurface = Color(0xFFE6F0FF),
                    onSurfaceVariant = Color(0xFF8FA3C7),
                    outline = Color(0xFF2A3B66),
                    error = Color(0xFFFF5C6C),
                ),
                ThemeExtras(
                    style = style, dark = true,
                    borderWidth = 1.dp, borderColor = cyan.copy(alpha = 0.55f),
                    glowColor = cyan, glowColor2 = Color(0xFF7CFFB2),
                ),
            )
        } else {
            val blue = Color(0xFF0B5FFF)
            StylePalette(
                buildScheme(
                    dark = false,
                    primary = blue,
                    secondary = Color(0xFF00875A),
                    tertiary = Color(0xFFB25E00),
                    background = Color(0xFFEEF3FA),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFDCE6F5),
                    onSurface = Color(0xFF0A1A33),
                    onSurfaceVariant = Color(0xFF4A5E80),
                    outline = Color(0xFF9DB2D6),
                    error = Color(0xFFC62828),
                ),
                ThemeExtras(
                    style = style, dark = false,
                    borderWidth = 1.dp, borderColor = blue.copy(alpha = 0.5f),
                    glowColor = blue, glowColor2 = Color(0xFF00875A),
                ),
            )
        }
    }

    if (accent == null) return palette
    // The user's accent takes over `primary`; everything that follows it
    // (its text colour, its container, the style's glow) is re-derived.
    val scheme = palette.scheme.copy(
        primary = accent,
        onPrimary = contentOn(accent),
        primaryContainer = lerp(palette.scheme.surface, accent, 0.28f),
    )
    val extras = when (style) {
        ThemeStyle.NEOBRUTALISM -> if (dark) palette.extras.copy(shadowColor = accent) else palette.extras
        ThemeStyle.VAPORWAVE, ThemeStyle.SPACE -> palette.extras.copy(
            glowColor = accent,
            borderColor = accent.copy(alpha = 0.6f),
        )
        else -> palette.extras
    }
    return StylePalette(scheme, extras)
}

internal fun shapesFor(style: ThemeStyle): Shapes = when (style) {
    ThemeStyle.BITCHORD -> Shapes()
    ThemeStyle.NEOBRUTALISM -> Shapes(
        extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(12.dp),
    )
    ThemeStyle.WINDOWS95 -> Shapes(
        extraSmall = RoundedCornerShape(0.dp), small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(0.dp), large = RoundedCornerShape(0.dp),
        extraLarge = RoundedCornerShape(0.dp),
    )
    ThemeStyle.VAPORWAVE -> Shapes(
        extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
    ThemeStyle.SPACE -> Shapes(
        extraSmall = CutCornerShape(4.dp), small = CutCornerShape(6.dp),
        medium = CutCornerShape(8.dp), large = CutCornerShape(10.dp),
        extraLarge = CutCornerShape(14.dp),
    )
}

/**
 * [default] for the original style; the style's own corner family otherwise,
 * with the radius capped so a pill does not turn into a bevel.
 */
@Composable
fun themedShape(default: Shape, radius: Dp = 12.dp): Shape {
    val e = LocalThemeExtras.current
    return when (e.style) {
        ThemeStyle.BITCHORD, ThemeStyle.VAPORWAVE -> default
        ThemeStyle.NEOBRUTALISM -> RoundedCornerShape(minOf(radius, 12.dp))
        ThemeStyle.WINDOWS95 -> RectangleShape
        ThemeStyle.SPACE -> CutCornerShape(minOf(radius, 12.dp))
    }
}

/**
 * Whatever sits *behind* a surface: Neobrutalism's hard offset shadow and
 * Vaporwave's neon glow. Goes before `clip`, because a clip would cut it off.
 */
@Composable
fun Modifier.themedUnder(shape: Shape): Modifier {
    val e = LocalThemeExtras.current
    return when (e.style) {
        ThemeStyle.NEOBRUTALISM -> this.drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            translate(e.shadowOffset.toPx(), e.shadowOffset.toPx()) {
                drawOutline(outline, color = e.shadowColor)
            }
        }
        ThemeStyle.VAPORWAVE -> this.shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = e.glowColor,
            spotColor = e.glowColor,
        )
        else -> this
    }
}

/**
 * The edge drawn *over* a surface: Neobrutalism's thick outline, Windows 95's
 * 3D bevel, Vaporwave's neon rim, Space's outline with corner brackets. Goes
 * after the surface's own background.
 */
@Composable
fun Modifier.themedOver(shape: Shape): Modifier {
    val e = LocalThemeExtras.current
    return when (e.style) {
        ThemeStyle.BITCHORD -> this
        ThemeStyle.NEOBRUTALISM -> this.border(e.borderWidth, e.borderColor, shape)
        ThemeStyle.VAPORWAVE -> this.border(
            e.borderWidth,
            Brush.horizontalGradient(listOf(e.glowColor.copy(alpha = 0.85f), e.glowColor2.copy(alpha = 0.85f))),
            shape,
        )
        ThemeStyle.WINDOWS95 -> this.drawWithContent {
            drawContent()
            drawBevel(e, e.borderWidth.toPx() / 2f)
        }
        ThemeStyle.SPACE -> this
            .border(e.borderWidth, e.borderColor, shape)
            .drawWithContent {
                drawContent()
                drawBrackets(e.glowColor, 10.dp.toPx(), 1.5.dp.toPx())
            }
    }
}

/**
 * A plain box drawn in the style: [fill] behind, then the style's edge over it.
 * For the original style it only paints [fill], if given.
 */
@Composable
fun Modifier.themedFrame(shape: Shape, fill: Color? = null): Modifier {
    val e = LocalThemeExtras.current
    if (e.isDefault) return this
    val filled = if (fill == null) this else this.drawBehind {
        drawOutline(shape.createOutline(size, layoutDirection, this), color = fill)
    }
    return filled.themedUnder(shape).themedOver(shape)
}

/** Windows 95's two-step bevel. [t] is the width of one step. */
private fun DrawScope.drawBevel(e: ThemeExtras, t: Float) {
    val w = size.width
    val h = size.height
    if (w < t * 4 || h < t * 4) return
    // Outer ring: light top/left, dark bottom/right.
    drawRect(e.bevelOuterLight, Offset.Zero, Size(w, t))
    drawRect(e.bevelOuterLight, Offset.Zero, Size(t, h))
    drawRect(e.bevelOuterDark, Offset(0f, h - t), Size(w, t))
    drawRect(e.bevelOuterDark, Offset(w - t, 0f), Size(t, h))
    // Inner ring, one step in.
    drawRect(e.bevelInnerLight, Offset(t, t), Size(w - 2 * t, t))
    drawRect(e.bevelInnerLight, Offset(t, t), Size(t, h - 2 * t))
    drawRect(e.bevelInnerDark, Offset(t, h - 2 * t), Size(w - 2 * t, t))
    drawRect(e.bevelInnerDark, Offset(w - 2 * t, t), Size(t, h - 2 * t))
}

/** Space's four corner brackets. */
private fun DrawScope.drawBrackets(color: Color, length: Float, stroke: Float) {
    val w = size.width
    val h = size.height
    if (w < length * 3 || h < length * 3) return
    val s = stroke
    // top-left
    drawRect(color, Offset.Zero, Size(length, s))
    drawRect(color, Offset.Zero, Size(s, length))
    // top-right
    drawRect(color, Offset(w - length, 0f), Size(length, s))
    drawRect(color, Offset(w - s, 0f), Size(s, length))
    // bottom-left
    drawRect(color, Offset(0f, h - s), Size(length, s))
    drawRect(color, Offset(0f, h - length), Size(s, length))
    // bottom-right
    drawRect(color, Offset(w - length, h - s), Size(length, s))
    drawRect(color, Offset(w - s, h - length), Size(s, length))
}
