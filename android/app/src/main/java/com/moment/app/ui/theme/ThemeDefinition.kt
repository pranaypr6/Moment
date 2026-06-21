package com.moment.app.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemeDefinition(
    val id: String,
    val name: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val primaryAccent: Color,
    val textDeep: Color,
    val textMuted: Color,
    val surfaceSoft: Color,
    val isDark: Boolean = false
)

object Themes {
    val Blush = ThemeDefinition(
        id = "blush",
        name = "Blush",
        gradientStart = Color(0xFFFFF9F5), // SoftCream
        gradientEnd = Color(0xFFFADADD),   // RoseQuartz
        primaryAccent = Color(0xFFB56576), // HeartRed
        textDeep = Color(0xFF352F36),
        textMuted = Color(0xFF8B8089),
        surfaceSoft = Color(0xFFFFFFFF),
        isDark = false
    )

    val Midnight = ThemeDefinition(
        id = "midnight",
        name = "Midnight",
        gradientStart = Color(0xFF0F172A),
        gradientEnd = Color(0xFF1E293B),
        primaryAccent = Color(0xFFFACC15), // Glowing gold
        textDeep = Color(0xFFF8FAFC),
        textMuted = Color(0xFF94A3B8),
        surfaceSoft = Color(0xFF334155),
        isDark = true
    )

    val Matcha = ThemeDefinition(
        id = "matcha",
        name = "Matcha",
        gradientStart = Color(0xFFFBFBF9),
        gradientEnd = Color(0xFFE2E8DD),
        primaryAccent = Color(0xFF5A7247),
        textDeep = Color(0xFF2C3526),
        textMuted = Color(0xFF7E8C78),
        surfaceSoft = Color(0xFFFFFFFF),
        isDark = false
    )

    val Aura = ThemeDefinition(
        id = "aura",
        name = "Aura",
        gradientStart = Color(0xFFF3E8FF),
        gradientEnd = Color(0xFFFFEDD5),
        primaryAccent = Color(0xFF9333EA),
        textDeep = Color(0xFF4A044E),
        textMuted = Color(0xFF86198F),
        surfaceSoft = Color(0xFFFFFFFF),
        isDark = false
    )

    val All = listOf(Blush, Midnight, Matcha, Aura)

    fun getThemeById(id: String?): ThemeDefinition {
        return All.find { it.id == id } ?: Blush
    }
}
