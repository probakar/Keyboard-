package com.customboard.keyboard.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.google.mlkit.vision.digitalink.recognition.Ink

/** Touch-and-stylus drawing surface that keeps timed strokes for digital-ink recognition. */
class HandwritingCanvasView(context: Context) : View(context) {

    var onInkChanged: (() -> Unit)? = null

    private data class TimedPoint(val inkPoint: Ink.Point, val x: Float, val y: Float)

    private val completedStrokes = mutableListOf<List<TimedPoint>>()
    private var activeStroke: MutableList<TimedPoint>? = null
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val path = Path()
    private var theme: ThemeColors = ThemeManager.getInstance(context).current

    init {
        isFocusable = true
        contentDescription = "Handwriting input area"
        setBackgroundColor(theme.keyBackground)
        setTheme(theme)
    }

    fun setTheme(theme: ThemeColors) {
        this.theme = theme
        strokePaint.color = theme.accent
        strokePaint.strokeWidth = context.dpToPx(3.2f)
        guidePaint.color = theme.keySecondaryText and 0x33FFFFFF
        guidePaint.strokeWidth = context.dpToPx(1f)
        setBackgroundColor(theme.keyBackground)
        invalidate()
    }

    fun clear(notify: Boolean = true) {
        completedStrokes.clear()
        activeStroke = null
        invalidate()
        if (notify) onInkChanged?.invoke()
    }

    fun hasInk(): Boolean = completedStrokes.isNotEmpty() || !activeStroke.isNullOrEmpty()

    fun buildInk(): Ink? {
        if (completedStrokes.isEmpty()) return null
        val inkBuilder = Ink.builder()
        completedStrokes.forEach { samples ->
            if (samples.isNotEmpty()) {
                val strokeBuilder = Ink.Stroke.builder()
                samples.forEach { strokeBuilder.addPoint(it.inkPoint) }
                inkBuilder.addStroke(strokeBuilder.build())
            }
        }
        return inkBuilder.build()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val guideY = height - context.dpToPx(18f)
        canvas.drawLine(
            context.dpToPx(10f), guideY, width - context.dpToPx(10f), guideY, guidePaint
        )
        completedStrokes.forEach { stroke -> drawStroke(canvas, stroke) }
        activeStroke?.let { drawStroke(canvas, it) }
    }

    private fun drawStroke(canvas: Canvas, stroke: List<TimedPoint>) {
        if (stroke.isEmpty()) return
        path.reset()
        path.moveTo(stroke.first().x, stroke.first().y)
        for (index in 1 until stroke.size) {
            path.lineTo(stroke[index].x, stroke[index].y)
        }
        if (stroke.size == 1) {
            val point = stroke.first()
            canvas.drawCircle(point.x, point.y, strokePaint.strokeWidth / 2f, strokePaint)
        } else {
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activeStroke = mutableListOf()
                appendPoint(event.x, event.y, event.eventTime)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.historySize) {
                    appendPoint(event.getHistoricalX(index), event.getHistoricalY(index), event.getHistoricalEventTime(index))
                }
                appendPoint(event.x, event.y, event.eventTime)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                appendPoint(event.x, event.y, event.eventTime)
                activeStroke?.takeIf { it.isNotEmpty() }?.let(completedStrokes::add)
                activeStroke = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                onInkChanged?.invoke()
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activeStroke = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun appendPoint(x: Float, y: Float, time: Long) {
        val stroke = activeStroke ?: return
        if (stroke.lastOrNull()?.let { kotlin.math.abs(it.x - x) < MIN_POINT_DISTANCE && kotlin.math.abs(it.y - y) < MIN_POINT_DISTANCE } == true) {
            return
        }
        stroke += TimedPoint(Ink.Point.create(x, y, time), x, y)
    }

    companion object {
        private const val MIN_POINT_DISTANCE = 0.7f
    }
}
