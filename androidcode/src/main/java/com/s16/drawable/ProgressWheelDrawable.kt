package com.s16.drawable

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.TextUtils
import kotlin.math.round


class ProgressWheelDrawable : Drawable(), Animatable {

    //Sizes (with defaults)
    private var mFullRadius = 100
    private var mCircleRadius = 80
    private var mBarLength = 60
    private var mBarWidth = 20
    private var mRimWidth = 20
    private var mTextSize = 20

    //Colors (with defaults)
    private var mBarColor = -0x56000000
    private var mCircleColor = 0x00000000
    private var mRimColor = -0x55222223
    private var mTextColor = -0x1000000

    //Paints
    private val mBarPaint: Paint = Paint()
    private val mCirclePaint: Paint = Paint()
    private val mRimPaint: Paint = Paint()
    private val mTextPaint: Paint = Paint()

    //Rectangles
    private var mCircleBounds = RectF()

    //Animation
    //The amount of pixels to move the bar by on each draw
    private var mSpinSpeed = 3
    private var mProgress = 0
    private val mUpdater: Runnable = object : Runnable {
        override fun run() {
            if (mRunning) {
                mProgress += mSpinSpeed
                if (mProgress > 360) {
                    mProgress = 0
                }
                scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_DURATION)
            }
            invalidateSelf()
        }
    }
    private var mRunning = false

    //Other
    private var mText: CharSequence = ""
    private var mSplitText = arrayOf<String>()

    /**
     * Set the properties of the paints we're using to
     * draw the progress wheel
     */
    init {
        mBarPaint.color = mBarColor
        mBarPaint.isAntiAlias = true
        mBarPaint.style = Paint.Style.STROKE
        mBarPaint.strokeWidth = mBarWidth.toFloat()
        mRimPaint.color = mRimColor
        mRimPaint.isAntiAlias = true
        mRimPaint.style = Paint.Style.STROKE
        mRimPaint.strokeWidth = mRimWidth.toFloat()
        mCirclePaint.color = mCircleColor
        mCirclePaint.isAntiAlias = true
        mCirclePaint.style = Paint.Style.FILL
        mTextPaint.color = mTextColor
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.isAntiAlias = true
        mTextPaint.textSize = mTextSize.toFloat()
    }

    /**
     * Set the bounds of the component
     */
    private fun setupBounds() {
        val padding = Rect()
        getPadding(padding)
        mCircleBounds = RectF(
            (padding.left + mBarWidth).toFloat(),
            (padding.top + mBarWidth).toFloat(),
            this.getWidth() - padding.right - mBarWidth,
            this.getHeight() - padding.bottom - mBarWidth
        )
        mFullRadius = ((this.getWidth() - padding.right - mBarWidth) / 2).toInt()
        mCircleRadius = mFullRadius - mBarWidth + 1
    }

    override fun start() {
        if (isRunning) return
        scheduleSelf(mUpdater, SystemClock.uptimeMillis() + FRAME_DURATION)
        invalidateSelf()
    }

    override fun stop() {
        if (!isRunning) return
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

    /**
     * Increment the progress by 1 (of 360)
     */
    fun incrementProgress() {
        mRunning = false
        mProgress++
        setText(round(mProgress.toFloat() / 360 * 100).toString() + "%")
        scheduleSelf(mUpdater, SystemClock.uptimeMillis() + FRAME_DURATION)
        invalidateSelf()
    }

    /**
     * Set the progress to a specific value
     */
    fun setProgress(i: Int) {
        mRunning = false
        mProgress = i
        scheduleSelf(mUpdater, SystemClock.uptimeMillis() + FRAME_DURATION)
        invalidateSelf()
    }

    /**
     * Reset the count (in increment mode)
     */
    fun resetCount() {
        mProgress = 0
        setText("0%")
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect?) {
        setupBounds()
    }

    override fun draw(canvas: Canvas) {
        val padding = Rect()
        getPadding(padding)
        val saveCount: Int = canvas.save()
        val dx: Float = (bounds.width() - getWidth()) / 2
        val dy: Float = (bounds.height() - getHeight()) / 2
        canvas.translate(dx, dy)

        //Draw the rim
        canvas.drawArc(mCircleBounds, 360f, 360f, false, mRimPaint)
        //Draw the bar
        if (mRunning) {
            canvas.drawArc(
                mCircleBounds, mProgress - 90f, mBarLength.toFloat(), false,
                mBarPaint
            )
        } else {
            canvas.drawArc(mCircleBounds, -90f, mProgress.toFloat(), false, mBarPaint)
        }
        //Draw the inner circle
        canvas.drawCircle(
            mCircleBounds.width() / 2 + mRimWidth + padding.left,
            mCircleBounds.height() / 2 + mRimWidth + padding.top,
            mCircleRadius.toFloat(),
            mCirclePaint
        )
        //Draw the text (attempts to center it horizontally and vertically)
        var offsetNum = 0
        for (s in mSplitText) {
            val offset = mTextPaint.measureText(s) / 2
            canvas.drawText(
                s, this.getWidth() / 2 - offset, this.getHeight() / 2 + mTextSize * offsetNum
                        - (mSplitText.size - 1) * (mTextSize / 2), mTextPaint
            )
            offsetNum++
        }
        canvas.restoreToCount(saveCount)
    }

    //----------------------------------
    //Getters + setters
    //----------------------------------
    override fun setAlpha(alpha: Int) {
        mBarPaint.alpha = alpha
        mRimPaint.alpha = alpha
        mCirclePaint.alpha = alpha
        mTextPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    private fun getWidth(): Float {
        return Math.min(bounds.width(), bounds.height()).toFloat()
    }

    private fun getHeight(): Float {
        return Math.min(bounds.width(), bounds.height()).toFloat()
    }

    fun setText(text: CharSequence) {
        mText = text
        if (!TextUtils.isEmpty(mText)) {
            mSplitText = mText.toString().split("\n").toTypedArray()
        }
        invalidateSelf()
    }

    fun getCircleRadius(): Int {
        return mCircleRadius
    }

    fun setCircleRadius(circleRadius: Int) {
        mCircleRadius = circleRadius
        invalidateSelf()
    }

    fun getBarLength(): Int {
        return mBarLength
    }

    fun setBarLength(barLength: Int) {
        mBarLength = barLength
        invalidateSelf()
    }

    fun getBarWidth(): Int {
        return mBarWidth
    }

    fun setBarWidth(barWidth: Int) {
        mBarWidth = barWidth
        mBarPaint.strokeWidth = barWidth.toFloat()
        invalidateSelf()
    }

    fun getTextSize(): Int {
        return mTextSize
    }

    fun setTextSize(textSize: Int) {
        mTextSize = textSize
        mTextPaint.textSize = mTextSize.toFloat()
        invalidateSelf()
    }

    fun getTextColor(): Int {
        return mTextColor
    }

    fun setTextColor(textColor: Int) {
        mTextColor = textColor
        mTextPaint.color = mTextColor
        invalidateSelf()
    }

    fun getBarColor(): Int {
        return mBarColor
    }

    fun setBarColor(barColor: Int) {
        mBarColor = barColor
        mBarPaint.color = mBarColor
        invalidateSelf()
    }

    fun getCircleColor(): Int {
        return mCircleColor
    }

    fun setCircleColor(circleColor: Int) {
        mCircleColor = circleColor
        mCirclePaint.color = mCircleColor
        invalidateSelf()
    }

    fun getRimColor(): Int {
        return mRimColor
    }

    fun setRimColor(rimColor: Int) {
        mRimColor = rimColor
        mRimPaint.color = mRimColor
        invalidateSelf()
    }

    fun getRimShader(): Shader? {
        return mRimPaint.shader
    }

    fun setRimShader(shader: Shader?) {
        mRimPaint.shader = shader
        invalidateSelf()
    }

    fun getSpinSpeed(): Int {
        return mSpinSpeed
    }

    fun setSpinSpeed(spinSpeed: Int) {
        mSpinSpeed = spinSpeed
        invalidateSelf()
    }

    fun getRimWidth(): Int {
        return mRimWidth
    }

    fun setRimWidth(rimWidth: Int) {
        mRimWidth = rimWidth
        mRimPaint.strokeWidth = mRimWidth.toFloat()
        invalidateSelf()
    }

    companion object {
        private const val FRAME_DURATION = (1000 / 60).toLong()
    }
}