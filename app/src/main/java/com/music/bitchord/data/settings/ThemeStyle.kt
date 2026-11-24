package com.music.bitchord.data.settings

/**
 * The look of the whole app — its "skin". Independent of [ThemeMode]: every
 * style has a light and a dark version, and follows the light/dark/system
 * choice like the original one does.
 */
enum class ThemeStyle {
    /** The original BitChord look. */
    BITCHORD,

    /** Strong colours, thick borders, hard shadows, big bold type. */
    NEOBRUTALISM,

    /** Title bars, 3D bevelled edges, grey panels. */
    WINDOWS95,

    /** Gradients, neon, retro grid and sun. */
    VAPORWAVE,

    /** Stars, coordinate grid, navigation-panel feel. */
    SPACE,
}

/**
 * Accent colours offered in Settings, as ARGB. `null` in the settings means
 * "the style's own accent".
 */
val ThemeAccents: List<Int> = listOf(
    0xFFFF3B6B.toInt(), // pink-red
    0xFFFF8A00.toInt(), // orange
    0xFFFFD60A.toInt(), // yellow
    0xFF30D158.toInt(), // green
    0xFF00C7BE.toInt(), // teal
    0xFF0A84FF.toInt(), // blue
    0xFF7C4DFF.toInt(), // violet
    0xFFFF5CE1.toInt(), // magenta
)
