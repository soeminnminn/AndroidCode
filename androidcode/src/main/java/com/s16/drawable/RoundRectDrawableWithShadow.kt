package com.s16.drawable

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import kotlin.math.cos
import kotlin.math.max


class RoundRectDrawableWithShadow(resources: Resources, backgroundColor: Int,
      topRadius: Float, bottomRadius: Float, shadowSize: Float, maxShadowSize: Float)
    : Drawable() {

    enum class TYPE {
        ALL, TOP, BOTTOM, NONE
    }

    private var mType = TYPE.ALL

    // extra shadow to avoid gaps between card and shadow
    private var mInsetShadow = 0
    private val sCornerRect = RectF()
    private var mPaint: Paint
    private var mCornerShadowPaint: Paint
    private var mEdgeShadowPaint: Paint
    private var mCardBounds: RectF
    private var mCornerRadius = 0f

    private var mCornerShadowPath: Path? = null

    // updated value with inset
    private var mMaxShadowSize = 0f

    // actual value set by developer
    private var mRawMaxShadowSize = 0f

    // multiplied value to account for shadow offset
    private var mShadowSize = 0f

    // actual value set by developer
    private var mRawShadowSize = 0f

    private var mDirty = true
    private var mShadowStartColor = 0
    private var mShadowEndColor = 0
    private var mAddPaddingForCorners = true

    /**
     * If shadow size is set to a value above max shadow, we print a warning
     */
    private var mPrintedShadowClipWarning = false

    init {
        mShadowStartColor = SHADOW_START_COLOR
        mShadowEndColor = SHADOW_END_COLOR

        val metrics: DisplayMetrics = resources.displayMetrics
        mInsetShadow = (INSET_SHADOW * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)).toInt()

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        mPaint.color = backgroundColor

        setCornerRadius(topRadius, bottomRadius)

        mCornerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        mCornerShadowPaint.style = Paint.Style.FILL

        mEdgeShadowPaint = Paint(mCornerShadowPaint)
        //mCornerRadius = (int) (radius + .5f);
        //mCornerRadius = (int) (radius + .5f);
        mCardBounds = RectF()

        mEdgeShadowPaint.isAntiAlias = false
        setShadowSize(shadowSize, maxShadowSize)
    }

    constructor(resources: Resources, backgroundColor: Int, radius: Float, shadowSize: Float, maxShadowSize: Float)
        : this(resources, backgroundColor, radius, radius, shadowSize, maxShadowSize) {
    }

    /**
     * Casts the value to an even integer.
     */
    private fun toEven(value: Float): Int {
        val i = (value + .5f).toInt()
        return if (i % 2 == 1) {
            i - 1
        } else i
    }

    fun setAddPaddingForCorners(addPaddingForCorners: Boolean) {
        mAddPaddingForCorners = addPaddingForCorners
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
        mCornerShadowPaint.alpha = alpha
        mEdgeShadowPaint.alpha = alpha
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        mDirty = true
    }

    fun setShadowSize(shadowSize: Float, maxShadowSize: Float) {
        var lShadowSize = shadowSize
        var lMaxShadowSize = maxShadowSize

        require(!(lShadowSize < 0 || lMaxShadowSize < 0)) { "invalid shadow size" }
        lShadowSize = toEven(lShadowSize).toFloat()
        lMaxShadowSize = toEven(lMaxShadowSize).toFloat()
        if (lShadowSize > lMaxShadowSize) {
            lShadowSize = lMaxShadowSize
            if (!mPrintedShadowClipWarning) {
                mPrintedShadowClipWarning = true
            }
        }
        if (mRawShadowSize == lShadowSize && mRawMaxShadowSize == lMaxShadowSize) {
            return
        }
        mRawShadowSize = lShadowSize
        mRawMaxShadowSize = lMaxShadowSize
        mShadowSize = (lShadowSize * SHADOW_MULTIPLIER + mInsetShadow + .5f)
        mMaxShadowSize = lMaxShadowSize + mInsetShadow
        mDirty = true
        invalidateSelf()
    }

    override fun getPadding(padding: Rect): Boolean {
        val rad: Float
        val vTopOffset: Int
        val vBottomOffset: Int
        if (mType === TYPE.TOP) {
            rad = mCornerRadius
            vTopOffset = Math.ceil(
                calculateVerticalPadding(
                    mRawMaxShadowSize, rad,
                    mAddPaddingForCorners
                ).toDouble()
            ).toInt()
            vBottomOffset = 0
        } else if (mType === TYPE.BOTTOM) {
            rad = mCornerRadius
            vTopOffset = 0
            vBottomOffset = Math.ceil(
                calculateVerticalPadding(
                    mRawMaxShadowSize, rad,
                    mAddPaddingForCorners
                ).toDouble()
            ).toInt()
        } else {
            rad = mCornerRadius
            vBottomOffset = Math.ceil(
                calculateVerticalPadding(
                    mRawMaxShadowSize,
                    rad, mAddPaddingForCorners
                ).toDouble()
            ).toInt()
            vTopOffset = vBottomOffset
        }
        val hOffset = Math.ceil(
            calculateHorizontalPadding(
                mRawMaxShadowSize, rad,
                mAddPaddingForCorners
            ).toDouble()
        ).toInt()
        padding.set(hOffset, vTopOffset, hOffset, vBottomOffset)
        return true
    }

    private fun calculateVerticalPadding(
        maxShadowSize: Float, cornerRadius: Float,
        addPaddingForCorners: Boolean
    ): Float {
        return if (addPaddingForCorners) {
            (maxShadowSize * SHADOW_MULTIPLIER + (1 - COS_45) * cornerRadius).toFloat()
        } else {
            maxShadowSize * SHADOW_MULTIPLIER
        }
    }

    private fun calculateHorizontalPadding(
        maxShadowSize: Float, cornerRadius: Float,
        addPaddingForCorners: Boolean
    ): Float {
        return if (addPaddingForCorners) {
            (maxShadowSize + (1 - COS_45) * cornerRadius).toFloat()
        } else {
            maxShadowSize
        }
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.colorFilter = cf
        mCornerShadowPaint.colorFilter = cf
        mEdgeShadowPaint.colorFilter = cf
    }

    fun setCornerRadius(topRadius: Float, bottomRadius: Float) {
        var lTopRadius = topRadius
        var lBottomRadius = bottomRadius
        lTopRadius = (lTopRadius + .5f)
        lBottomRadius = (lBottomRadius + .5f)
        if (lTopRadius == lBottomRadius && mCornerRadius == lTopRadius && mCornerRadius > 0) {
            return
        }
        if (lTopRadius == lBottomRadius && lTopRadius > 0) {
            mCornerRadius = lTopRadius
            mType = TYPE.ALL
        } else if (lTopRadius > 0) {
            mCornerRadius = lTopRadius
            mType = TYPE.TOP
        } else if (lBottomRadius > 0) {
            mCornerRadius = lBottomRadius
            mType = TYPE.BOTTOM
        } else {
            mCornerRadius = 0f
            mType = TYPE.NONE
        }
        mDirty = true
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun draw(canvas: Canvas) {
        if (mDirty) {
            buildComponents(bounds)
            mDirty = false
        }
        canvas.translate(0f, mRawShadowSize / 2)
        drawShadow(canvas)
        canvas.translate(0f, -mRawShadowSize / 2)
        drawRoundRect(canvas, mCardBounds, mCornerRadius, mPaint, mType)
    }

    private fun drawShadow(canvas: Canvas) {
        val edgeShadowTop = -mCornerRadius - mShadowSize
        val inset = mCornerRadius + mInsetShadow + mRawShadowSize / 2
        val drawHorizontalEdges = mCardBounds.width() - 2 * inset > 0
        val drawVerticalEdges = mCardBounds.height() - 2 * inset > 0
        var saved = canvas.save()
        if (mType === TYPE.TOP) {
            // LT
            canvas.translate(mCardBounds.left + inset, mCardBounds.top + inset)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawHorizontalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.width() - 2 * inset, -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)

            // RT
            saved = canvas.save()
            canvas.translate(mCardBounds.right - inset, mCardBounds.top + inset)
            canvas.rotate(90f)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawVerticalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.height(), -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)
            saved = canvas.save()
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom)
            canvas.rotate(270f)
            if (drawVerticalEdges) {
                canvas.drawRect(
                    (-mInsetShadow).toFloat(),
                    edgeShadowTop,
                    mCardBounds.height() - inset,
                    -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)
        } else if (mType === TYPE.BOTTOM) {
            // LB
            saved = canvas.save()
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom - inset)
            canvas.rotate(270f)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawVerticalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.bottom, -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)

            // RB
            saved = canvas.save()
            canvas.translate(mCardBounds.right - inset, mCardBounds.bottom - inset)
            canvas.rotate(180f)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawHorizontalEdges) {
                canvas.drawRect(
                    0f,
                    edgeShadowTop,
                    mCardBounds.width() - 2 * inset,  /*-mCornerRadius + mCornerRadius +*/
                    mInsetShadow - mCornerRadius / 2,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)
            saved = canvas.save()
            canvas.translate(mCardBounds.right - inset, mCardBounds.top)
            canvas.rotate(90f)
            if (drawVerticalEdges) {
                canvas.drawRect(
                    -2 * mCornerRadius - mInsetShadow,
                    edgeShadowTop,
                    mCardBounds.height() - inset,
                    -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)
        } else if (mType === TYPE.ALL) {
            // LT
            canvas.translate(mCardBounds.left + inset, mCardBounds.top + inset)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawHorizontalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.width() - 2 * inset, -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)

            // RB
            saved = canvas.save()
            canvas.translate(mCardBounds.right - inset, mCardBounds.bottom - inset)
            canvas.rotate(180f)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawHorizontalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.width() - 2 * inset,
                    -mCornerRadius + mShadowSize, mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)

            // LB
            saved = canvas.save()
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom - inset)
            canvas.rotate(270f)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawVerticalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.height() - 2 * inset, -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)

            // RT
            saved = canvas.save()
            canvas.translate(mCardBounds.right - inset, mCardBounds.top + inset)
            canvas.rotate(90f)
            canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
            if (drawVerticalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.height() - 2 * inset, -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)
        } else {
            saved = canvas.save()
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom)
            canvas.rotate(270f)
            //canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawVerticalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.bottom + inset, -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)
            saved = canvas.save()
            canvas.translate(mCardBounds.right - inset, mCardBounds.top - inset)
            canvas.rotate(90f)
            if (drawVerticalEdges) {
                canvas.drawRect(
                    0f, edgeShadowTop, mCardBounds.bottom + inset, -mCornerRadius,
                    mEdgeShadowPaint
                )
            }
            canvas.restoreToCount(saved)
        }
    }

    private fun drawRoundRect(
        canvas: Canvas, bounds: RectF, cornerRadius: Float,
        paint: Paint, type: TYPE
    ) {
        var lCornerRadius = cornerRadius
        val twoRadius = lCornerRadius * 2
        val innerWidth = bounds.width() - twoRadius - 1
        val innerHeight = bounds.height() - twoRadius - 1
        var topRadius: Float
        var bottomRadius: Float
        bottomRadius = lCornerRadius
        topRadius = bottomRadius

        // increment it to account for half pixels.
        if (lCornerRadius >= 1f) {
            lCornerRadius += .5f
            sCornerRect[-lCornerRadius, -lCornerRadius, lCornerRadius] = lCornerRadius
            val saved: Int = canvas.save()
            canvas.translate(bounds.left + lCornerRadius, bounds.top + lCornerRadius)
            if (type === TYPE.TOP) {
                canvas.drawArc(sCornerRect, 180f, 90f, true, paint)
                canvas.translate(innerWidth, 0f)
                //canvas.rotate(90);
                canvas.drawArc(sCornerRect, 270f, 180f, true, paint)
                bottomRadius = 0f
            } else if (type === TYPE.BOTTOM) {
                canvas.translate(0f, innerHeight)
                //canvas.rotate(90);
                canvas.drawArc(sCornerRect, 360f, 270f, true, paint)
                canvas.translate(innerWidth, 0f)
                //canvas.rotate(90);
                canvas.drawArc(sCornerRect, 270f, 360f, true, paint)
                topRadius = 0f
            } else if (type === TYPE.ALL) {
                canvas.drawArc(sCornerRect, 180f, 90f, true, paint)
                canvas.translate(innerWidth, 0f)
                canvas.rotate(90f)
                canvas.drawArc(sCornerRect, 180f, 90f, true, paint)
                canvas.translate(innerHeight, 0f)
                canvas.rotate(90f)
                canvas.drawArc(sCornerRect, 180f, 90f, true, paint)
                canvas.translate(innerWidth, 0f)
                canvas.rotate(90f)
                canvas.drawArc(sCornerRect, 180f, 90f, true, paint)
            } else {
                bottomRadius = 0f
                topRadius = bottomRadius
            }
            canvas.restoreToCount(saved)
            //draw top and bottom pieces
            if (type === TYPE.TOP || type === TYPE.ALL) {
                canvas.drawRect(
                    bounds.left + lCornerRadius - 1f, bounds.top,
                    bounds.right - lCornerRadius + 1f, bounds.top + lCornerRadius, paint
                )
            }
            if (type === TYPE.BOTTOM || type === TYPE.ALL) {
                canvas.drawRect(
                    bounds.left + lCornerRadius - 1f,
                    bounds.bottom - lCornerRadius + 1f, bounds.right - lCornerRadius + 1f,
                    bounds.bottom, paint
                )
            }
        }
        ////                center
        canvas.drawRect(
            bounds.left, bounds.top + max(0f, topRadius - 1f),
            bounds.right, bounds.bottom - bottomRadius + 1f, paint
        )
    }

    private fun buildShadowCorners() {
        val hRad = mCornerRadius
        var vTRad = 0f
        var vBRad = 0f
        when (mType) {
            TYPE.ALL -> {
                vBRad = mCornerRadius
                vTRad = vBRad
            }
            TYPE.TOP -> {
                vTRad = mCornerRadius
                vBRad = 0f
            }
            TYPE.BOTTOM -> {
                vTRad = 0f
                vBRad = mCornerRadius
            }
            else -> {}
        }
        val innerBounds = RectF(-hRad, -vTRad, hRad, vBRad)
        val outerBounds = RectF(innerBounds)
        outerBounds.inset(-mShadowSize, -mShadowSize)
        if (mCornerShadowPath == null) {
            mCornerShadowPath = Path()
        } else {
            mCornerShadowPath!!.reset()
        }
        mCornerShadowPath!!.fillType = Path.FillType.EVEN_ODD
        mCornerShadowPath!!.moveTo(-hRad, 0f)
        mCornerShadowPath!!.rLineTo(-mShadowSize, 0f)
        // outer arc
        mCornerShadowPath!!.arcTo(outerBounds, 180f, 90f, false)
        // inner arc
        mCornerShadowPath!!.arcTo(innerBounds, 270f, -90f, false)
        mCornerShadowPath!!.close()
        val startRatio = mCornerRadius / (mCornerRadius + mShadowSize)
        mCornerShadowPaint.shader =
            RadialGradient(
                0f,
                0f,
                mCornerRadius + mShadowSize,
                intArrayOf(mShadowStartColor, mShadowStartColor, mShadowEndColor),
                floatArrayOf(0f, startRatio, 1f),
                Shader.TileMode.CLAMP
            )
        // we offset the content shadowSize/2 pixels up to make it more realistic.
        // this is why edge shadow shader has some extra space
        // When drawing bottom edge shadow, we use that extra space.
        mEdgeShadowPaint.shader = LinearGradient(
            0f,
            -mCornerRadius + mShadowSize,
            0f,
            -mCornerRadius - mShadowSize,
            intArrayOf(mShadowStartColor, mShadowStartColor, mShadowEndColor),
            floatArrayOf(0f, .5f, 1f),
            Shader.TileMode.CLAMP
        )
        mEdgeShadowPaint.isAntiAlias = false
    }

    private fun buildComponents(bounds: Rect) {
        // Card is offset SHADOW_MULTIPLIER * maxShadowSize to account for the shadow shift.
        // We could have different top-bottom offsets to avoid extra gap above but in that case
        // center aligning Views inside the CardView would be problematic.
        var vTopOff = 0f
        var vBottomOff = 0f
        val verticalOffset = mRawMaxShadowSize * SHADOW_MULTIPLIER
        when (mType) {
            TYPE.ALL -> {
                vBottomOff = verticalOffset
                vTopOff = vBottomOff
            }
            TYPE.TOP -> {
                vTopOff = verticalOffset
                vBottomOff = 0f
            }
            TYPE.BOTTOM -> {
                vTopOff = 0f
                vBottomOff = verticalOffset
            }
            else -> {}
        }
        mCardBounds[bounds.left + mRawMaxShadowSize, bounds.top + vTopOff, bounds.right - mRawMaxShadowSize] =
            bounds.bottom - vBottomOff
        buildShadowCorners()
    }

    fun getCornerRadius(): Float {
        return mCornerRadius
    }

    fun getMaxShadowAndCornerPadding(into: Rect?) {
        getPadding(into!!)
    }

    fun setShadowSize(size: Float) {
        setShadowSize(size, mRawMaxShadowSize)
    }

    fun setMaxShadowSize(size: Float) {
        setShadowSize(mRawShadowSize, size)
    }

    fun getShadowSize(): Float {
        return mRawShadowSize
    }

    fun getMaxShadowSize(): Float {
        return mRawMaxShadowSize
    }

    fun getMinWidth(): Float {
        val content = 2 * Math.max(
            mRawMaxShadowSize,
            mCornerRadius + mInsetShadow + mRawMaxShadowSize / 2
        )
        return content + (mRawMaxShadowSize + mInsetShadow) * 2
    }

    fun getMinHeight(): Float {
        val content = 2 * Math.max(
            mRawMaxShadowSize,
            mCornerRadius + mInsetShadow + mRawMaxShadowSize * SHADOW_MULTIPLIER / 2
        )
        return content + (mRawMaxShadowSize * SHADOW_MULTIPLIER + mInsetShadow) * 2
    }

    fun setColor(color: Int) {
        mPaint.color = color
        invalidateSelf()
    }

    companion object {
        // used to calculate content padding
        private val COS_45 = cos(Math.toRadians(45.0))
        private const val SHADOW_START_COLOR = 0x37000000
        private const val SHADOW_END_COLOR = 0x03000000
        private const val INSET_SHADOW = 1.0f

        private const val SHADOW_MULTIPLIER = 1.5f
    }
}