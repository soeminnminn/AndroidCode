package com.s16.drawable

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt


class CircleDrawableWithShadow(@ColorInt backgroundColor: Int, light: Boolean) : Drawable() {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        color = backgroundColor
    }

    private val mShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.FILL
        color = SHADOW_COLOR
    }

    private val mShadowColors = if (light) SHADOW_COLORS_LIGHT else SHADOW_COLORS

    override fun draw(canvas: Canvas) {
        val saveCount = canvas.save()

        val shadowPaint = mShadowPaint
        val bounds = RectF()
        bounds.set(getBounds())

        for (i in SHADOW_PADDING.indices) {
            shadowPaint.color = mShadowColors[i]
            val padding = SHADOW_PADDING[i]
            bounds.bottom -= (padding * 2).toFloat()
            bounds.right -= (padding * 2).toFloat()
            canvas.translate(padding.toFloat(), padding.toFloat())
            canvas.drawOval(bounds, shadowPaint)
        }

        canvas.restoreToCount(saveCount)
        bounds.set(getBounds())
        bounds.bottom -= SHADOW_SIZE * 2
        bounds.right -= SHADOW_SIZE * 2
        canvas.translate(SHADOW_SIZE, SHADOW_SIZE / 2)
        canvas.drawOval(bounds, mPaint)
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.colorFilter = cf
        mShadowPaint.colorFilter = cf
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE

    override fun getPadding(padding: Rect): Boolean {
        padding[SHADOW_SIZE.toInt(), (SHADOW_SIZE / 2).toInt(), SHADOW_SIZE.toInt()] =
            (SHADOW_SIZE / 2).toInt()
        return super.getPadding(padding)
    }

    companion object {
        private val SHADOW_SIZE = 15.0f
        private val SHADOW_COLOR = -0x1000000

        private val SHADOW_COLORS_LIGHT = intArrayOf(
            0x08000000,
            0x09000000,
            0x10000000,
            0x11000000,
            0x12000000,
            0x13000000,
            0x14000000,
            0x15000000,
            0x16000000,
            0x17000000
        )

        private val SHADOW_COLORS = intArrayOf(
            0x05757575,
            0x06757575,
            0x07757575,
            0x08757575,
            0x09757575,
            0x10757575,
            0x11757575,
            0x12757575,
            0x13757575,
            0x14757575
        )

        private val SHADOW_PADDING = intArrayOf(
            3, 2, 2, 1, 1, 1, 1, 1, 1, 1
        )
    }
}