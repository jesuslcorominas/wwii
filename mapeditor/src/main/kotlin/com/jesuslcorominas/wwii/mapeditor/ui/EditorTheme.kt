package com.jesuslcorominas.wwii.mapeditor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colours from DESIGN.md (dark "command post" UI around a paper map). */
object EditorColors {
    val land = Color(0xFFECE3C9)
    val sea = Color(0xFFA7C1CB)
    val lake = Color(0xFF7FA3B2)
    val river = Color(0xFF1F5F8B)
    val ink = Color(0xFF2B2414)
    val labelHalo = Color(0xF2F7F3E6)
    val mountain = Color(0xFF6E5C3A)
    val objective = Color(0xFFB07F1E)
    val grid = Color(0x992B2414)
    val canvasBackground = Color(0xFF0E1116)
    val selection = Color(0xFFD9A441)
    val hover = Color(0xFFF0C674)
    val issue = Color(0xFFE5484D)
}

private val scheme =
    darkColorScheme(
        primary = Color(0xFFD9A441),
        onPrimary = Color(0xFF1A1408),
        primaryContainer = Color(0xFF3A2F17),
        onPrimaryContainer = Color(0xFFF0C674),
        secondary = Color(0xFF9AA3AD),
        onSecondary = Color(0xFF12161C),
        secondaryContainer = Color(0xFF262E39),
        onSecondaryContainer = Color(0xFFE8E4D8),
        background = Color(0xFF12161C),
        onBackground = Color(0xFFE8E4D8),
        surface = Color(0xFF12161C),
        onSurface = Color(0xFFE8E4D8),
        surfaceVariant = Color(0xFF1C222B),
        onSurfaceVariant = Color(0xFF9AA3AD),
        surfaceContainer = Color(0xFF1C222B),
        surfaceContainerHigh = Color(0xFF262E39),
        outline = Color(0xFF6B7683),
        outlineVariant = Color(0xFF333D4A),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
    )

@Composable
fun EditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
