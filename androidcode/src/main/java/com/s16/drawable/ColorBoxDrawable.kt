package com.s16.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import kotlin.math.round


class ColorBoxDrawable(private val context: Context, @ColorInt color: Int) : Drawable() {

    private val mBoxPaint = Paint().apply {
        setColor(color)
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return round(dp * density).toInt()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(bounds, mBoxPaint)
    }

    override fun setAlpha(alpha: Int) {
        mBoxPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mBoxPaint.colorFilter = cf
        invalidateSelf()
    }

    fun setColor(@ColorInt color: Int) {
        mBoxPaint.color = color
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return if (mBoxPaint.alpha < 255) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE
    }

    override fun getIntrinsicWidth(): Int {
        return dpToPx(46)
    }

    override fun getIntrinsicHeight(): Int {
        return dpToPx(30)
    }
}