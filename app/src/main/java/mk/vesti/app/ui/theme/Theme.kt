package mk.vesti.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mk.vesti.core.Category

private val Red = Color(0xFFD62828)
private val RedDark = Color(0xFFFF8A80)
private val Sun = Color(0xFFFFB703)

private val LightColors = lightColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF775652),
    tertiary = Sun,
    background = Color(0xFFFAF8F7),
    surface = Color(0xFFFAF8F7),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF2EEED),
    surfaceContainerHigh = Color(0xFFECE7E6),
    onSurfaceVariant = Color(0xFF6B6260),
)

private val DarkColors = darkColorScheme(
    primary = RedDark,
    onPrimary = Color(0xFF690003),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD5),
    tertiary = Sun,
    background = Color(0xFF121010),
    surface = Color(0xFF121010),
    surfaceContainerLow = Color(0xFF1C1918),
    surfaceContainer = Color(0xFF221E1D),
    surfaceContainerHigh = Color(0xFF2C2827),
    onSurfaceVariant = Color(0xFFB8AEAC),
)

private val base = Typography()

val VestiTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 20.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
)

val VestiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

/** Accent per category, used on pills so sections are recognisable at a glance. */
fun Category.accent(): Color = when (this) {
    Category.MAKEDONIJA -> Color(0xFFD62828)
    Category.SVET -> Color(0xFF2563EB)
    Category.EKONOMIJA -> Color(0xFF059669)
    Category.HRONIKA -> Color(0xFF7C3AED)
    Category.SPORT -> Color(0xFFEA580C)
    Category.KULTURA -> Color(0xFFDB2777)
    Category.TEHNOLOGIJA -> Color(0xFF0891B2)
    Category.ZABAVA -> Color(0xFFCA8A04)
}

@Composable
fun VestiTheme(
    dark: Boolean = isSystemInDarkTheme(),
    /** Material You wallpaper colours (Android 12+) instead of the red brand palette. */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = VestiTypography, shapes = VestiShapes, content = content)
}
