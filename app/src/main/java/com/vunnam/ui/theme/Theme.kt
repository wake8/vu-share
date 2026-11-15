package com.vunnam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

@Immutable
data class VuColors(
    val isLight: Boolean,
    val background: Color,
    val primaryCard: Color,
    val secondaryCard: Color,
    val elevatedSurface: Color,
    val inputBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val secondaryAccent: Color,
    val onSecondaryAccent: Color,
    val secondaryHue: Color,
    val border: Color,
    val divider: Color,
    val selectedSurface: Color,
    val selectedBorder: Color,
    val disabledSurface: Color,
    val disabledText: Color,
    val sendCardBg: Color,
    val sendCardBorder: Color,
    val sendCardTitle: Color,
    val sendCardSubtitle: Color,
    val sendCardIconBg: Color,
    val sendCardIconTint: Color,
    val receiveCardBg: Color,
    val receiveCardBorder: Color,
    val receiveCardTitle: Color,
    val receiveCardSubtitle: Color,
    val receiveCardIconBg: Color,
    val receiveCardIconTint: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val progressTrack: Color,
    val progressIndicator: Color,
    val switchCheckedThumb: Color,
    val switchCheckedTrack: Color,
    val switchUncheckedThumb: Color,
    val switchUncheckedTrack: Color,
    val switchUncheckedBorder: Color,
    val favoriteRed: Color
)

private fun contrastRatio(fg: Color, bg: Color): Double {
    val l1 = fg.luminance().toDouble()
    val l2 = bg.luminance().toDouble()
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
}

private fun accessibleOnColor(bg: Color): Color {
    return if (contrastRatio(Color.White, bg) >= 4.5) Color.White else Color(0xFF0B1320)
}

private fun ensureMinContrast(
    fg: Color,
    bg: Color,
    fallback: Color,
    minRatio: Double = 4.7
): Color {
    if (contrastRatio(fg, bg) >= minRatio) return fg
    var step = 0.1f
    while (step <= 1.0f) {
        val candidate = lerp(fg, fallback, step)
        if (contrastRatio(candidate, bg) >= minRatio) return candidate
        step += 0.1f
    }
    return fallback
}

fun getAccentColor(accentName: String, isLight: Boolean = false, isModernLight: Boolean = false): Color {
    return if (isLight) {
        when (accentName) {
            "VIOLET" -> LightAccentViolet
            "EMERALD" -> LightAccentEmerald
            "CORAL" -> LightAccentCoral
            "GOLD" -> LightAccentGold
            "ROSE" -> LightAccentRose
            "CYAN" -> if (isModernLight) LightAccentBlue else LightAccentCyan
            else -> LightAccentBlue
        }
    } else {
        when (accentName) {
            "VIOLET" -> ElectricViolet
            "EMERALD" -> AccentEmerald
            "CORAL" -> AccentCoral
            "GOLD" -> AccentGold
            "ROSE" -> AccentRose
            else -> ElectricCyan
        }
    }
}

private fun resolveEffectiveTheme(themePreference: String, systemIsDark: Boolean): String {
    return if (themePreference == "SYSTEM") {
        if (systemIsDark) "MIDNIGHT_BLUE" else "LIGHT"
    } else {
        themePreference
    }
}

fun buildVuColors(
    themePreference: String,
    accentName: String,
    systemIsDark: Boolean
): VuColors {
    val effective = resolveEffectiveTheme(themePreference, systemIsDark)

    if (effective == "LIGHT") {
        val accent = getAccentColor(accentName, isLight = true, isModernLight = true)
        val onAccent = accessibleOnColor(accent)
        val receiveTintBg = if (accentName == "CYAN") LightAccentSecondary else accent.copy(alpha = 0.14f).compositeOver(Color.White)
        val receiveTintBorder = if (accentName == "CYAN") Color(0xFFB6CEFA) else accent.copy(alpha = 0.38f).compositeOver(Color.White)
        val receiveDarkText = if (accentName == "CYAN") {
            LightOnAccentSecondary
        } else {
            ensureMinContrast(accent, receiveTintBg, LightTextPrimary, minRatio = 4.75)
        }
        val receiveSubText = ensureMinContrast(
            fg = receiveDarkText.copy(alpha = 0.88f).compositeOver(receiveTintBg),
            bg = receiveTintBg,
            fallback = LightTextPrimary,
            minRatio = 4.75
        )
        val sendSubColor = if (onAccent == Color.White) {
            ensureMinContrast(Color(0xFFE6EFFF), accent, Color.White, minRatio = 4.55)
        } else {
            onAccent.copy(alpha = 0.88f)
        }

        return VuColors(
            isLight = true,
            background = LightBackground,
            primaryCard = LightPrimaryCard,
            secondaryCard = LightSecondaryCard,
            elevatedSurface = LightElevatedSurface,
            inputBackground = LightInputBackground,
            primaryText = LightTextPrimary,
            secondaryText = LightTextSecondary,
            mutedText = LightTextSecondary,
            primaryAccent = accent,
            onPrimaryAccent = onAccent,
            secondaryAccent = receiveTintBg,
            onSecondaryAccent = receiveDarkText,
            secondaryHue = LightAccentViolet,
            border = LightCardBorder,
            divider = LightDivider,
            selectedSurface = receiveTintBg.copy(alpha = 0.72f).compositeOver(LightPrimaryCard),
            selectedBorder = accent,
            disabledSurface = LightDisabledSurface,
            disabledText = LightDisabledText,
            sendCardBg = accent,
            sendCardBorder = accent,
            sendCardTitle = onAccent,
            sendCardSubtitle = sendSubColor,
            sendCardIconBg = onAccent.copy(alpha = 0.22f),
            sendCardIconTint = onAccent,
            receiveCardBg = receiveTintBg,
            receiveCardBorder = receiveTintBorder,
            receiveCardTitle = receiveDarkText,
            receiveCardSubtitle = receiveSubText,
            receiveCardIconBg = accent.copy(alpha = 0.15f),
            receiveCardIconTint = receiveDarkText,
            success = SuccessGreenLightTheme,
            onSuccess = Color.White,
            successContainer = Color(0xFFD1FAE5),
            warning = WarningAmberLightTheme,
            warningContainer = Color(0xFFFEF3C7),
            error = ErrorRedLightTheme,
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            progressTrack = Color(0xFFDCE2EE),
            progressIndicator = accent,
            switchCheckedThumb = Color.White,
            switchCheckedTrack = accent,
            switchUncheckedThumb = Color(0xFF5E687A),
            switchUncheckedTrack = Color(0xFFDCE2EE),
            switchUncheckedBorder = Color(0xFF94A3B8),
            favoriteRed = Color(0xFFDC2626)
        )
    }

    if (effective == "MINIMAL_LIGHT") {
        val accent = getAccentColor(accentName, isLight = true, isModernLight = false)
        val onAccent = accessibleOnColor(accent)
        val receiveTintBg = accent.copy(alpha = 0.12f).compositeOver(Color.White)
        val receiveTintBorder = accent.copy(alpha = 0.35f).compositeOver(Color.White)
        val receiveDarkText = ensureMinContrast(accent, receiveTintBg, MinimalLightTextPrimary, minRatio = 4.75)
        val sendSubColor = if (onAccent == Color.White) {
            ensureMinContrast(Color(0xFFF1F5F9), accent, Color.White, minRatio = 4.55)
        } else {
            onAccent.copy(alpha = 0.88f)
        }

        return VuColors(
            isLight = true,
            background = MinimalLightBackground,
            primaryCard = MinimalLightPrimaryCard,
            secondaryCard = MinimalLightSecondaryCard,
            elevatedSurface = MinimalLightPrimaryCard,
            inputBackground = MinimalLightInputBg,
            primaryText = MinimalLightTextPrimary,
            secondaryText = MinimalLightTextSecondary,
            mutedText = MinimalLightTextSecondary,
            primaryAccent = accent,
            onPrimaryAccent = onAccent,
            secondaryAccent = receiveTintBg,
            onSecondaryAccent = receiveDarkText,
            secondaryHue = LightAccentViolet,
            border = MinimalLightBorder,
            divider = Color(0xFFE5EAF2),
            selectedSurface = receiveTintBg,
            selectedBorder = accent,
            disabledSurface = Color(0xFFE5E7EB),
            disabledText = Color(0xFF6B7280),
            sendCardBg = accent,
            sendCardBorder = accent,
            sendCardTitle = onAccent,
            sendCardSubtitle = sendSubColor,
            sendCardIconBg = onAccent.copy(alpha = 0.2f),
            sendCardIconTint = onAccent,
            receiveCardBg = receiveTintBg,
            receiveCardBorder = receiveTintBorder,
            receiveCardTitle = receiveDarkText,
            receiveCardSubtitle = MinimalLightTextSecondary,
            receiveCardIconBg = accent.copy(alpha = 0.14f),
            receiveCardIconTint = receiveDarkText,
            success = SuccessGreenLightTheme,
            onSuccess = Color.White,
            successContainer = Color(0xFFD1FAE5),
            warning = WarningAmberLightTheme,
            warningContainer = Color(0xFFFEF3C7),
            error = ErrorRedLightTheme,
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            progressTrack = Color(0xFFD8E0EC),
            progressIndicator = accent,
            switchCheckedThumb = Color.White,
            switchCheckedTrack = accent,
            switchUncheckedThumb = Color(0xFF4B5563),
            switchUncheckedTrack = Color(0xFFD9E0EA),
            switchUncheckedBorder = Color(0xFF94A3B8),
            favoriteRed = Color(0xFFDC2626)
        )
    }

    // Dark themes: DARK, MIDNIGHT_BLUE, AMOLED_BLACK, AURORA, EMERALD
    val baseAccent = if (effective == "EMERALD" && accentName == "CYAN") {
        AccentEmerald
    } else {
        getAccentColor(accentName, isLight = false)
    }
    val onAccent = accessibleOnColor(baseAccent)

    val bg: Color
    val pCard: Color
    val sCard: Color
    val inputBg: Color
    val border: Color
    val divider: Color
    val pText: Color
    val sText: Color
    val secondaryHue: Color

    when (effective) {
        "AMOLED_BLACK" -> {
            bg = AmoledBackground
            pCard = AmoledPrimaryCard
            sCard = AmoledSecondaryCard
            inputBg = AmoledInputBg
            border = AmoledBorder
            divider = Color(0xFF23252C)
            pText = AmoledTextPrimary
            sText = AmoledTextSecondary
            secondaryHue = ElectricViolet
        }
        "AURORA" -> {
            bg = AuroraBackground
            pCard = AuroraPrimaryCard
            sCard = AuroraSecondaryCard
            inputBg = AuroraInputBg
            border = AuroraBorder
            divider = Color(0xFF302957)
            pText = AuroraTextPrimary
            sText = AuroraTextSecondary
            secondaryHue = Color(0xFFC084FC)
        }
        "EMERALD" -> {
            bg = EmeraldBackground
            pCard = EmeraldPrimaryCard
            sCard = EmeraldSecondaryCard
            inputBg = EmeraldInputBg
            border = EmeraldBorder
            divider = Color(0xFF1C4A3D)
            pText = EmeraldTextPrimary
            sText = EmeraldTextSecondary
            secondaryHue = ElectricCyan
        }
        "DARK" -> {
            bg = NeutralDarkBackground
            pCard = NeutralDarkPrimaryCard
            sCard = NeutralDarkSecondaryCard
            inputBg = NeutralDarkInputBg
            border = NeutralDarkBorder
            divider = Color(0xFF2A313E)
            pText = NeutralDarkTextPrimary
            sText = NeutralDarkTextSecondary
            secondaryHue = ElectricViolet
        }
        else -> { // MIDNIGHT_BLUE
            bg = MidnightBackground
            pCard = MidnightPrimaryCard
            sCard = MidnightSecondaryCard
            inputBg = MidnightInputBg
            border = MidnightBorder
            divider = Color(0xFF222E4A)
            pText = MidnightTextPrimary
            sText = MidnightTextSecondary
            secondaryHue = ElectricViolet
        }
    }

    val sendBg = baseAccent.copy(alpha = 0.16f).compositeOver(pCard)
    val receiveBg = secondaryHue.copy(alpha = 0.18f).compositeOver(pCard)
    val selectedBg = baseAccent.copy(alpha = 0.14f).compositeOver(pCard)

    return VuColors(
        isLight = false,
        background = bg,
        primaryCard = pCard,
        secondaryCard = sCard,
        elevatedSurface = sCard,
        inputBackground = inputBg,
        primaryText = pText,
        secondaryText = sText,
        mutedText = sText,
        primaryAccent = baseAccent,
        onPrimaryAccent = onAccent,
        secondaryAccent = secondaryHue.copy(alpha = 0.22f).compositeOver(pCard),
        onSecondaryAccent = Color(0xFFE9D5FF),
        secondaryHue = secondaryHue,
        border = border,
        divider = divider,
        selectedSurface = selectedBg,
        selectedBorder = baseAccent,
        disabledSurface = Color(0xFF2A2F3A),
        disabledText = Color(0xFF8B95A5),
        sendCardBg = sendBg,
        sendCardBorder = baseAccent.copy(alpha = 0.78f),
        sendCardTitle = Color.White,
        sendCardSubtitle = baseAccent,
        sendCardIconBg = baseAccent,
        sendCardIconTint = onAccent,
        receiveCardBg = receiveBg,
        receiveCardBorder = secondaryHue.copy(alpha = 0.78f),
        receiveCardTitle = Color.White,
        receiveCardSubtitle = Color(0xFFE2C8FF),
        receiveCardIconBg = secondaryHue,
        receiveCardIconTint = accessibleOnColor(secondaryHue),
        success = Color(0xFF34D399),
        onSuccess = Color(0xFF042218),
        successContainer = Color(0xFF0F3D2F),
        warning = Color(0xFFFBBF24),
        warningContainer = Color(0xFF3F2B08),
        error = Color(0xFFF87171),
        onError = Color(0xFF2B0707),
        errorContainer = Color(0xFF45161B),
        progressTrack = border,
        progressIndicator = baseAccent,
        switchCheckedThumb = onAccent,
        switchCheckedTrack = baseAccent,
        switchUncheckedThumb = sText,
        switchUncheckedTrack = inputBg,
        switchUncheckedBorder = border,
        favoriteRed = Color(0xFFF87171)
    )
}

fun buildColorScheme(
    vu: VuColors
): ColorScheme {
    return if (vu.isLight) {
        lightColorScheme(
            primary = vu.primaryAccent,
            onPrimary = vu.onPrimaryAccent,
            primaryContainer = vu.secondaryAccent,
            onPrimaryContainer = vu.onSecondaryAccent,
            secondary = vu.secondaryHue,
            onSecondary = Color.White,
            secondaryContainer = vu.secondaryAccent,
            onSecondaryContainer = vu.onSecondaryAccent,
            tertiary = vu.success,
            onTertiary = vu.onSuccess,
            background = vu.background,
            onBackground = vu.primaryText,
            surface = vu.primaryCard,
            onSurface = vu.primaryText,
            surfaceVariant = vu.secondaryCard,
            onSurfaceVariant = vu.secondaryText,
            outline = vu.border,
            outlineVariant = vu.divider,
            error = vu.error,
            onError = vu.onError,
            errorContainer = vu.errorContainer
        )
    } else {
        darkColorScheme(
            primary = vu.primaryAccent,
            onPrimary = vu.onPrimaryAccent,
            primaryContainer = vu.selectedSurface,
            onPrimaryContainer = vu.primaryAccent,
            secondary = vu.secondaryHue,
            onSecondary = accessibleOnColor(vu.secondaryHue),
            secondaryContainer = vu.secondaryAccent,
            onSecondaryContainer = vu.onSecondaryAccent,
            tertiary = vu.success,
            onTertiary = vu.onSuccess,
            background = vu.background,
            onBackground = vu.primaryText,
            surface = vu.primaryCard,
            onSurface = vu.primaryText,
            surfaceVariant = vu.secondaryCard,
            onSurfaceVariant = vu.secondaryText,
            outline = vu.border,
            outlineVariant = vu.divider,
            error = vu.error,
            onError = vu.onError,
            errorContainer = vu.errorContainer
        )
    }
}

val LocalVuColors = staticCompositionLocalOf {
    buildVuColors("MIDNIGHT_BLUE", "CYAN", systemIsDark = true)
}

object VuShareTheme {
    val colors: VuColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVuColors.current
}

@Composable
fun VuShareTheme(
    themePreference: String = "MIDNIGHT_BLUE",
    customAccent: String = "CYAN",
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val vuColors = buildVuColors(themePreference, customAccent, systemIsDark)
    val colorScheme = buildColorScheme(vuColors)

    CompositionLocalProvider(LocalVuColors provides vuColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
