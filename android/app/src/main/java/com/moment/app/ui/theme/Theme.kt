package com.moment.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowCompat

private val CoupleColorScheme = lightColorScheme(
    primary = HeartRed,
    onPrimary = White,
    primaryContainer = RoseQuartz,
    onPrimaryContainer = HeartRed,
    
    secondary = DeepMauve,
    onSecondary = White,
    secondaryContainer = WarmBeige,
    onSecondaryContainer = DeepMauve,
    
    background = SoftCream,
    onBackground = TextDeep,
    
    surface = White,
    onSurface = TextDeep,
    surfaceVariant = WarmBeige,
    onSurfaceVariant = TextMuted,
    
    outline = SoftRose.copy(alpha = 0.5f),
    error = ErrorSoft,
    onError = White
)

val LocalMomentTheme = staticCompositionLocalOf { Themes.Blush }

@Composable
fun MomentTheme(
    themeDefinition: ThemeDefinition = Themes.Blush,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeDefinition.isDark) {
        darkColorScheme(
            primary = themeDefinition.primaryAccent,
            onPrimary = themeDefinition.textDeep,
            background = themeDefinition.gradientStart,
            onBackground = themeDefinition.textDeep,
            surface = themeDefinition.surfaceSoft,
            onSurface = themeDefinition.textDeep,
            secondary = themeDefinition.primaryAccent,
            onSecondary = themeDefinition.textDeep
        )
    } else {
        lightColorScheme(
            primary = themeDefinition.primaryAccent,
            onPrimary = White,
            background = themeDefinition.gradientStart,
            onBackground = themeDefinition.textDeep,
            surface = themeDefinition.surfaceSoft,
            onSurface = themeDefinition.textDeep,
            secondary = themeDefinition.primaryAccent,
            onSecondary = White
        )
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) break
                currentContext = currentContext.baseContext
            }
            
            val window = (currentContext as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !themeDefinition.isDark
                controller.isAppearanceLightNavigationBars = !themeDefinition.isDark
            }
        }
    }

    CompositionLocalProvider(LocalMomentTheme provides themeDefinition) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
