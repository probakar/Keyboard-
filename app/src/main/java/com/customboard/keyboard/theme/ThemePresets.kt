package com.customboard.keyboard.theme

import android.graphics.Color

/** The built-in theme catalogue (Gboard-style light/dark plus colourful presets). */
object ThemePresets {

    const val ID_LIGHT = "light"
    const val ID_DARK = "dark"
    const val ID_AMOLED = "amoled"
    const val ID_MATERIAL_YOU = "material_you"
    const val ID_CUSTOM = "custom"

    val LIGHT = ThemeColors(
        id = ID_LIGHT,
        displayName = "Light",
        isDark = false,
        background = Color.parseColor("#F1F3F4"),
        backgroundEnd = Color.parseColor("#F1F3F4"),
        keyBackground = Color.parseColor("#FFFFFF"),
        keyPressed = Color.parseColor("#DADCE0"),
        keySpecial = Color.parseColor("#DADCE0"),
        keyAccent = Color.parseColor("#1A73E8"),
        keyText = Color.parseColor("#1F1F1F"),
        keySecondaryText = Color.parseColor("#5F6368"),
        keyAccentText = Color.parseColor("#FFFFFF"),
        popupBackground = Color.parseColor("#FFFFFF"),
        popupText = Color.parseColor("#1F1F1F"),
        suggestionText = Color.parseColor("#1F1F1F"),
        suggestionHighlight = Color.parseColor("#1A73E8"),
        border = Color.parseColor("#DADCE0"),
        accent = Color.parseColor("#1A73E8"),
        gestureTrail = Color.parseColor("#1A73E8")
    )

    val DARK = ThemeColors(
        id = ID_DARK,
        displayName = "Dark",
        isDark = true,
        background = Color.parseColor("#202124"),
        backgroundEnd = Color.parseColor("#202124"),
        keyBackground = Color.parseColor("#3C4043"),
        keyPressed = Color.parseColor("#5F6368"),
        keySpecial = Color.parseColor("#282A2D"),
        keyAccent = Color.parseColor("#8AB4F8"),
        keyText = Color.parseColor("#E8EAED"),
        keySecondaryText = Color.parseColor("#9AA0A6"),
        keyAccentText = Color.parseColor("#202124"),
        popupBackground = Color.parseColor("#4A4D51"),
        popupText = Color.parseColor("#E8EAED"),
        suggestionText = Color.parseColor("#E8EAED"),
        suggestionHighlight = Color.parseColor("#8AB4F8"),
        border = Color.parseColor("#5F6368"),
        accent = Color.parseColor("#8AB4F8"),
        gestureTrail = Color.parseColor("#8AB4F8")
    )

    val AMOLED = DARK.copy(
        id = ID_AMOLED,
        displayName = "AMOLED black",
        background = Color.BLACK,
        backgroundEnd = Color.BLACK,
        keyBackground = Color.parseColor("#151515"),
        keyPressed = Color.parseColor("#2A2A2A"),
        keySpecial = Color.parseColor("#0A0A0A"),
        popupBackground = Color.parseColor("#1C1C1C"),
        border = Color.parseColor("#2A2A2A")
    )

    val BLUE = ThemeColors.fromSeeds(
        "blue", "Blue",
        background = Color.parseColor("#0B2A5B"),
        keyBackground = Color.parseColor("#17407F"),
        accent = Color.parseColor("#8AB4F8"),
        keyText = Color.WHITE
    )

    val GREEN = ThemeColors.fromSeeds(
        "green", "Green",
        background = Color.parseColor("#0C2B18"),
        keyBackground = Color.parseColor("#17492A"),
        accent = Color.parseColor("#81C995"),
        keyText = Color.WHITE
    )

    val PURPLE = ThemeColors.fromSeeds(
        "purple", "Purple",
        background = Color.parseColor("#241339"),
        keyBackground = Color.parseColor("#3C2259"),
        accent = Color.parseColor("#D7AEFB"),
        keyText = Color.WHITE
    )

    val RED = ThemeColors.fromSeeds(
        "red", "Red",
        background = Color.parseColor("#3A1210"),
        keyBackground = Color.parseColor("#5C1D1A"),
        accent = Color.parseColor("#F28B82"),
        keyText = Color.WHITE
    )

    val OCEAN = ThemeColors.fromSeeds(
        "ocean", "Ocean",
        background = Color.parseColor("#04303A"),
        keyBackground = Color.parseColor("#0A4C5C"),
        accent = Color.parseColor("#4DD0E1"),
        keyText = Color.WHITE,
        backgroundEnd = Color.parseColor("#06505F"),
        hasGradient = true
    )

    val SUNSET = ThemeColors.fromSeeds(
        "sunset", "Sunset",
        background = Color.parseColor("#FF6F3C"),
        keyBackground = Color.parseColor("#FF8A5B"),
        accent = Color.parseColor("#FFF3E0"),
        keyText = Color.WHITE,
        backgroundEnd = Color.parseColor("#C2185B"),
        hasGradient = true
    )

    val FOREST = ThemeColors.fromSeeds(
        "forest", "Forest",
        background = Color.parseColor("#1B3A2B"),
        keyBackground = Color.parseColor("#2E5C43"),
        accent = Color.parseColor("#A8D5BA"),
        keyText = Color.WHITE,
        backgroundEnd = Color.parseColor("#14281E"),
        hasGradient = true
    )

    val CANDY = ThemeColors.fromSeeds(
        "candy", "Candy",
        background = Color.parseColor("#FDE7F1"),
        keyBackground = Color.parseColor("#FFFFFF"),
        accent = Color.parseColor("#EC407A"),
        keyText = Color.parseColor("#5A1136")
    )

    val MONO = ThemeColors.fromSeeds(
        "mono", "Monochrome",
        background = Color.parseColor("#E0E0E0"),
        keyBackground = Color.parseColor("#FAFAFA"),
        accent = Color.parseColor("#424242"),
        keyText = Color.parseColor("#212121")
    )

    val MIDNIGHT = ThemeColors.fromSeeds(
        "midnight", "Midnight blue",
        background = Color.parseColor("#0D1B2A"),
        keyBackground = Color.parseColor("#1B263B"),
        accent = Color.parseColor("#778DA9"),
        keyText = Color.parseColor("#E0E1DD"),
        backgroundEnd = Color.parseColor("#060D14"),
        hasGradient = true
    )

    /** Ordered list shown in the theme picker. */
    val ALL: List<ThemeColors> = listOf(
        LIGHT, DARK, AMOLED, BLUE, GREEN, PURPLE, RED, OCEAN, SUNSET, FOREST, CANDY, MONO, MIDNIGHT
    )

    fun byId(id: String): ThemeColors? = ALL.firstOrNull { it.id == id }
}
