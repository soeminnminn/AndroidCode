package com.s16.drawable

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue


class RatingStarsDrawable : Drawable {

    private inner class StarShape {
        val path: Path = Path()

        fun setStar(x: Float, y: Float, radius: Float, innerRadius: Float, numOfPt: Int) {
            val section = 2.0 * Math.PI / numOfPt
            path.reset()
            path.moveTo(
                (x + radius * Math.cos(0.0)).toFloat(),
                (y + radius * Math.sin(0.0)).toFloat()
            )
            path.lineTo(
                (x + innerRadius * Math.cos(0 + section / 2.0)).toFloat(),
                (y + innerRadius * Math.sin(0 + section / 2.0)).toFloat()
            )
            for (i in 1 until numOfPt) {
                path.lineTo(
                    (x + radius * Math.cos(section * i)).toFloat(),
                    (y + radius * Math.sin(section * i)).toFloat()
                )
                path.lineTo(
                    (x + innerRadius * Math.cos(section * i + section / 2.0)).toFloat(),
                    (y + innerRadius * Math.sin(section * i + section / 2.0)).toFloat()
                )
            }
            path.close()
        }

        fun rotate(bounds: RectF, angle: Float) {
            val matrix = Matrix()
            path.computeBounds(bounds, true)
            matrix.postRotate(
                angle,
                (bounds.right + bounds.left) / 2,
                (bounds.bottom + bounds.top) / 2
            )
            path.transform(matrix)
        }

    }

    private var mOrientation = AUTO
    private val mIntrinsicWidth: Int
    private val mIntrinsicHeight: Int
    private var mShapeArr: Array<StarShape?>
    private var mRatioRadius = 0.5f
    private var mRatioInnerRadius = 0.25f
    private var mNumberOfPoint = 5
    private val mPaintHot: Paint
    private val mPaintClear: Paint
    private var mValue = 0

    constructor(baseIntrinsicSize: Int) {
        mShapeArr = arrayOfNulls(5)
        for (i in 0..4) {
            mShapeArr[i] = StarShape()
        }

        mPaintHot = Paint()
        mPaintHot.style = Paint.Style.FILL
        mPaintHot.color = DEFAULT_HOT_COLOR

        mPaintClear = Paint()
        mPaintClear.color = DEFAULT_COLOR
        mPaintClear.style = Paint.Style.FILL
        mIntrinsicHeight = baseIntrinsicSize
        mIntrinsicWidth = mIntrinsicHeight * 5
    }

    constructor(context: Context) {
        mShapeArr = arrayOfNulls(5)
        for (i in 0..4) {
            mShapeArr[i] = StarShape()
        }

        mPaintHot = Paint()
        mPaintHot.style = Paint.Style.FILL
        mPaintHot.color = DEFAULT_HOT_COLOR

        mPaintClear = Paint()
        mPaintClear.color = DEFAULT_COLOR
        mPaintClear.style = Paint.Style.FILL

        val dm = context.resources.displayMetrics
        val defaultIntrinsicSize = getDefaultIntrinsicSize(context)
        mIntrinsicHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            defaultIntrinsicSize.toFloat(),
            dm
        ).toInt()
        mIntrinsicWidth = mIntrinsicHeight * 5
    }

    override fun draw(canvas: Canvas) {
        onBoundsChange(bounds)
        for (i in 0..4) {
            val shape = mShapeArr[i]
            if (i < mValue) {
                canvas.drawPath(shape!!.path, mPaintHot)
            } else {
                canvas.drawPath(shape!!.path, mPaintClear)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        mPaintHot.alpha = alpha
        mPaintClear.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaintHot.colorFilter = cf
        mPaintClear.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun onBoundsChange(bounds: Rect) {
        measureShape(bounds.width(), bounds.height())
    }

    override fun getIntrinsicWidth(): Int {
        if (mOrientation == HORIZONTAL) {
            return mIntrinsicWidth
        } else if (mOrientation == VERTICAL) {
            return mIntrinsicHeight
        }
        return mIntrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        if (mOrientation == HORIZONTAL) {
            return mIntrinsicHeight
        } else if (mOrientation == VERTICAL) {
            return mIntrinsicWidth
        }
        return mIntrinsicHeight
    }

    private fun measureShape(width: Int, height: Int) {
        if (width == 0 || height == 0) return
        var w = 0f
        var h = 0f
        if (mOrientation == HORIZONTAL) {
            w = width / 5.0f
            h = height.toFloat()
        } else if (mOrientation == VERTICAL) {
            w = width.toFloat()
            h = height / 5.0f
        } else {
            if (width > height) {
                w = width / 5.0f
                h = height.toFloat()
            } else {
                w = width.toFloat()
                h = height / 5.0f
            }
        }
        var left = 0f
        var top = 0f
        for (i in 0..4) {
            val x = left + w / 2.0f
            val y = top + h / 2.0f
            var radius: Float
            var innerRadius: Float
            if (w > h) {
                radius = h * mRatioRadius
                innerRadius = h * mRatioInnerRadius
            } else {
                radius = w * mRatioRadius
                innerRadius = w * mRatioInnerRadius
            }
            val shape = mShapeArr[i]
            shape!!.setStar(x, y, radius, innerRadius, mNumberOfPoint)
            val bounds = RectF(x, y, x + w, y + h)
            shape.rotate(bounds, -90.0f)
            if (mOrientation == HORIZONTAL) {
                left += w
            } else if (mOrientation == VERTICAL) {
                top += h
            } else {
                if (width > height) {
                    left += w
                } else {
                    top += h
                }
            }
        }
    }

    fun setOrientation(value: Int) {
        if (mOrientation != value) {
            mOrientation = value
            invalidateSelf()
        }
    }

    fun setHotColor(color: Int) {
        mPaintHot.color = color
        invalidateSelf()
    }

    fun setFillColor(color: Int) {
        mPaintClear.color = color
        invalidateSelf()
    }

    fun setShapeRadiusRatio(ratio: Float) {
        if (mRatioRadius != ratio) {
            mRatioRadius = ratio
            invalidateSelf()
        }
    }

    fun setShapeInnerRadiusRatio(ratio: Float) {
        if (mRatioInnerRadius != ratio) {
            mRatioInnerRadius = ratio
            invalidateSelf()
        }
    }

    fun setNumberOfPoint(pt: Int) {
        if (mNumberOfPoint != pt) {
            mNumberOfPoint = pt
            invalidateSelf()
        }
    }

    fun getValue() : Int = mValue

    fun setValue(value: Int) {
        if (value < 0 || value > 5) return
        if (mValue != value) {
            mValue = value
            invalidateSelf()
        }
    }

    companion object {
        private const val DEFAULT_HOT_COLOR = Color.BLUE
        private const val DEFAULT_COLOR = Color.GRAY
        private const val DEFAULT_INTRINSIC_SIZE = 8

        const val AUTO = -1
        const val VERTICAL = 0
        const val HORIZONTAL = 1

        private fun getDefaultIntrinsicSize(context: Context): Int {
            val screenLayout =
                context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
            when (screenLayout) {
                1 -> return (DEFAULT_INTRINSIC_SIZE + 2.0f).toInt()
                2 -> return (DEFAULT_INTRINSIC_SIZE * 1.5f + 2.0f).toInt()
                3 -> return DEFAULT_INTRINSIC_SIZE * 2
                4 -> return DEFAULT_INTRINSIC_SIZE * 3
                else -> {}
            }
            return if (screenLayout > 4) DEFAULT_INTRINSIC_SIZE * 4 else DEFAULT_INTRINSIC_SIZE
        }
    }
}