package com.customboard.keyboard.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyType
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.ShiftState
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.theme.FontManager
import com.customboard.keyboard.theme.KeyBorderManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemePresets
import com.customboard.keyboard.utils.Constants
import com.customboard.keyboard.utils.KeyCodes
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.withAlpha
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Fully custom, Canvas rendered keyboard.
 *
 * Nothing from the deprecated `android.inputmethodservice.KeyboardView` is used: keys are
 * measured from a [KeyboardLayout] description and drawn directly, which keeps a frame under
 * 16 ms even with gesture trails and popups on screen.
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** A key together with the rectangle it occupies. */
    data class KeyPlacement(val key: Key, val rect: RectF) {
        val centerX: Float get() = rect.centerX()
        val centerY: Float get() = rect.centerY()
    }

    interface KeyboardListener {
        fun onKeyDown(key: Key)
        fun onKeyUp(key: Key)
        fun onKeyRepeat(key: Key)
        fun onKeyLongPress(key: Key): Boolean
        fun onPopupCharSelected(text: String)
        fun onPopupMove(x: Float, y: Float)
        fun onPopupRelease()
        fun onGestureTypingStarted()
        fun onGestureTypingFinished(points: List<PointF>, placements: List<KeyPlacement>)
        fun onSpaceSlide(deltaPx: Float)
        fun onSpaceSlideFinished()
        fun onSwipeDown()
        fun onSwipeUpOnKey(key: Key)
        fun onDeleteSwipe()
    }

    var listener: KeyboardListener? = null

    private val prefs = PreferencesManager.getInstance(context)
    private val borderManager = KeyBorderManager(context)

    private var layoutModel: KeyboardLayout? = null
    private var theme: ThemeColors = ThemePresets.LIGHT
    private val placements = mutableListOf<KeyPlacement>()
    private val iconCache = HashMap<Int, Drawable?>()

    private var shiftState: ShiftState = ShiftState.OFF
    private var spaceLabel: String = ""
    private var enterLabel: String = ""
    private var splitEnabled = false
    private var incognito = false

    // ----- paints -------------------------------------------------------
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val backgroundPaint = Paint()
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // ----- touch state --------------------------------------------------
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var pressedPlacement: KeyPlacement? = null
    private var downX = 0f
    private var downY = 0f
    private var lastSlideX = 0f
    private var longPressFired = false
    private var popupShowing = false
    private var slidingSpace = false
    private var gestureTyping = false
    private var ignoreUntilUp = false

    private val gesturePoints = mutableListOf<PointF>()
    private val gesturePath = Path()
    private val handler = Handler(Looper.getMainLooper())

    private val longPressRunnable = Runnable {
        val placement = pressedPlacement ?: return@Runnable
        longPressFired = true
        val handled = listener?.onKeyLongPress(placement.key) ?: false
        if (handled) popupShowing = true
    }

    private var repeatCount = 0
    private val repeatRunnable = object : Runnable {
        override fun run() {
            val placement = pressedPlacement ?: return
            if (!placement.key.repeatable) return
            repeatCount++
            listener?.onKeyRepeat(placement.key)
            val delay = if (repeatCount > 12) 22L else prefs.keyRepeatDelay.toLong()
            handler.postDelayed(this, delay)
        }
    }

    init {
        isFocusable = false
        isHapticFeedbackEnabled = true
        setWillNotDraw(false)
    }

    // ------------------------------------------------------------------
    //  Public API
    // ------------------------------------------------------------------

    fun setKeyboardLayout(layout: KeyboardLayout) {
        layoutModel = layout
        requestLayout()
        computePlacements(width, height)
        invalidate()
    }

    fun currentLayout(): KeyboardLayout? = layoutModel

    fun setTheme(newTheme: ThemeColors) {
        theme = newTheme
        iconCache.clear()
        invalidate()
    }

    fun setShiftState(state: ShiftState) {
        if (shiftState != state) {
            shiftState = state
            invalidate()
        }
    }

    fun setSpaceBarLabel(label: String) {
        if (spaceLabel != label) {
            spaceLabel = label
            invalidate()
        }
    }

    fun setEnterLabel(label: String) {
        if (enterLabel != label) {
            enterLabel = label
            invalidate()
        }
    }

    fun setSplitEnabled(enabled: Boolean) {
        if (splitEnabled != enabled) {
            splitEnabled = enabled
            computePlacements(width, height)
            invalidate()
        }
    }

    fun setIncognito(enabled: Boolean) {
        if (incognito != enabled) {
            incognito = enabled
            invalidate()
        }
    }

    fun placements(): List<KeyPlacement> = placements

    /** Finds the rectangle of the key currently under the finger (used by popups). */
    fun placementAt(x: Float, y: Float): KeyPlacement? = findKey(x, y)

    fun cancelAllInput() {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(repeatRunnable)
        pressedPlacement = null
        activePointerId = MotionEvent.INVALID_POINTER_ID
        gestureTyping = false
        slidingSpace = false
        popupShowing = false
        gesturePoints.clear()
        gesturePath.reset()
        invalidate()
    }

    // ------------------------------------------------------------------
    //  Measurement & placement
    // ------------------------------------------------------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computePlacements(w, h)
    }

    private fun computePlacements(width: Int, height: Int) {
        placements.clear()
        val layout = layoutModel ?: return
        if (width <= 0 || height <= 0) return

        val sidePadding = context.dpToPx(3f)
        val gapX = context.dpToPx(if (splitEnabled) 3f else 3.5f)
        val gapY = context.dpToPx(5f)
        val bottomPadding = context.dpToPx(prefs.bottomPaddingDp.toFloat())

        val usableHeight = height - bottomPadding
        val totalWeight = layout.totalHeightWeight.coerceAtLeast(1f)
        val rowHeight = (usableHeight - gapY * (layout.rowCount + 1)) / totalWeight

        var y = gapY
        for (row in layout.rows) {
            val h = rowHeight * row.heightWeight
            val keys = if (layout.isRtl) row.keys.reversed() else row.keys
            val rowWeight = row.totalWeight.coerceAtLeast(0.01f)
            val splitGap = if (splitEnabled) width * 0.14f else 0f
            val available = width - sidePadding * 2 - gapX * (keys.size - 1) - splitGap
            val unit = available / rowWeight

            var x = sidePadding
            val middleIndex = keys.size / 2
            keys.forEachIndexed { index, key ->
                if (splitEnabled && index == middleIndex) x += splitGap
                val w = unit * key.widthWeight
                placements += KeyPlacement(key, RectF(x, y, x + w, y + h))
                x += w + gapX
            }
            y += h + gapY
        }
    }

    // ------------------------------------------------------------------
    //  Drawing
    // ------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)

        val fontScale = prefs.keyFontSizePercent / 100f
        val baseTextSize = context.dpToPx(if (prefs.largeKeys) 22f else 19f) * fontScale
        labelPaint.typeface = FontManager.typeface(context)
        hintPaint.typeface = labelPaint.typeface
        hintPaint.textSize = context.dpToPx(9f) * fontScale
        borderPaint.strokeWidth = borderManager.borderWidthPx
        borderPaint.color = theme.border

        for (placement in placements) {
            drawKey(canvas, placement, baseTextSize)
        }

        if (gestureTyping && prefs.gestureTrail && gesturePoints.size > 1) {
            drawGestureTrail(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        if (theme.hasGradient && theme.background != theme.backgroundEnd) {
            backgroundPaint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                theme.background, theme.backgroundEnd, Shader.TileMode.CLAMP
            )
        } else {
            backgroundPaint.shader = null
            backgroundPaint.color = theme.background
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (incognito) {
            backgroundPaint.shader = null
            backgroundPaint.color = Color.parseColor("#7C4DFF").withAlpha(26)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        }
    }

    private fun drawKey(canvas: Canvas, placement: KeyPlacement, baseTextSize: Float) {
        val key = placement.key
        val rect = placement.rect
        val pressed = pressedPlacement === placement && !gestureTyping
        val radius = borderManager.cornerRadiusPx(rect.height())

        if (borderManager.drawsKeyBackground()) {
            keyPaint.color = when {
                pressed -> theme.keyPressed
                key.type == KeyType.ENTER -> theme.keyAccent
                key.type == KeyType.CHARACTER || key.type == KeyType.SPACE -> theme.keyBackground
                else -> theme.keySpecial
            }
            canvas.drawRoundRect(rect, radius, radius, keyPaint)
            if (borderManager.showBorders) {
                canvas.drawRoundRect(rect, radius, radius, borderPaint)
            }
        } else if (pressed) {
            keyPaint.color = theme.keyPressed
            canvas.drawRoundRect(rect, radius, radius, keyPaint)
        }

        val tint = when {
            key.type == KeyType.ENTER -> theme.keyAccentText
            key.type == KeyType.CHARACTER -> theme.keyText
            else -> theme.keyText
        }

        // Icon keys
        if (key.iconRes != 0 && !(key.type == KeyType.ENTER && enterLabel.isNotEmpty())) {
            val iconRes = when {
                key.code == KeyCodes.SHIFT && shiftState == ShiftState.LOCKED ->
                    com.customboard.keyboard.R.drawable.ic_shift_locked
                else -> key.iconRes
            }
            drawIcon(canvas, iconRes, rect, tint, shiftActive = key.code == KeyCodes.SHIFT &&
                shiftState != ShiftState.OFF)
            return
        }

        // Space bar: show the language name
        if (key.type == KeyType.SPACE) {
            if (spaceLabel.isNotEmpty()) {
                labelPaint.color = theme.keySecondaryText
                labelPaint.textSize = baseTextSize * 0.62f
                canvas.drawText(
                    spaceLabel, rect.centerX(),
                    rect.centerY() + labelPaint.textSize / 3f, labelPaint
                )
            } else {
                keyPaint.color = theme.keySecondaryText.withAlpha(120)
                val lineWidth = rect.width() * 0.3f
                canvas.drawRoundRect(
                    RectF(
                        rect.centerX() - lineWidth / 2, rect.centerY() - context.dpToPx(1f),
                        rect.centerX() + lineWidth / 2, rect.centerY() + context.dpToPx(1f)
                    ), context.dpToPx(1f), context.dpToPx(1f), keyPaint
                )
            }
            return
        }

        val label = displayLabel(key)
        if (label.isNotEmpty()) {
            labelPaint.color = tint
            labelPaint.textSize = when {
                key.type == KeyType.MODIFIER -> baseTextSize * 0.72f
                key.type == KeyType.ENTER -> baseTextSize * 0.7f
                label.length > 2 -> baseTextSize * 0.72f
                else -> baseTextSize
            }
            val baseline = rect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(label, rect.centerX(), baseline, labelPaint)
        }

        val hint = key.hint
        if (!hint.isNullOrEmpty() && rect.height() > context.dpToPx(34f)) {
            hintPaint.color = theme.keySecondaryText.withAlpha(185)
            canvas.drawText(
                hint,
                rect.right - context.dpToPx(9f),
                rect.top + hintPaint.textSize + context.dpToPx(3f),
                hintPaint
            )
        }
    }

    private fun displayLabel(key: Key): String {
        if (key.type == KeyType.ENTER && enterLabel.isNotEmpty()) return enterLabel
        if (!key.isLetter) return key.label
        val uppercase = shiftState.isUppercase && (layoutModel?.supportsShift != false)
        return if (uppercase) key.label.uppercase() else key.label
    }

    private fun drawIcon(
        canvas: Canvas,
        iconRes: Int,
        rect: RectF,
        tint: Int,
        shiftActive: Boolean = false
    ) {
        val drawable = iconCache.getOrPut(iconRes) {
            ContextCompat.getDrawable(context, iconRes)?.mutate()
        } ?: return
        val size = (minOf(rect.width(), rect.height()) * 0.44f)
            .coerceIn(context.dpToPx(16f), context.dpToPx(26f))
        val left = (rect.centerX() - size / 2).toInt()
        val top = (rect.centerY() - size / 2).toInt()
        drawable.setBounds(left, top, (left + size).toInt(), (top + size).toInt())
        DrawableCompat.setTint(drawable, if (shiftActive) theme.accent else tint)
        drawable.draw(canvas)
    }

    private fun drawGestureTrail(canvas: Canvas) {
        gesturePath.reset()
        val visible = if (gesturePoints.size > 48) {
            gesturePoints.subList(gesturePoints.size - 48, gesturePoints.size)
        } else {
            gesturePoints
        }
        gesturePath.moveTo(visible.first().x, visible.first().y)
        for (i in 1 until visible.size) {
            val prev = visible[i - 1]
            val cur = visible[i]
            gesturePath.quadTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
        }
        trailPaint.strokeWidth = context.dpToPx(5f)
        trailPaint.color = theme.gestureTrail.withAlpha(210)
        canvas.drawPath(gesturePath, trailPaint)
    }

    // ------------------------------------------------------------------
    //  Touch handling
    // ------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                handleDown(event.x, event.y)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Fast two-thumb typing: commit the previous key immediately.
                val index = event.actionIndex
                finishPress(commit = true)
                activePointerId = event.getPointerId(index)
                handleDown(event.getX(index), event.getY(index))
            }

            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index >= 0) handleMove(event.getX(index), event.getY(index))
            }

            MotionEvent.ACTION_UP -> {
                finishPress(commit = true)
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                if (event.getPointerId(index) == activePointerId) {
                    finishPress(commit = true)
                    // Hand control to a remaining finger if there is one.
                    val next = (0 until event.pointerCount).firstOrNull { it != index }
                    activePointerId = next?.let { event.getPointerId(it) }
                        ?: MotionEvent.INVALID_POINTER_ID
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                finishPress(commit = false)
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
        return true
    }

    private fun handleDown(x: Float, y: Float) {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(repeatRunnable)
        longPressFired = false
        popupShowing = false
        slidingSpace = false
        gestureTyping = false
        ignoreUntilUp = false
        repeatCount = 0
        downX = x
        downY = y
        lastSlideX = x

        val placement = findKey(x, y) ?: return
        pressedPlacement = placement
        listener?.onKeyDown(placement.key)
        invalidate()

        handler.postDelayed(longPressRunnable, prefs.longPressDelay.toLong())
        if (placement.key.repeatable) {
            handler.postDelayed(repeatRunnable, 380L)
        }
    }

    private fun handleMove(x: Float, y: Float) {
        val placement = pressedPlacement ?: return
        val dx = x - downX
        val dy = y - downY
        val distance = hypot(dx, dy)
        val threshold = context.dpToPx(Constants.GESTURE_MIN_DISTANCE_DP)

        if (popupShowing) {
            // The long-press popup is not touchable, so forward the finger to it.
            listener?.onPopupMove(x, y)
            return
        }

        // Space bar sliding -> cursor control
        if (placement.key.type == KeyType.SPACE && prefs.spaceCursor && abs(dx) > threshold * 0.6f) {
            slidingSpace = true
            handler.removeCallbacks(longPressRunnable)
            val delta = x - lastSlideX
            if (abs(delta) > context.dpToPx(9f)) {
                listener?.onSpaceSlide(delta)
                lastSlideX = x
            }
            return
        }

        // Backspace swipe -> delete whole words
        if (placement.key.type == KeyType.DELETE && prefs.swipeDeleteWord && dx < -threshold) {
            handler.removeCallbacks(longPressRunnable)
            handler.removeCallbacks(repeatRunnable)
            if (!ignoreUntilUp) {
                ignoreUntilUp = true
                listener?.onDeleteSwipe()
            }
            return
        }

        // Swipe down on the keyboard -> hide
        if (prefs.swipeDownToHide && dy > threshold * 2.4f && abs(dx) < threshold) {
            handler.removeCallbacks(longPressRunnable)
            handler.removeCallbacks(repeatRunnable)
            if (!ignoreUntilUp) {
                ignoreUntilUp = true
                listener?.onSwipeDown()
                finishPress(commit = false)
            }
            return
        }

        // Glide typing
        if (prefs.gestureTyping && placement.key.isLetter && !gestureTyping && distance > threshold) {
            val target = findKey(x, y)
            if (target != null && target !== placement) {
                gestureTyping = true
                handler.removeCallbacks(longPressRunnable)
                handler.removeCallbacks(repeatRunnable)
                gesturePoints.clear()
                gesturePoints += PointF(downX, downY)
                listener?.onGestureTypingStarted()
            }
        }

        if (gestureTyping) {
            gesturePoints += PointF(x, y)
            invalidate()
            return
        }

        // Sliding to a neighbouring key before release simply moves the pressed key.
        val target = findKey(x, y)
        if (target != null && target !== placement && distance > context.dpToPx(10f)) {
            handler.removeCallbacks(longPressRunnable)
            handler.removeCallbacks(repeatRunnable)
            pressedPlacement = target
            downX = x
            downY = y
            listener?.onKeyDown(target.key)
            handler.postDelayed(longPressRunnable, prefs.longPressDelay.toLong())
            invalidate()
        }
    }

    private fun finishPress(commit: Boolean) {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(repeatRunnable)
        val placement = pressedPlacement
        pressedPlacement = null

        if (gestureTyping) {
            gestureTyping = false
            val points = gesturePoints.toList()
            gesturePoints.clear()
            gesturePath.reset()
            invalidate()
            if (commit && points.size > 2) {
                listener?.onGestureTypingFinished(points, placements.toList())
            }
            return
        }

        if (slidingSpace) {
            slidingSpace = false
            listener?.onSpaceSlideFinished()
            invalidate()
            return
        }

        invalidate()
        if (placement == null || !commit || ignoreUntilUp) return

        if (popupShowing) {
            popupShowing = false
            listener?.onPopupRelease()
            return
        }

        // Swipe up on a key inserts its first alternate character.
        if (prefs.swipeUpForSymbols && placement.key.popupKeys.isNotEmpty() &&
            downY - lastTouchY() > context.dpToPx(Constants.SWIPE_THRESHOLD_DP)
        ) {
            listener?.onSwipeUpOnKey(placement.key)
            return
        }

        if (longPressFired && placement.key.repeatable) return
        if (repeatCount > 0) return
        listener?.onKeyUp(placement.key)
    }

    private var lastY = 0f

    private fun lastTouchY(): Float = lastY

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        lastY = event.y
        return super.dispatchTouchEvent(event)
    }

    private fun findKey(x: Float, y: Float): KeyPlacement? {
        var best: KeyPlacement? = null
        var bestDistance = Float.MAX_VALUE
        for (placement in placements) {
            if (placement.rect.contains(x, y)) return placement
            val dx = x - placement.centerX
            val dy = y - placement.centerY
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = placement
            }
        }
        // Only snap to a nearby key when the touch is inside the keyboard area.
        return if (y in 0f..height.toFloat()) best else null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}
