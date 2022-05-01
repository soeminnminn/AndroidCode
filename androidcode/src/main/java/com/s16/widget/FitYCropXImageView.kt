package com.s16.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView


@SuppressLint("AppCompatCustomView")
class FitYCropXImageView : ImageView {
    private var done = false

    constructor(context: Context?) : super(context) { }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) { }

    init {
        scaleType = ScaleType.MATRIX
    }

    private val drawableRect = RectF(0F, 0F, 0F, 0F)
    private val viewRect = RectF(0F, 0F, 0F, 0F)
    private val m: Matrix = Matrix()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (done) {
            return  //Already fixed drawable scale
        }
        val d: Drawable = drawable
            ?: return  //No drawable to correct for
        val viewHeight: Int = measuredHeight
        val viewWidth: Int = measuredWidth
        val drawableWidth = d.intrinsicWidth
        val drawableHeight = d.intrinsicHeight
        drawableRect[0f, 0f, drawableWidth.toFloat()] =
            drawableHeight.toFloat() //Represents the original image
        //Compute the left and right bounds for the scaled image
        val viewHalfWidth = viewWidth / 2.toFloat()
        val scale = viewHeight.toFloat() / drawableHeight.toFloat()
        val scaledWidth = drawableWidth * scale
        val scaledHalfWidth = scaledWidth / 2
        viewRect[viewHalfWidth - scaledHalfWidth, 0f, viewHalfWidth + scaledHalfWidth] =
            viewHeight.toFloat()
        m.setRectToRect(
            drawableRect,
            viewRect,
            Matrix.ScaleToFit.CENTER /* This constant doesn't matter? */
        )
        imageMatrix = m
        done = true
        requestLayout()
    }
}