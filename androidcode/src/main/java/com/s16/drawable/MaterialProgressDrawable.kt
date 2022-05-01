package com.s16.drawable

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.*
import android.view.animation.Interpolator
import androidx.annotation.IntDef
import kotlin.math.*


/**
 * Fancy progress indicator for Material theme.
 *
 * @Deprecated Use [CircularProgressDrawable]
 */
class MaterialProgressDrawable(context: Context, parent: View) :
    Drawable(), Animatable {
    @kotlin.annotation.Retention(AnnotationRetention.BINARY)
    @IntDef(SMALL, DEFAULT, LARGE)
    annotation class ProgressDrawableSize

    private val COLORS = intArrayOf(
        Color.BLACK
    )

    /** The list of animators operating on this drawable.  */
    private val mAnimators = ArrayList<Animation>()

    /** The indicator ring, used to manage animation state.  */
    private val mRing: Ring

    /** Canvas rotation in degrees.  */
    private var mRotation = 0f
    private val mResources: Resources = context.resources
    private val mParent: View = parent
    private var mAnimation: Animation? = null
    private var mRotationCount = 0f
    private var mWidth = 0.0
    private var mHeight = 0.0
    private var mFinishing = false

    private val mCallback: Callback = object : Callback {
        override fun invalidateDrawable(d: Drawable) {
            invalidateSelf()
        }

        override fun scheduleDrawable(d: Drawable, what: Runnable, `when`: Long) {
            scheduleSelf(what, `when`)
        }

        override fun unscheduleDrawable(d: Drawable, what: Runnable) {
            unscheduleSelf(what)
        }
    }

    init {
        mRing = Ring(mCallback, this)
        mRing.setColors(COLORS)
        mRing.setAlpha(255)
        mRing.setArrowScale(1.0f)
        updateSizes(DEFAULT)
        setupAnimators()
    }

    private fun setSizeParameters(
        progressCircleWidth: Double, progressCircleHeight: Double,
        centerRadius: Double, strokeWidth: Double, arrowWidth: Float, arrowHeight: Float
    ) {
        val ring = mRing
        val metrics = mResources.displayMetrics
        val screenDensity = metrics.density
        mWidth = progressCircleWidth * screenDensity
        mHeight = progressCircleHeight * screenDensity
        ring.setStrokeWidth(strokeWidth.toFloat() * screenDensity)
        ring.setCenterRadius(centerRadius * screenDensity)
        ring.setColorIndex(0)
        ring.setArrowDimensions(arrowWidth * screenDensity, arrowHeight * screenDensity)
        ring.setInsets(mWidth.toInt(), mHeight.toInt())
    }

    /**
     * Set the overall size for the progress spinner. This updates the radius
     * and stroke width of the ring.
     *
     * @param size One of {MaterialProgressDrawable.LARGE} or
     * {MaterialProgressDrawable.DEFAULT}
     */
    fun updateSizes(@ProgressDrawableSize size: Int) {
        if (size == LARGE) {
            setSizeParameters(
                CIRCLE_DIAMETER_LARGE.toDouble(),
                CIRCLE_DIAMETER_LARGE.toDouble(),
                CENTER_RADIUS_LARGE.toDouble(),
                STROKE_WIDTH_LARGE.toDouble(),
                ARROW_WIDTH_LARGE.toFloat(),
                ARROW_HEIGHT_LARGE.toFloat()
            )
        } else if (size == SMALL) {
            setSizeParameters(
                CIRCLE_DIAMETER_SMALL.toDouble(),
                CIRCLE_DIAMETER_SMALL.toDouble(), CENTER_RADIUS_SMALL.toDouble(),
                STROKE_WIDTH_SMALL.toDouble(),
                ARROW_WIDTH_SMALL.toFloat(), ARROW_HEIGHT_SMALL.toFloat()
            )
        } else {
            setSizeParameters(
                CIRCLE_DIAMETER.toDouble(), CIRCLE_DIAMETER.toDouble(),
                CENTER_RADIUS.toDouble(), STROKE_WIDTH.toDouble(),
                ARROW_WIDTH.toFloat(), ARROW_HEIGHT.toFloat()
            )
        }
    }

    /**
     * @param show Set to true to display the arrowhead on the progress spinner.
     */
    fun showArrow(show: Boolean) {
        mRing.setShowArrow(show)
    }

    /**
     * @param scale Set the scale of the arrowhead for the spinner.
     */
    fun setArrowScale(scale: Float) {
        mRing.setArrowScale(scale)
    }

    /**
     * Set the start and end trim for the progress spinner arc.
     *
     * @param startAngle start angle
     * @param endAngle end angle
     */
    fun setStartEndTrim(startAngle: Float, endAngle: Float) {
        mRing.setStartTrim(startAngle)
        mRing.setEndTrim(endAngle)
    }

    /**
     * Set the amount of rotation to apply to the progress spinner.
     *
     * @param rotation Rotation is from [0..1]
     */
    fun setProgressRotation(rotation: Float) {
        mRing.setRotation(rotation)
    }

    /**
     * Update the background color of the circle image view.
     */
    fun setBackgroundColor(color: Int) {
        mRing.setBackgroundColor(color)
    }

    /**
     * Set the colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colors
     */
    fun setColorSchemeColors(vararg colors: Int) {
        mRing.setColors(colors)
        mRing.setColorIndex(0)
    }

    override fun getIntrinsicHeight(): Int {
        return mHeight.toInt()
    }

    override fun getIntrinsicWidth(): Int {
        return mWidth.toInt()
    }

    @Suppress("DEPRECATION")
    override fun draw(canvas: Canvas) {
        val saveMatrix = canvas.matrix
        canvas.restore()
        val bounds = canvas.clipBounds
        val size = min(mWidth, mHeight)
        val dx = ((bounds.width() - size) * 0.5f).toFloat()
        val dy = ((bounds.height() - size) * 0.5f).toFloat()
        canvas.rotate(mRotation, bounds.exactCenterX(), bounds.exactCenterY())
        val rectDraw = Rect(dx.toInt(), dy.toInt(), (dx + size).toInt(), (dy + size).toInt())
        mRing.draw(canvas, rectDraw)
        canvas.setMatrix(saveMatrix)
    }

    override fun setAlpha(alpha: Int) {
        mRing.setAlpha(alpha)
    }

    override fun getAlpha(): Int {
        return mRing.getAlpha()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mRing.setColorFilter(colorFilter)
    }

    fun setRotation(rotation: Float) {
        mRotation = rotation
        invalidateSelf()
    }

    private fun getRotation(): Float {
        return mRotation
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun isRunning(): Boolean {
        val animators = mAnimators
        val N: Int = animators.size
        for (i in 0 until N) {
            val animator = animators[i]
            if (animator.hasStarted() && !animator.hasEnded()) {
                return true
            }
        }
        return false
    }

    override fun start() {
        mAnimation!!.reset()
        mRing.storeOriginals()
        // Already showing some part of the ring
        if (mRing.getEndTrim() != mRing.getStartTrim()) {
            mFinishing = true
            mAnimation!!.duration = (ANIMATION_DURATION / 2).toLong()
            mParent.startAnimation(mAnimation)
        } else {
            mRing.setColorIndex(0)
            mRing.resetOriginals()
            mAnimation!!.duration = ANIMATION_DURATION.toLong()
            mParent.startAnimation(mAnimation)
        }
    }

    override fun stop() {
        mParent.clearAnimation()
        setRotation(0f)
        mRing.setShowArrow(false)
        mRing.setColorIndex(0)
        mRing.resetOriginals()
    }

    private fun applyFinishTranslation(interpolatedTime: Float, ring: Ring) {
        // shrink back down and complete a full rotation before
        // starting other circles
        // Rotation goes between [0..1].
        val targetRotation = (floor((ring.getStartingRotation() / MAX_PROGRESS_ARC).toDouble())
                + 1f).toFloat()
        val startTrim = (ring.getStartingStartTrim()
                + (ring.getStartingEndTrim() - ring.getStartingStartTrim()) * interpolatedTime)
        ring.setStartTrim(startTrim)
        val rotation = (ring.getStartingRotation()
                + (targetRotation - ring.getStartingRotation()) * interpolatedTime)
        ring.setRotation(rotation)
    }

    private fun setupAnimators() {
        val ring = mRing
        val animation: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (mFinishing) {
                    applyFinishTranslation(interpolatedTime, ring)
                } else {
                    // The minProgressArc is calculated from 0 to create an
                    // angle that
                    // matches the stroke width.
                    val minProgressArc = Math.toRadians(
                        ring.getStrokeWidth() / (2 * Math.PI * ring.getCenterRadius())
                    ).toFloat()
                    val startingEndTrim = ring.getStartingEndTrim()
                    val startingTrim = ring.getStartingStartTrim()
                    val startingRotation = ring.getStartingRotation()
                    // Offset the minProgressArc to where the endTrim is
                    // located.
                    val minArc = MAX_PROGRESS_ARC - minProgressArc
                    val endTrim = startingEndTrim + (minArc
                            * START_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime))
                    ring.setEndTrim(endTrim)
                    val startTrim = startingTrim + (MAX_PROGRESS_ARC
                            * END_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime))
                    ring.setStartTrim(startTrim)
                    val rotation = startingRotation + 0.25f * interpolatedTime
                    ring.setRotation(rotation)
                    val groupRotation = (720.0f / NUM_POINTS * interpolatedTime
                            + 720.0f * (mRotationCount / NUM_POINTS))
                    setRotation(groupRotation)
                }
            }
        }
        animation.repeatCount = Animation.INFINITE
        animation.repeatMode = Animation.RESTART
        animation.interpolator = LINEAR_INTERPOLATOR
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                mRotationCount = 0f
            }

            override fun onAnimationEnd(animation: Animation) {
                // do nothing
            }

            override fun onAnimationRepeat(animation: Animation) {
                ring.storeOriginals()
                ring.goToNextColor()
                ring.setStartTrim(ring.getEndTrim())
                if (mFinishing) {
                    // finished closing the last ring from the swipe gesture; go
                    // into progress mode
                    mFinishing = false
                    animation.duration = ANIMATION_DURATION.toLong()
                    ring.setShowArrow(false)
                } else {
                    mRotationCount = (mRotationCount + 1) % NUM_POINTS
                }
            }
        })
        mAnimation = animation
    }

    private class Ring {

        private val mTempBounds = RectF()
        private val mPaint = Paint()
        private val mArrowPaint = Paint()
        private var mStartTrim = 0.0f
        private var mEndTrim = 0.0f
        private var mRotation = 0.0f
        private var mStrokeWidth = 5.0f
        private var mStrokeInset = 2.5f
        private var mColors: IntArray = intArrayOf()
        private var mCallback: Callback
        private var mDrawable: Drawable

        // mColorIndex represents the offset into the available mColors that the
        // progress circle should currently display. As the progress circle is
        // animating, the mColorIndex moves by one to the next available color.
        private var mColorIndex = 0
        private var mStartingStartTrim = 0f
        private var mStartingEndTrim = 0f
        private var mStartingRotation = 0f
        private var mShowArrow = false
        private var mArrow: Path? = null
        private var mArrowScale = 0f
        private var mRingCenterRadius = 0.0
        private var mArrowWidth = 0
        private var mArrowHeight = 0
        private var mAlpha = 0
        private val mCirclePaint = Paint()
        private var mBackgroundColor = 0

        constructor(callback: Callback, drawable: Drawable) {
            mCallback = callback
            mDrawable = drawable

            mPaint.strokeCap = Paint.Cap.SQUARE
            mPaint.isAntiAlias = true
            mPaint.style = Paint.Style.STROKE
            mArrowPaint.style = Paint.Style.FILL
            mArrowPaint.isAntiAlias = true
        }

        fun setBackgroundColor(color: Int) {
            mBackgroundColor = color
        }

        /**
         * Set the dimensions of the arrowhead.
         *
         * @param width Width of the hypotenuse of the arrow head
         * @param height Height of the arrow point
         */
        fun setArrowDimensions(width: Float, height: Float) {
            mArrowWidth = width.toInt()
            mArrowHeight = height.toInt()
        }

        /**
         * Draw the progress spinner
         */
        fun draw(c: Canvas, bounds: Rect) {
            val arcBounds = mTempBounds
            arcBounds.set(bounds)
            arcBounds.inset(mStrokeInset, mStrokeInset)
            val startAngle = (mStartTrim + mRotation) * 360
            val endAngle = (mEndTrim + mRotation) * 360
            val sweepAngle = endAngle - startAngle
            mPaint.color = mColors[mColorIndex]
            c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint)
            drawTriangle(c, startAngle, sweepAngle, bounds)
            if (mAlpha < 255) {
                mCirclePaint.color = mBackgroundColor
                mCirclePaint.alpha = 255 - mAlpha
                c.drawCircle(
                    bounds.exactCenterX(), bounds.exactCenterY(), (bounds.width() / 2).toFloat(),
                    mCirclePaint
                )
            }
        }

        private fun drawTriangle(c: Canvas, startAngle: Float, sweepAngle: Float, bounds: Rect) {
            if (mShowArrow) {
                if (mArrow == null) {
                    mArrow = Path()
                    mArrow!!.fillType = Path.FillType.EVEN_ODD
                } else {
                    mArrow!!.reset()
                }
                // Adjust the position of the triangle so that it is inset as
                // much as the arc, but also centered on the arc.
                val inset = mStrokeInset.toInt() / 2 * mArrowScale
                val x = (mRingCenterRadius * cos(0.0) + bounds.exactCenterX()).toFloat()
                val y = (mRingCenterRadius * sin(0.0) + bounds.exactCenterY()).toFloat()
                // Update the path each time. This works around an issue in SKIA
                // where concatenating a rotation matrix to a scale matrix
                // ignored a starting negative rotation. This appears to have
                // been fixed as of API 21.
                mArrow!!.moveTo(0f, 0f)
                mArrow!!.lineTo(mArrowWidth * mArrowScale, 0f)
                mArrow!!.lineTo(
                    mArrowWidth * mArrowScale / 2, mArrowHeight
                            * mArrowScale
                )
                mArrow!!.offset(x - inset, y)
                mArrow!!.close()
                // draw a triangle
                mArrowPaint.color = mColors[mColorIndex]
                c.rotate(
                    startAngle + sweepAngle - ARROW_OFFSET_ANGLE, bounds.exactCenterX(),
                    bounds.exactCenterY()
                )
                c.drawPath(mArrow!!, mArrowPaint)
            }
        }

        /**
         * Set the colors the progress spinner alternates between.
         *
         * @param colors Array of integers describing the colors. Must be non-`null`.
         */
        fun setColors(colors: IntArray) {
            mColors = colors
            // if colors are reset, make sure to reset the color index as well
            setColorIndex(0)
        }

        /**
         * @param index Index into the color array of the color to display in
         * the progress spinner.
         */
        fun setColorIndex(index: Int) {
            mColorIndex = index
        }

        /**
         * Proceed to the next available ring color. This will automatically
         * wrap back to the beginning of colors.
         */
        fun goToNextColor() {
            mColorIndex = (mColorIndex + 1) % mColors.size
        }

        fun setColorFilter(filter: ColorFilter?) {
            mPaint.colorFilter = filter
            invalidateSelf()
        }

        /**
         * @param alpha Set the alpha of the progress spinner and associated arrowhead.
         */
        fun setAlpha(alpha: Int) {
            mAlpha = alpha
        }

        /**
         * @return Current alpha of the progress spinner and arrowhead.
         */
        fun getAlpha(): Int {
            return mAlpha
        }

        /**
         * @param strokeWidth Set the stroke width of the progress spinner in pixels.
         */
        fun setStrokeWidth(strokeWidth: Float) {
            mStrokeWidth = strokeWidth
            mPaint.strokeWidth = strokeWidth
            invalidateSelf()
        }

        fun getStrokeWidth(): Float {
            return mStrokeWidth
        }

        fun setStartTrim(startTrim: Float) {
            mStartTrim = startTrim
            invalidateSelf()
        }

        fun getStartTrim(): Float {
            return mStartTrim
        }

        fun getStartingStartTrim(): Float {
            return mStartingStartTrim
        }

        fun getStartingEndTrim(): Float {
            return mStartingEndTrim
        }

        fun setEndTrim(endTrim: Float) {
            mEndTrim = endTrim
            invalidateSelf()
        }

        fun getEndTrim(): Float {
            return mEndTrim
        }

        fun setRotation(rotation: Float) {
            mRotation = rotation
            invalidateSelf()
        }

        fun getRotation(): Float {
            return mRotation
        }

        fun setInsets(width: Int, height: Int) {
            val minEdge = min(width, height).toFloat()
            val insets: Float = if (mRingCenterRadius <= 0 || minEdge < 0) {
                ceil((mStrokeWidth / 2.0f).toDouble()).toFloat()
            } else {
                (minEdge / 2.0f - mRingCenterRadius).toFloat()
            }
            mStrokeInset = insets
        }

        fun getInsets(): Float {
            return mStrokeInset
        }

        /**
         * @param centerRadius Inner radius in px of the circle the progress
         * spinner arc traces.
         */
        fun setCenterRadius(centerRadius: Double) {
            mRingCenterRadius = centerRadius
        }

        fun getCenterRadius(): Double {
            return mRingCenterRadius
        }

        /**
         * @param show Set to true to show the arrow head on the progress spinner.
         */
        fun setShowArrow(show: Boolean) {
            if (mShowArrow != show) {
                mShowArrow = show
                invalidateSelf()
            }
        }

        /**
         * @param scale Set the scale of the arrowhead for the spinner.
         */
        fun setArrowScale(scale: Float) {
            if (scale != mArrowScale) {
                mArrowScale = scale
                invalidateSelf()
            }
        }

        /**
         * @return The amount the progress spinner is currently rotated, between [0..1].
         */
        fun getStartingRotation(): Float {
            return mStartingRotation
        }

        /**
         * If the start / end trim are offset to begin with, store them so that
         * animation starts from that offset.
         */
        fun storeOriginals() {
            mStartingStartTrim = mStartTrim
            mStartingEndTrim = mEndTrim
            mStartingRotation = mRotation
        }

        /**
         * Reset the progress spinner to default rotation, start and end angles.
         */
        fun resetOriginals() {
            mStartingStartTrim = 0f
            mStartingEndTrim = 0f
            mStartingRotation = 0f
            setStartTrim(0f)
            setEndTrim(0f)
            setRotation(0f)
        }

        private fun invalidateSelf() {
            mCallback.invalidateDrawable(mDrawable)
        }
    }

    /**
     * Squishes the interpolation curve into the second half of the animation.
     */
    private class EndCurveInterpolator : AccelerateDecelerateInterpolator() {
        override fun getInterpolation(input: Float): Float {
            return super.getInterpolation(max(0f, (input - 0.5f) * 2.0f))
        }
    }

    /**
     * Squishes the interpolation curve into the first half of the animation.
     */
    private class StartCurveInterpolator : AccelerateDecelerateInterpolator() {
        override fun getInterpolation(input: Float): Float {
            return super.getInterpolation(min(1f, input * 2.0f))
        }
    }

    companion object {
        private val LINEAR_INTERPOLATOR: Interpolator = LinearInterpolator()
        private val END_CURVE_INTERPOLATOR: Interpolator = EndCurveInterpolator()
        private val START_CURVE_INTERPOLATOR: Interpolator = StartCurveInterpolator()
        private val EASE_INTERPOLATOR: Interpolator = AccelerateDecelerateInterpolator()

        // Maps to ProgressBar default style
        const val DEFAULT = 0

        // Maps to ProgressBar.Small style
        const val SMALL = 2

        // Maps to ProgressBar.Large style
        const val LARGE = 1

        // Maps to ProgressBar.Small style
        private const val CIRCLE_DIAMETER_SMALL = 28
        private const val CENTER_RADIUS_SMALL = 6.4f //should add up to 10 when + stroke_width
        private const val STROKE_WIDTH_SMALL = 1.0f

        // Maps to ProgressBar default style
        private const val CIRCLE_DIAMETER = 64
        private const val CENTER_RADIUS = 19f
        private const val STROKE_WIDTH = 3.8f

        // Maps to ProgressBar.Large style
        private const val CIRCLE_DIAMETER_LARGE = 108
        private const val CENTER_RADIUS_LARGE = 30f
        private const val STROKE_WIDTH_LARGE = 6f

        /** The duration of a single progress spin in milliseconds.  */
        private const val ANIMATION_DURATION = 1000 * 80 / 60

        /** The number of points in the progress "star".  */
        private const val NUM_POINTS = 5f
        private const val ARROW_OFFSET_ANGLE = 5f

        /** Layout info for the arrowhead for the small spinner in dp  */
        private const val ARROW_WIDTH_SMALL = 6
        private const val ARROW_HEIGHT_SMALL = 4

        /** Layout info for the arrowhead in dp  */
        private const val ARROW_WIDTH = 12
        private const val ARROW_HEIGHT = 6

        /** Layout info for the arrowhead for the large spinner in dp  */
        private const val ARROW_WIDTH_LARGE = 24
        private const val ARROW_HEIGHT_LARGE = 14
        private const val MAX_PROGRESS_ARC = .8f
    }
}