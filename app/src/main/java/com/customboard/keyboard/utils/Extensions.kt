package com.customboard.keyboard.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

val Float.sp: Float
    get() = this * Resources.getSystem().displayMetrics.scaledDensity

fun Context.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.spToPx(sp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

fun View.visible() {
    if (visibility != View.VISIBLE) visibility = View.VISIBLE
}

fun View.gone() {
    if (visibility != View.GONE) visibility = View.GONE
}

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun ViewGroup.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}

/** Blend [this] colour with [other] by [ratio] (0 = this, 1 = other). */
@ColorInt
fun Int.blendWith(@ColorInt other: Int, ratio: Float): Int {
    val inverse = 1f - ratio
    return Color.argb(
        (Color.alpha(this) * inverse + Color.alpha(other) * ratio).roundToInt(),
        (Color.red(this) * inverse + Color.red(other) * ratio).roundToInt(),
        (Color.green(this) * inverse + Color.green(other) * ratio).roundToInt(),
        (Color.blue(this) * inverse + Color.blue(other) * ratio).roundToInt()
    )
}

@ColorInt
fun Int.withAlpha(alpha: Int): Int =
    Color.argb(alpha.coerceIn(0, 255), Color.red(this), Color.green(this), Color.blue(this))

@ColorInt
fun Int.lighten(amount: Float = 0.12f): Int = blendWith(Color.WHITE, amount.coerceIn(0f, 1f))

@ColorInt
fun Int.darken(amount: Float = 0.12f): Int = blendWith(Color.BLACK, amount.coerceIn(0f, 1f))

/** Perceived luminance, 0 (black) .. 1 (white). */
fun Int.luminance(): Float =
    (0.299f * Color.red(this) + 0.587f * Color.green(this) + 0.114f * Color.blue(this)) / 255f

fun Int.isDarkColor(): Boolean = luminance() < 0.5f

/** Returns black or white, whichever contrasts better with the receiver. */
@ColorInt
fun Int.contrastingTextColor(): Int = if (isDarkColor()) Color.WHITE else Color.parseColor("#1F1F1F")

fun String.truncate(maxLength: Int): String =
    if (length <= maxLength) this else substring(0, max(0, maxLength - 1)) + "…"

fun String.isUrl(): Boolean {
    val t = trim()
    return t.startsWith("http://", true) || t.startsWith("https://", true) ||
        t.startsWith("www.", true) || Regex("^[\\w.-]+\\.(com|net|org|io|dev|co|pk|in|uk)(/.*)?$")
        .matches(t)
}

fun String.isEmailAddress(): Boolean =
    Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$").matches(trim())

fun String.isPhoneNumber(): Boolean =
    Regex("^[+]?[\\d\\s()-]{7,20}$").matches(trim()) && count { it.isDigit() } >= 7

fun String.isNumeric(): Boolean = trim().isNotEmpty() && trim().all { it.isDigit() }

fun CharSequence.lastWord(): String {
    var end = length
    while (end > 0 && this[end - 1].isWhitespace()) end--
    var start = end
    while (start > 0 && !this[start - 1].isWhitespace()) start--
    return subSequence(start, end).toString()
}

fun CharSequence.wordCount(): Int =
    trim().split(Regex("\\s+")).count { it.isNotBlank() }

fun Float.clamp(minValue: Float, maxValue: Float): Float = min(max(this, minValue), maxValue)

fun Int.clamp(minValue: Int, maxValue: Int): Int = min(max(this, minValue), maxValue)
