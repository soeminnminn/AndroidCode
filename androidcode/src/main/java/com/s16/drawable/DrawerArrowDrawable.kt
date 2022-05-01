package com.s16.drawable

import android.content.Context
import android.graphics.*
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import kotlin.math.sqrt


/** A drawable that rotates between a drawer icon and a back arrow based on parameter. */
class DrawerArrowDrawable(context: Context, rounded: Boolean) : Drawable() {

    private var mTopLine: BridgingLine? = null
    private var mMiddleLine: BridgingLine? = null
    private var mBottomLine: BridgingLine? = null

    private var mBounds: Rect? = null
    private var mHalfStrokeWidthPixel = 0f
    private var mLinePaint: Paint? = null
    private var mRounded = rounded

    private var mFlip = false
    private var mParameter = 0f

    // Helper fields during drawing calculations.
    private var mVx = 0f  // Helper fields during drawing calculations.
    private var mVy = 0f  // Helper fields during drawing calculations.
    private var mMagnitude = 0f  // Helper fields during drawing calculations.
    private var mParamA = 0f  // Helper fields during drawing calculations.
    private var mParamB = 0f
    private var mCoordsA = floatArrayOf(0f, 0f)
    private var mCoordsB = floatArrayOf(0f, 0f)

    /**
     * Joins two [Path]s as if they were one where the first 50% of the path is `PathFirst` and the second 50% of the path is `pathSecond`.
     */
    private class JoinedPath constructor(pathFirst: Path, pathSecond: Path) {
        private val measureFirst = PathMeasure(pathFirst, false)
        private val measureSecond = PathMeasure(pathSecond, false)
        private val lengthFirst = measureFirst.length
        private val lengthSecond = measureSecond.length

        /**
         * Returns a point on this curve at the given `parameter`.
         * For `parameter` values less than .5f, the first path will drive the point.
         * For `parameter` values greater than .5f, the second path will drive the point.
         * For `parameter` equal to .5f, the point will be the point where the two
         * internal paths connect.
         */
        fun getPointOnLine(parameter: Float, coords: FloatArray) {
            var param = parameter
            if (param <= .5f) {
                param *= 2f
                measureFirst.getPosTan(lengthFirst * param, coords, null)
            } else {
                param -= .5f
                param *= 2f
                measureSecond.getPosTan(lengthSecond * param, coords, null)
            }
        }
    }

    /** Draws a line between two [JoinedPath]s at distance `parameter` along each path.  */
    private inner class BridgingLine constructor(
        private val pathA: JoinedPath,
        private val pathB: JoinedPath
    ) {
        /**
         * Draw a line between the points defined on the paths backing `measureA` and
         * `measureB` at the current parameter.
         */
        fun draw(canvas: Canvas) {
            pathA.getPointOnLine(mParameter, mCoordsA)
            pathB.getPointOnLine(mParameter, mCoordsB)
            if (mRounded) insetPointsForRoundCaps()

            mLinePaint?.let {
                canvas.drawLine(
                    mCoordsA[0],
                    mCoordsA[1],
                    mCoordsB[0],
                    mCoordsB[1],
                    it
                )
            }
        }

        /**
         * Insets the end points of the current line to account for the protruding
         * ends drawn for [Cap.ROUND] style lines.
         */
        private fun insetPointsForRoundCaps() {
            mVx = mCoordsB.get(0) - mCoordsA.get(0)
            mVy = mCoordsB.get(1) - mCoordsA.get(1)
            mMagnitude = sqrt(mVx * mVx + mVy * mVy) as Float
            mParamA = (mMagnitude - mHalfStrokeWidthPixel) / mMagnitude
            mParamB = mHalfStrokeWidthPixel / mMagnitude
            mCoordsA[0] = mCoordsB[0] - mVx * mParamA
            mCoordsA[1] = mCoordsB[1] - mVy * mParamA
            mCoordsB[0] = mCoordsB[0] - mVx * mParamB
            mCoordsB[1] = mCoordsB[1] - mVy * mParamB
        }
    }

    init {
        val resources = context.resources
        val density = resources.displayMetrics.density
        val strokeWidthPixel = STROKE_WIDTH_DP * density
        mHalfStrokeWidthPixel = strokeWidthPixel / 2

        mLinePaint = Paint(Paint.SUBPIXEL_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = if (rounded) Paint.Cap.ROUND else Paint.Cap.BUTT
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPixel
        }

        val dimen = (DIMEN_DP * density).toInt()
        mBounds = Rect(0, 0, dimen, dimen)

        var joinedA: JoinedPath
        var joinedB: JoinedPath

        // Top
        var first = Path()
        first.moveTo(5.042f, 20f)
        first.rCubicTo(8.125f, -16.317f, 39.753f, -27.851f, 55.49f, -2.765f)
        var second = Path()
        second.moveTo(60.531f, 17.235f)
        second.rCubicTo(11.301f, 18.015f, -3.699f, 46.083f, -23.725f, 43.456f)
        scalePath(first, density)
        scalePath(second, density)
        joinedA = JoinedPath(first, second)

        first = Path()
        first.moveTo(64.959f, 20f)
        first.rCubicTo(4.457f, 16.75f, 1.512f, 37.982f, -22.557f, 42.699f)
        second = Path()
        second.moveTo(42.402f, 62.699f)
        second.cubicTo(18.333f, 67.418f, 8.807f, 45.646f, 8.807f, 32.823f)
        scalePath(first, density)
        scalePath(second, density)
        joinedB = JoinedPath(first, second)
        mTopLine = BridgingLine(joinedA, joinedB)

        // Middle
        first = Path()
        first.moveTo(5.042f, 35f)
        first.cubicTo(5.042f, 20.333f, 18.625f, 6.791f, 35f, 6.791f)
        second = Path()
        second.moveTo(35f, 6.791f)
        second.rCubicTo(16.083f, 0f, 26.853f, 16.702f, 26.853f, 28.209f)
        scalePath(first, density)
        scalePath(second, density)
        joinedA = JoinedPath(first, second)

        first = Path()
        first.moveTo(64.959f, 35f)
        first.rCubicTo(0f, 10.926f, -8.709f, 26.416f, -29.958f, 26.416f)
        second = Path()
        second.moveTo(35f, 61.416f)
        second.rCubicTo(-7.5f, 0f, -23.946f, -8.211f, -23.946f, -26.416f)
        scalePath(first, density)
        scalePath(second, density)
        joinedB = JoinedPath(first, second)
        mMiddleLine = BridgingLine(joinedA, joinedB)

        // Bottom
        first = Path()
        first.moveTo(5.042f, 50f)
        first.cubicTo(2.5f, 43.312f, 0.013f, 26.546f, 9.475f, 17.346f)
        second = Path()
        second.moveTo(9.475f, 17.346f)
        second.rCubicTo(9.462f, -9.2f, 24.188f, -10.353f, 27.326f, -8.245f)
        scalePath(first, density)
        scalePath(second, density)
        joinedA = JoinedPath(first, second)

        first = Path()
        first.moveTo(64.959f, 50f)
        first.rCubicTo(-7.021f, 10.08f, -20.584f, 19.699f, -37.361f, 12.74f)
        second = Path()
        second.moveTo(27.598f, 62.699f)
        second.rCubicTo(-15.723f, -6.521f, -18.8f, -23.543f, -18.8f, -25.642f)
        scalePath(first, density)
        scalePath(second, density)
        joinedB = JoinedPath(first, second)
        mBottomLine = BridgingLine(joinedA, joinedB)
    }

    override fun getIntrinsicHeight(): Int {
        return mBounds?.height() ?: 0
    }

    override fun getIntrinsicWidth(): Int {
        return mBounds?.width() ?: 0
    }

    override fun draw(canvas: Canvas) {
        if (mFlip) {
            canvas.save()
            canvas.scale(
                1f, -1f, (intrinsicWidth / 2).toFloat(),
                (intrinsicHeight / 2).toFloat()
            )
        }
        mTopLine!!.draw(canvas)
        mMiddleLine!!.draw(canvas)
        mBottomLine!!.draw(canvas)
        if (mFlip) canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        mLinePaint!!.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mLinePaint!!.colorFilter = cf
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    fun setStrokeColor(color: Int) {
        mLinePaint!!.color = color
        invalidateSelf()
    }

    /**
     * Sets the rotation of this drawable based on `parameter` between 0 and 1. Usually driven
     * via [DrawerListener.onDrawerSlide]'s `slideOffset` parameter.
     */
    fun setParameter(parameter: Float) {
        require(!(parameter > 1 || parameter < 0)) { "Value must be between 1 and zero inclusive!" }
        this.mParameter = parameter
        invalidateSelf()
    }

    /**
     * When false, rotates from 3 o'clock to 9 o'clock between a drawer icon and a back arrow.
     * When true, rotates from 9 o'clock to 3 o'clock between a back arrow and a drawer icon.
     */
    fun setFlip(flip: Boolean) {
        this.mFlip = flip
        invalidateSelf()
    }

    companion object {
        /** Paths were generated at a 3px/dp density; this is the scale factor for different densities.  */
        private const val PATH_GEN_DENSITY = 3f

        /** Paths were generated with at this size for [DrawerArrowDrawable.PATH_GEN_DENSITY].  */
        private const val DIMEN_DP = 23.5f

        /**
         * Paths were generated targeting this stroke width to form the arrowhead properly, modification
         * may cause the arrow to not for nicely.
         */
        private const val STROKE_WIDTH_DP = 2f

        /**
         * Scales the paths to the given screen density. If the density matches the
         * [DrawerArrowDrawable.PATH_GEN_DENSITY], no scaling needs to be done.
         */
        private fun scalePath(path: Path, density: Float) {
            if (density == PATH_GEN_DENSITY) return
            val scaleMatrix = Matrix()
            scaleMatrix.setScale(density / PATH_GEN_DENSITY, density / PATH_GEN_DENSITY, 0f, 0f)
            path.transform(scaleMatrix)
        }
    }
}