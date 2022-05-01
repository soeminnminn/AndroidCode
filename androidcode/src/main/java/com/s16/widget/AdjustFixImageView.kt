package com.s16.widget

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class AdjustFixImageView : AppCompatImageView {
    private var oldScaleType: ScaleType = ScaleType.CENTER

    constructor(context: Context)
            : super(context) { }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {}

    var adjustFix : Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    oldScaleType = scaleType
                    scaleType = ScaleType.MATRIX
                } else {
                    scaleType = oldScaleType
                }
                requestLayout()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (adjustFix) {
            imageMatrix = calculateMatrix(drawable, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
    }

    private fun calculateMatrix(drawable: Drawable?, viewWidth: Float, viewHeight: Float): Matrix? {
        return drawable?.let { d ->
            val drawableWidth = d.intrinsicWidth.toFloat()
            val drawableHeight = d.intrinsicHeight.toFloat()

            val drawableRect = RectF(0f, 0f, drawableWidth, drawableHeight)
            var distRect: RectF

            // Height fix
            val viewHalfWidth = viewWidth / 2f
            var scale = viewHeight / drawableHeight
            val scaledWidth = drawableWidth * scale
            val scaledHalfWidth = scaledWidth / 2f
            distRect = RectF(
                viewHalfWidth - scaledHalfWidth,
                0f,
                viewHalfWidth + scaledHalfWidth,
                viewHeight
            )

            if (distRect.width() < viewWidth) {
                // Width fix
                val viewHalfHeight = viewHeight / 2f
                scale = viewWidth / drawableWidth
                val scaledHeight = drawableHeight * scale
                val scaledHalfHeight = scaledHeight / 2
                distRect = RectF(
                    0f,
                    viewHalfHeight - scaledHalfHeight,
                    viewWidth,
                    viewHalfHeight + scaledHalfHeight
                )
            }

            Matrix().apply {
                setRectToRect(
                    drawableRect,
                    distRect,
                    Matrix.ScaleToFit.CENTER
                )
            }
        }
    }
}