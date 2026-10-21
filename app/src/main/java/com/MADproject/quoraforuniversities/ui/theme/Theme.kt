package com.MADproject.quoraforuniversities.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val CampusLight = lightColorScheme(
    primary = Forest, onPrimary = Color.White,
    primaryContainer = ForestSoft, onPrimaryContainer = Forest,
    secondary = Color(0xFF386674), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F0F2), onSecondaryContainer = Color(0xFF204D59),
    tertiary = Color(0xFF8B4939), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8E9E2), onTertiaryContainer = Color(0xFF6C3629),
    background = Paper, onBackground = Ink, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFEEF3F0), onSurfaceVariant = Slate,
    surfaceContainer = Color(0xFFF0F4F2), surfaceContainerLow = Color(0xFFF8FAF9),
    outline = Color(0xFF7A8981), outlineVariant = Line
)

private val CampusDark = darkColorScheme(
    primary = Color(0xFF9CD8BC), onPrimary = Color(0xFF003827),
    primaryContainer = Color(0xFF174C3B), onPrimaryContainer = Color(0xFFBCEBD5),
    background = Color(0xFF141B18), surface = Color(0xFF1D2621),
    onBackground = Color(0xFFE5ECE7), onSurface = Color(0xFFE5ECE7),
    onSurfaceVariant = Color(0xFFB3C3B9), outlineVariant = Color(0xFF354A3E)
)

@Composable
fun CampusQnATheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) CampusDark else CampusLight, typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp)),
        content = content)
}
