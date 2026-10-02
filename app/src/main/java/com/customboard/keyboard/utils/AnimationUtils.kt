package com.customboard.keyboard.utils

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/** Lightweight animations used by the keyboard (kept short to stay under 16 ms frames). */
object AnimationUtils {

    fun pressScale(view: View) {
        view.animate().cancel()
        view.animate()
            .scaleX(0.94f).scaleY(0.94f)
            .setDuration(55L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f)
                    .setDuration(75L)
                    .setInterpolator(OvershootInterpolator(1.4f))
                    .start()
            }
            .start()
    }

    fun fadeIn(view: View, duration: Long = 140L, onEnd: (() -> Unit)? = null) {
        view.alpha = 0f
        view.setVisible(true)
        view.animate().alpha(1f).setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onEnd?.invoke() }
            .start()
    }

    fun fadeOut(view: View, duration: Long = 120L, hide: Boolean = true, onEnd: (() -> Unit)? = null) {
        view.animate().alpha(0f).setDuration(duration)
            .withEndAction {
                if (hide) view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }

    fun slideUpIn(view: View, distancePx: Float = 24f.dp, duration: Long = 170L) {
        view.translationY = distancePx
        view.alpha = 0f
        view.setVisible(true)
        view.animate().translationY(0f).alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /** Animates an integer property (used for keyboard resize / one-handed transitions). */
    fun animateInt(from: Int, to: Int, duration: Long = 180L, onUpdate: (Int) -> Unit) {
        ValueAnimator.ofInt(from, to).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { onUpdate(it.animatedValue as Int) }
            start()
        }
    }
}
