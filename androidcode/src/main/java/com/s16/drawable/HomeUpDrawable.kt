package com.s16.drawable

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin


@SuppressLint("ObsoleteSdkInt")
class HomeUpDrawable : Drawable, Animatable {

    private var mContext: Context? = null
    private var mMode = 0
    private var mIcon: Drawable? = null
    private var mArrowPath: Path? = null
    private var mArrowPaint: Paint? = null
    private var mPaint: Paint? = null
    private var mProgress = 0f
    private var mRotate = 0f

    private var mIntrinsicWidth = 0
    private var mIntrinsicHeight = 0

    private var mBarGap = 0f
    private var mBarSize = 0f
    private var mBarThickness = 0f
    private var mMiddleArrowSize = 0f
    private var mTopBottomArrowSize = 0f

    private val mUpdater: Runnable = object : Runnable {
        override fun run() {
            if (mRunning) {
                if (mProgress >= 0.0f) {
                    mProgress += 0.1f
                    if (mProgress >= 1.0f) {
                        mProgress = 1.0f
                        unscheduleSelf(this)
                        mRunning = false
                    } else {
                        scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_DURATION)
                    }
                } else {
                    mProgress -= 0.1f
                    if (mProgress <= 0.0f) {
                        mProgress = 0.0f
                        unscheduleSelf(this)
                        mRunning = false
                    } else {
                        scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_DURATION)
                    }
                }
                updateBound()
                invalidateSelf()
            }
        }
    }
    private var mRunning = false

    constructor(context: Context):
        this(context, null, DEFAULT_ARROW_COLOR, MODE_NORMAL) {

    }

    constructor(context: Context, mode: Int) :
        this(context, null, DEFAULT_ARROW_COLOR, mode) {

    }

    constructor(context: Context, arrowColor: Int, mode: Int) :
        this(context, null, arrowColor, mode) {

    }

    constructor(context: Context, icon: Drawable?, mode: Int):
        this(context, icon, DEFAULT_ARROW_COLOR, mode) {

    }

    constructor(context: Context, icon: Drawable?, arrowColor: Int, mode: Int) {
        mContext = context

        mIcon = if (icon == null) {
            val applicationInfo = context.applicationInfo
            ContextCompat.getDrawable(context, applicationInfo.icon)
        } else {
            icon
        }
        mMode = mode
        val dm = context.resources.displayMetrics
        mBarSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18f, dm)
        mTopBottomArrowSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11f, dm)
        mBarThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm)
        mBarGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        mMiddleArrowSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, dm)
        mArrowPath = Path()
        mArrowPaint = Paint().apply {
            setStyle(Paint.Style.FILL)
            setColor(arrowColor)
        }

        mPaint = Paint().apply {
            setColor(arrowColor)
            setStyle(Paint.Style.STROKE)
            setStrokeJoin(Paint.Join.ROUND)
            setStrokeCap(Paint.Cap.SQUARE)
            setStrokeWidth(mBarThickness)
        }

        val defaultIntrinsicSize: Int = getDefaultIntrinsicSize(context)
        val baseSize =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                defaultIntrinsicSize.toFloat(),
                dm
            )
                .toInt()
        if (MATERIAL_SDK_INT) {
            mIntrinsicHeight = baseSize
            mIntrinsicWidth = baseSize
        } else {
            mIntrinsicHeight = baseSize / 4 * 3
            mIntrinsicWidth = baseSize
        }
    }

    private fun createArrowPath(bounds: RectF) {
        if (mMode == MODE_NORMAL) return
        val progress = if (mMode == MODE_HOME_UP) 1.0f else mProgress
        if (MATERIAL_SDK_INT) {
            val f1 = lerp(mBarSize, mTopBottomArrowSize, progress)
            val f2 = lerp(mBarSize, mMiddleArrowSize, progress)
            val f3 = lerp(0.0f, mBarThickness / 2.0f, progress)
            val f4 = lerp(0.0f, ARROW_HEAD_ANGLE, progress)
            val f5 = 0.0f
            val f6 = 180.0f
            mRotate = lerp(f5, f6, progress)
            val f8 = lerp(mBarGap + mBarThickness, 0.0f, progress)
            mArrowPath!!.rewind()
            val f9 = -f2 / 2.0f
            mArrowPath!!.moveTo(f9 + f3, 0.0f)
            mArrowPath!!.rLineTo(f2 - f3, 0.0f)
            val f10 = round(f1 * cos(f4.toDouble())).toFloat()
            val f11 = round(f1 * sin(f4.toDouble())).toFloat()
            mArrowPath!!.moveTo(f9, f8)
            mArrowPath!!.rLineTo(f10, f11)
            mArrowPath!!.moveTo(f9, -f8)
            mArrowPath!!.rLineTo(f10, -f11)
            mArrowPath!!.moveTo(0.0f, 0.0f)
        } else {
            val width = bounds.width()
            val height = bounds.height()
            val s = min(width, height) / 48

            if (mMode == MODE_HOME_UP) {
                mArrowPath!!.rewind()
                mArrowPath!!.moveTo(s * 26, s * 2)
                mArrowPath!!.lineTo(s * 35, s * 2)
                mArrowPath!!.lineTo(s * 21, s * 23)
                mArrowPath!!.lineTo(s * 34, s * 44)
                mArrowPath!!.lineTo(s * 25, s * 44)
                mArrowPath!!.lineTo(s * 12, s * 23)
                mArrowPath!!.lineTo(s * 26, s * 2)
            } else if (mMode == MODE_DRAWER) {
                val dx = 21.0f + progress * 10.0f
                mArrowPath!!.rewind()
                mArrowPath!!.moveTo(s * 1, s * 4)
                mArrowPath!!.lineTo(s * dx, s * 4)
                mArrowPath!!.lineTo(s * dx, s * 12)
                mArrowPath!!.lineTo(s * 1, s * 12)
                mArrowPath!!.lineTo(s * 1, s * 4)
                mArrowPath!!.moveTo(s * 1, s * 22)
                mArrowPath!!.lineTo(s * dx, s * 22)
                mArrowPath!!.lineTo(s * dx, s * 30)
                mArrowPath!!.lineTo(s * 1, s * 30)
                mArrowPath!!.lineTo(s * 1, s * 22)
                mArrowPath!!.moveTo(s * 1, s * 40)
                mArrowPath!!.lineTo(s * dx, s * 40)
                mArrowPath!!.lineTo(s * dx, s * 48)
                mArrowPath!!.lineTo(s * 1, s * 48)
                mArrowPath!!.lineTo(s * 1, s * 40)
            }
        }
        mArrowPath!!.close()
        mArrowPath!!.computeBounds(bounds, false)
    }

    private fun getCalculateBounds(bounds: Rect): Rect {
        return if (MATERIAL_SDK_INT) {
            val baseSize: Int = min(bounds.width(), bounds.height())
            val x: Int = (bounds.width() - baseSize) / 2
            val y: Int = (bounds.height() - baseSize) / 2
            Rect(x, y, x + baseSize, y + baseSize)
        } else {
            val dx: Int = bounds.width() / 4
            val dy: Int = bounds.height() / 3
            val baseSize = min(dx, dy)
            val width = baseSize * 4
            val height = baseSize * 3
            val x: Int = (bounds.width() - width) / 2
            val y: Int = (bounds.height() - height) / 2
            Rect(x, y, x + width, y + height)
        }
    }

    private fun getArrowBounds(bounds: Rect): RectF {
        return if (MATERIAL_SDK_INT) {
            val dx: Int = bounds.width() / 4
            val dy: Int = bounds.height() / 4
            val baseSize = min(dx, dy)
            val arrowBounds = RectF()
            arrowBounds.top = (bounds.top + baseSize).toFloat()
            arrowBounds.left = (bounds.left + baseSize).toFloat()
            arrowBounds.bottom = (bounds.left + baseSize * 3).toFloat()
            arrowBounds.right = (bounds.left + baseSize * 3).toFloat()
            arrowBounds
        } else {
            val dx: Int = bounds.width() / 4
            val dy: Int = bounds.height() / 3
            val baseSize = Math.min(dx, dy)
            val arrowBounds = RectF()
            arrowBounds.top = (bounds.top + baseSize).toFloat()
            arrowBounds.bottom = (bounds.top + baseSize * 2).toFloat()
            arrowBounds.left = bounds.left.toFloat()
            arrowBounds.right = (bounds.left + baseSize).toFloat()
            arrowBounds
        }
    }

    private fun getIconBounds(bounds: Rect): Rect {
        val dx: Int = bounds.width() / 4
        val dy: Int = bounds.height() / 3
        val baseSize = Math.min(dx, dy)
        val iconBounds = Rect(bounds)
        iconBounds.left += baseSize
        return iconBounds
    }

    private fun updateBound() {
        val bounds: Rect = bounds
        createArrowPath(getArrowBounds(bounds))
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val calcBounds = getCalculateBounds(bounds)
        if (mMode == MODE_HOME_UP || mMode == MODE_DRAWER) {
            val saveCount: Int = canvas.save()
            val arrowBounds = getArrowBounds(calcBounds)
            if (MATERIAL_SDK_INT) {
                canvas.rotate(180.0f, arrowBounds.centerX(), arrowBounds.centerY())
                canvas.rotate(mRotate, arrowBounds.centerX(), arrowBounds.centerY())
                canvas.translate(arrowBounds.centerX(), arrowBounds.centerY())
                canvas.drawPath(mArrowPath!!, mPaint!!)
            } else {
                canvas.translate(arrowBounds.left, arrowBounds.top)
                canvas.drawPath(mArrowPath!!, mArrowPaint!!)
            }
            canvas.restoreToCount(saveCount)
        }
        if (!MATERIAL_SDK_INT && mIcon != null) {
            val iconRect = getIconBounds(calcBounds)
            mIcon!!.bounds = iconRect
            mIcon!!.draw(canvas)
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        createArrowPath(getArrowBounds(bounds!!))
    }

    override fun setAlpha(alpha: Int) {
        mArrowPaint!!.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mArrowPaint!!.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun getIntrinsicWidth(): Int {
        return mIntrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mIntrinsicHeight
    }

    fun setProgress(paramFloat: Float) {
        mProgress = paramFloat
        updateBound()
        invalidateSelf()
    }

    fun setColor(resourceId: Int) {
        mArrowPaint!!.color = mContext!!.resources.getColor(resourceId)
    }

    override fun start() {
        if (mRunning) return
        scheduleSelf(mUpdater, SystemClock.uptimeMillis() + FRAME_DURATION)
        invalidateSelf()
    }

    override fun stop() {
        if (!mRunning) return
        mRunning = false
        unscheduleSelf(mUpdater)
    }

    override fun scheduleSelf(what: Runnable, `when`: Long) {
        mRunning = true
        super.scheduleSelf(what, `when`)
    }

    override fun isRunning(): Boolean {
        return mRunning
    }

    companion object {
        private const val DEFAULT_INTRINSIC_SIZE = 32 // 50

        private val ARROW_HEAD_ANGLE = Math.toRadians(45.0).toFloat()
        private const val FRAME_DURATION = (1000 / 60).toLong()

        const val DEFAULT_ARROW_COLOR = -0x5f000001
        const val DEFAULT_ARROW_COLOR_LIGHT = 0x60000000

        private val MATERIAL_SDK_INT = Build.VERSION.SDK_INT > 20

        const val MODE_NORMAL = 0
        const val MODE_HOME_UP = 1
        const val MODE_DRAWER = 2

        private fun getDefaultIntrinsicSize(context: Context): Int {
            val screenLayout =
                context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
            when (screenLayout) {
                1 -> return DEFAULT_INTRINSIC_SIZE
                2 -> return (DEFAULT_INTRINSIC_SIZE * 1.5f).toInt()
                3, 4 -> return DEFAULT_INTRINSIC_SIZE * 2
                else -> {}
            }
            return if (screenLayout > 4) DEFAULT_INTRINSIC_SIZE * 3 else DEFAULT_INTRINSIC_SIZE
        }

        private fun lerp(paramFloat1: Float, paramFloat2: Float, paramFloat3: Float): Float {
            return paramFloat1 + paramFloat3 * (paramFloat2 - paramFloat1)
        }
    }
}