package org.enterprisecams.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val colors = darkColorScheme(
    primary = Color(0xFF79DDF6), onPrimary = Color(0xFF002C39),
    primaryContainer = Color(0xFF123E50), onPrimaryContainer = Color(0xFFBCEEFF),
    secondary = Color(0xFFB8CADA), background = Color(0xFF080F18),
    surface = Color(0xFF101C2A), surfaceVariant = Color(0xFF1C2A38),
    onSurface = Color(0xFFEAF2FA), onSurfaceVariant = Color(0xFFB8C8D8),
    outline = Color(0xFF7F95A8), error = Color(0xFFFFB4AB),
)

@Composable
fun EnterpriseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
