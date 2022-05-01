package com.s16.drawable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.view.animation.LinearInterpolator
import java.util.*
import kotlin.math.*


/**
 * Creates a new ripple drawable with the specified ripple color and
 * optional content and mask drawables.
 *
 * @param color The ripple color
 */
class RippleDrawable(color: ColorStateList) : Drawable() {

    /** Current ripple effect bounds, used to constrain ripple effects.  */
    private val mHotspotBounds: Rect = Rect()

    private var mColor = ColorStateList.valueOf(Color.MAGENTA)

    private var mMaxRadius = RADIUS_AUTO

    private var mContent: Drawable? = null

    /** The masking layer, e.g. the layer with id R.id.mask.  */
    private var mMask: Drawable? = null

    /** The current background. May be actively animating or pending entry.  */
    private var mBackground: RippleBackground? = null

    /** Whether we expect to draw a background when visible.  */
    private var mBackgroundActive = false

    /** The current ripple. May be actively animating or pending entry.  */
    private var mRipple: Ripple? = null

    /** Whether we expect to draw a ripple when visible.  */
    private var mRippleActive = false

    // Hotspot coordinates that are awaiting activation.
    private var mPendingX = 0f
    private var mPendingY = 0f
    private var mHasPending = false

    /**
     * Lazily-created array of actively animating ripples. Inactive ripples are
     * pruned during draw(). The locations of these will not change.
     */
    private var mExitingRipples: Array<Ripple?> = arrayOf()
    private var mExitingRipplesCount = 0

    /** Paint used to control appearance of ripples.  */
    private var mRipplePaint: Paint? = null

    /** Paint used to control reveal layer masking.  */
    private var mMaskingPaint: Paint? = null

    /** Target density of the display into which ripples are drawn.  */
    private var mDensity = 1.0f

    /** Whether bounds are being overridden.  */
    private var mOverrideBounds = false

    /**
     * Whether the next draw MUST draw something to canvas. Used to work around
     * a bug in hardware invalidation following a render thread-accelerated
     * animation.
     */
    private var mNeedsDraw = false

    constructor(color: ColorStateList, content: Drawable) : this(color) {
        mContent = content
    }

    init {
        setColor(color)
    }

    override fun jumpToCurrentState() {
        super.jumpToCurrentState()
        mRipple?.jump()
        mBackground?.jump()
        val needsDraw: Boolean = cancelExitingRipples()
        mNeedsDraw = needsDraw
        invalidateSelf()
    }

    private fun cancelExitingRipples(): Boolean {
        val count = mExitingRipplesCount
        val ripples: Array<Ripple?> = mExitingRipples
        for (i in 0 until count) {
            ripples[i]?.cancel()
        }
        Arrays.fill(ripples, 0, count, null)
        mExitingRipplesCount = 0
        return false
    }

    override fun setAlpha(alpha: Int) {
        mColor.withAlpha(alpha)
    }

    override fun getAlpha(): Int {
        return Color.alpha(mColor.defaultColor)
    }

    override fun setColorFilter(cf: ColorFilter?) {
        //TODO how to implement?
    }

    override fun getOpacity(): Int {
        // Worst-case scenario.
        return PixelFormat.TRANSLUCENT
    }

    override fun onStateChange(stateSet: IntArray): Boolean {
        val changed = super.onStateChange(stateSet)
        var enabled = false
        var pressed = false
        var focused = false
        for (state in stateSet) {
            if (state == android.R.attr.state_enabled) {
                enabled = true
            }
            if (state == android.R.attr.state_focused) {
                focused = true
            }
            if (state == android.R.attr.state_pressed) {
                pressed = true
            }
        }
        setRippleActive(enabled && pressed)
        setBackgroundActive(focused || enabled && pressed)
        return changed
    }

    private fun setRippleActive(active: Boolean) {
        if (mRippleActive != active) {
            mRippleActive = active
            if (active) {
                tryRippleEnter()
            } else {
                tryRippleExit()
            }
        }
    }

    private fun setBackgroundActive(active: Boolean) {
        if (mBackgroundActive != active) {
            mBackgroundActive = active
            if (active) {
                tryBackgroundEnter()
            } else {
                tryBackgroundExit()
            }
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        if (!mOverrideBounds) {
            mHotspotBounds.set(bounds!!)
            onHotspotBoundsChanged()
        }
        invalidateSelf()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (!visible) {
            clearHotspots()
        } else if (changed) {
            // If we just became visible, ensure the background and ripple
            // visibilities are consistent with their internal states.
            if (mRippleActive) {
                tryRippleEnter()
            }
            if (mBackgroundActive) {
                tryBackgroundEnter()
            }
        }
        return changed
    }

    override fun setHotspot(x: Float, y: Float) {
        if (mRipple == null || mBackground == null) {
            mPendingX = x
            mPendingY = y
            mHasPending = true
        }
        mRipple?.move(x, y)
    }

    /**
     * Attempts to start an enter animation for the active hotspot. Fails if
     * there are too many animating ripples.
     */
    private fun tryRippleEnter() {
        if (mExitingRipplesCount >= MAX_RIPPLES) {
            // This should never happen unless the user is tapping like a maniac
            // or there is a bug that's preventing ripples from being removed.
            return
        }
        if (mRipple == null) {
            val x: Float
            val y: Float
            if (mHasPending) {
                mHasPending = false
                x = mPendingX
                y = mPendingY
            } else {
                x = mHotspotBounds.exactCenterX()
                y = mHotspotBounds.exactCenterY()
            }
            mRipple = Ripple(this, mHotspotBounds, x, y)
        }
        val color = mColor.getColorForState(state, Color.TRANSPARENT)
        mRipple!!.setup(mMaxRadius, color, mDensity)
        mRipple!!.enter()
    }

    /**
     * Attempts to start an exit animation for the active hotspot. Fails if
     * there is no active hotspot.
     */
    private fun tryRippleExit() {
        if (mRipple != null) {
            mExitingRipples[mExitingRipplesCount++] = mRipple!!
            mRipple!!.exit()
            mRipple = null
        }
    }

    /**
     * Cancels and removes the active ripple, all exiting ripples, and the
     * background. Nothing will be drawn after this method is called.
     */
    private fun clearHotspots() {
        var needsDraw = false
        if (mRipple != null) {
            needsDraw = false
            mRipple!!.cancel()
            mRipple = null
        }
        if (mBackground != null) {
            needsDraw = mBackground!!.isHardwareAnimating()
            mBackground!!.cancel()
            mBackground = null
        }
        needsDraw = needsDraw or cancelExitingRipples()
        mNeedsDraw = needsDraw
        invalidateSelf()
    }

    override fun setHotspotBounds(left: Int, top: Int, right: Int, bottom: Int) {
        mOverrideBounds = true
        mHotspotBounds[left, top, right] = bottom
        onHotspotBoundsChanged()
    }

    /** @hide
     */
    override fun getHotspotBounds(outRect: Rect) {
        outRect.set(mHotspotBounds)
    }

    /**
     * Notifies all the animating ripples that the hotspot bounds have changed.
     */
    private fun onHotspotBoundsChanged() {
        val count = mExitingRipplesCount
        val ripples: Array<Ripple?> = mExitingRipples
        for (i in 0 until count) {
            ripples[i]?.onHotspotBoundsChanged()
        }
        mRipple?.onHotspotBoundsChanged()
        mBackground?.onHotspotBoundsChanged()
    }

    /**
     * Creates an active hotspot at the specified location.
     */
    private fun tryBackgroundEnter() {
        if (mBackground == null) {
            mBackground = RippleBackground(this, mHotspotBounds)
        }
        val color = mColor.getColorForState(state, Color.TRANSPARENT)
        mBackground!!.setup(mMaxRadius, color, mDensity)
        mBackground!!.enter()
    }

    private fun tryBackgroundExit() {
        // Don't null out the background, we need it to draw!
        mBackground?.exit()
    }

    override fun draw(canvas: Canvas) {
        val hasMask = mMask != null
        val drawNonMaskContent = mContent != null //TODO if contentDrawable is not null
        val drawMask = hasMask && mMask!!.opacity != PixelFormat.OPAQUE
        val bounds = dirtyBounds
        val saveCount = canvas.save()
        canvas.clipRect(bounds)

        // If we have content, draw it into a layer first.
        if (drawNonMaskContent) {
            drawContentLayer(canvas, bounds, SRC_OVER)
        }

        // Next, try to draw the ripples (into a layer if necessary). If we need
        // to mask against the underlying content, set the xfermode to SRC_ATOP.
        val xfermode: PorterDuffXfermode =
            if (hasMask || !drawNonMaskContent) SRC_OVER else SRC_ATOP

        // If we have a background and a non-opaque mask, draw the masking layer.
        val backgroundLayer: Int = drawBackgroundLayer(canvas, bounds, xfermode, drawMask)
        if (backgroundLayer >= 0) {
            if (drawMask) {
                drawMaskingLayer(canvas, bounds, DST_IN)
            }
            canvas.restoreToCount(backgroundLayer)
        }

        // If we have ripples and a non-opaque mask, draw the masking layer.
        val rippleLayer: Int = drawRippleLayer(canvas, bounds, xfermode)
        if (rippleLayer >= 0) {
            if (drawMask) {
                drawMaskingLayer(canvas, bounds, DST_IN)
            }
            canvas.restoreToCount(rippleLayer)
        }

        // If we failed to draw anything and we just canceled animations, at
        // least draw a color so that hardware invalidation works correctly.
        if (mNeedsDraw) {
            canvas.drawColor(Color.TRANSPARENT)

            // Request another draw so we can avoid adding a transparent layer
            // during the next display list refresh.
            invalidateSelf()
        }
        mNeedsDraw = false
        canvas.restoreToCount(saveCount)
    }

    /**
     * Removes a ripple from the exiting ripple list.
     *
     * @param ripple the ripple to remove
     */
    fun removeRipple(ripple: Ripple) {
        // Ripple ripple ripple ripple. Ripple ripple.
        val ripples: Array<Ripple?> = mExitingRipples
        val count = mExitingRipplesCount
        val index = getRippleIndex(ripple)
        if (index >= 0) {
            System.arraycopy(ripples, index + 1, ripples, index, count - (index + 1))
            ripples[count - 1] = null
            mExitingRipplesCount--
            invalidateSelf()
        }
    }

    private fun getRippleIndex(ripple: Ripple): Int {
        val ripples: Array<Ripple?> = mExitingRipples
        val count = mExitingRipplesCount
        for (i in 0 until count) {
            if (ripples[i] === ripple) {
                return i
            }
        }
        return -1
    }

    private fun drawContentLayer(canvas: Canvas, bounds: Rect, mode: PorterDuffXfermode): Int {
        mContent!!.bounds = bounds
        mContent!!.draw(canvas)
        return -1
    }

    private fun drawBackgroundLayer(
        canvas: Canvas, bounds: Rect, mode: PorterDuffXfermode, drawMask: Boolean
    ): Int {
        val saveCount = -1
        if (mBackground != null && mBackground!!.shouldDraw()) {
            // TODO: We can avoid saveLayer here if we push the xfermode into
            val x = mHotspotBounds.exactCenterX()
            val y = mHotspotBounds.exactCenterY()
            canvas.translate(x, y)
            mBackground!!.draw(canvas, getRipplePaint())
            canvas.translate(-x, -y)
        }
        return saveCount
    }

    private fun drawRippleLayer(canvas: Canvas, bounds: Rect, mode: PorterDuffXfermode): Int {
        var drewRipples = false
        var restoreToCount = -1
        var restoreTranslate = -1

        // Draw ripples and update the animating ripples array.
        val count = mExitingRipplesCount
        val ripples: Array<Ripple?> = mExitingRipples
        for (i in 0..count) {
            val ripple: Ripple? = if (i < count) {
                ripples[i]
            } else if (mRipple != null) {
                mRipple
            } else {
                continue
            }

            // If we're masking the ripple layer, make sure we have a layer
            // first. This will merge SRC_OVER (directly) onto the canvas.
            if (restoreToCount < 0) {
                val maskingPaint: Paint? = getMaskingPaint(mode)
                val color = mColor.getColorForState(state, Color.TRANSPARENT)
                val alpha = Color.alpha(color)
                maskingPaint?.alpha = alpha / 2


                // Translate the canvas to the current hotspot bounds.
                restoreTranslate = canvas.save()
                canvas.translate(mHotspotBounds.exactCenterX(), mHotspotBounds.exactCenterY())
            }
            drewRipples = drewRipples or (ripple?.draw(canvas, getRipplePaint()) == true)
        }

        // Always restore the translation.
        if (restoreTranslate >= 0) {
            canvas.restoreToCount(restoreTranslate)
        }

        // If we created a layer with no content, merge it immediately.
        if (restoreToCount >= 0 && !drewRipples) {
            canvas.restoreToCount(restoreToCount)
            restoreToCount = -1
        }
        return restoreToCount
    }

    private fun drawMaskingLayer(canvas: Canvas, bounds: Rect, mode: PorterDuffXfermode): Int {
        // Ensure that DST_IN blends using the entire layer.
        canvas.drawColor(Color.TRANSPARENT)
        mMask!!.draw(canvas)
        return -1
    }

    private fun getRipplePaint(): Paint {
        if (mRipplePaint == null) {
            mRipplePaint = Paint()
            mRipplePaint!!.isAntiAlias = true
        }
        return mRipplePaint!!
    }

    private fun getMaskingPaint(xfermode: PorterDuffXfermode): Paint? {
        if (mMaskingPaint == null) {
            mMaskingPaint = Paint()
        }
        mMaskingPaint!!.xfermode = xfermode
        mMaskingPaint!!.alpha = 0xFF
        return mMaskingPaint
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param metrics The display metrics for this drawable.
     */
    private fun setTargetDensity(metrics: DisplayMetrics) {
        if (mDensity != metrics.density) {
            mDensity = metrics.density
            invalidateSelf()
        }
    }

    override fun isStateful(): Boolean {
        return true
    }

    fun setColor(color: ColorStateList) {
        mColor = color
        invalidateSelf()
    }

    override fun getDirtyBounds(): Rect {
        return bounds
    }

    /**
     * Sets the maximum ripple radius in pixels. The default value of
     * [.RADIUS_AUTO] defines the radius as the distance from the center
     * of the drawable bounds (or hotspot bounds, if specified) to a corner.
     *
     * @param maxRadius the maximum ripple radius in pixels or
     * [.RADIUS_AUTO] to automatically determine the maximum
     * radius based on the bounds
     * @see .getMaxRadius
     * @see .setHotspotBounds
     * @hide
     */
    fun setMaxRadius(maxRadius: Int) {
        require(!(maxRadius != RADIUS_AUTO && maxRadius < 0)) { "maxRadius must be RADIUS_AUTO or >= 0" }
        mMaxRadius = maxRadius
    }

    /**
     * @return the maximum ripple radius in pixels, or [.RADIUS_AUTO] if
     * the radius is determined automatically
     * @see .setMaxRadius
     * @hide
     */
    fun getMaxRadius(): Int {
        return mMaxRadius
    }

    /**
     * Interpolator with a smooth log deceleration
     */
    private class LogInterpolator : TimeInterpolator {

        override fun getInterpolation(input: Float): Float {
            return 1 - 400.0.pow(-input * 1.4).toFloat()
        }
    }

    /**
     * Creates a new ripple.
     */
    class Ripple(owner: RippleDrawable, bounds: Rect, startingX: Float, startingY: Float) {
        private val mOwner: RippleDrawable = owner

        /** Bounds used for computing max radius.  */
        private val mBounds: Rect = bounds

        /** Full-opacity color for drawing this ripple.  */
        private var mColorOpaque = 0

        /** Maximum ripple radius.  */
        private var mOuterRadius = 0f

        /** Screen density used to adjust pixel-based velocities.  */
        private var mDensity = 0f

        private var mStartingX = startingX
        private var mStartingY = startingY
        private var mClampedStartingX = 0f
        private var mClampedStartingY = 0f

        // Software animators.
        private var mAnimRadius: ObjectAnimator? = null
        private var mAnimOpacity: ObjectAnimator? = null
        private var mAnimX: ObjectAnimator? = null
        private var mAnimY: ObjectAnimator? = null

        // Temporary paint used for creating canvas properties.
        private var mTempPaint: Paint? = null

        // Software rendering properties.
        private var mOpacity = 1f
        private var mOuterX = 0f
        private var mOuterY = 0f

        // Values used to tween between the start and end positions.
        private var mTweenRadius = 0f
        private var mTweenX = 0f
        private var mTweenY = 0f

        /** Whether we have an explicit maximum radius.  */
        private var mHasMaxRadius = false

        /** Whether we were canceled externally and should avoid self-removal.  */
        private var mCanceled = false

        fun setup(maxRadius: Int, color: Int, density: Float) {
            mColorOpaque = color or -0x1000000
            if (maxRadius != -1) {
                mHasMaxRadius = true
                mOuterRadius = maxRadius.toFloat()
            } else {
                val halfWidth = mBounds.width() / 2.0f
                val halfHeight = mBounds.height() / 2.0f
                mOuterRadius =
                    sqrt((halfWidth * halfWidth + halfHeight * halfHeight).toDouble())
                        .toFloat()
            }
            mOuterX = 0f
            mOuterY = 0f
            mDensity = density
            clampStartingPosition()
        }

        private fun clampStartingPosition() {
            val cX = mBounds.exactCenterX()
            val cY = mBounds.exactCenterY()
            val dX = mStartingX - cX
            val dY = mStartingY - cY
            val r = mOuterRadius
            if (dX * dX + dY * dY > r * r) {
                // Point is outside the circle, clamp to the circumference.
                val angle = atan2(dY.toDouble(), dX.toDouble())
                mClampedStartingX = cX + (cos(angle) * r).toFloat()
                mClampedStartingY = cY + (sin(angle) * r).toFloat()
            } else {
                mClampedStartingX = mStartingX
                mClampedStartingY = mStartingY
            }
        }

        fun onHotspotBoundsChanged() {
            if (!mHasMaxRadius) {
                val halfWidth = mBounds.width() / 2.0f
                val halfHeight = mBounds.height() / 2.0f
                mOuterRadius =
                    sqrt((halfWidth * halfWidth + halfHeight * halfHeight).toDouble())
                        .toFloat()
                clampStartingPosition()
            }
        }

        fun setOpacity(a: Float) {
            mOpacity = a
            invalidateSelf()
        }

        fun getOpacity(): Float {
            return mOpacity
        }

        fun setRadiusGravity(r: Float) {
            mTweenRadius = r
            invalidateSelf()
        }

        fun getRadiusGravity(): Float {
            return mTweenRadius
        }

        fun setXGravity(x: Float) {
            mTweenX = x
            invalidateSelf()
        }

        fun getXGravity(): Float {
            return mTweenX
        }

        fun setYGravity(y: Float) {
            mTweenY = y
            invalidateSelf()
        }

        fun getYGravity(): Float {
            return mTweenY
        }

        /**
         * Draws the ripple centered at (0,0) using the specified paint.
         */
        fun draw(c: Canvas, p: Paint): Boolean {
            return drawSoftware(c, p)
        }

        private fun drawSoftware(c: Canvas, p: Paint): Boolean {
            var hasContent = false
            p.color = mColorOpaque
            val alpha = (255 * mOpacity + 0.5f).toInt()
            val radius: Float = lerp(0f, mOuterRadius, mTweenRadius)
            if (alpha > 0 && radius > 0) {
                val x: Float = lerp(
                    mClampedStartingX - mBounds.exactCenterX(), mOuterX, mTweenX
                )
                val y: Float = lerp(
                    mClampedStartingY - mBounds.exactCenterY(), mOuterY, mTweenY
                )
                p.alpha = alpha
                p.style = Paint.Style.FILL
                c.drawCircle(x, y, radius, p)
                hasContent = true
            }
            return hasContent
        }

        /**
         * Returns the maximum bounds of the ripple relative to the ripple center.
         */
        fun getBounds(bounds: Rect) {
            val outerX = mOuterX.toInt()
            val outerY = mOuterY.toInt()
            val r = mOuterRadius.toInt() + 1
            bounds[outerX - r, outerY - r, outerX + r] = outerY + r
        }

        /**
         * Specifies the starting position relative to the drawable bounds. No-op if
         * the ripple has already entered.
         */
        fun move(x: Float, y: Float) {
            mStartingX = x
            mStartingY = y
            clampStartingPosition()
        }

        /**
         * Starts the enter animation.
         */
        fun enter() {
            cancel()
            val radiusDuration =
                (1000 * Math.sqrt((mOuterRadius / WAVE_TOUCH_DOWN_ACCELERATION * mDensity).toDouble()) + 0.5).toInt()
            val radius = ObjectAnimator.ofFloat(this, "radiusGravity", 1f)
            //radius.setAutoCancel(true);
            radius.duration = radiusDuration.toLong()
            radius.interpolator = LINEAR_INTERPOLATOR
            radius.startDelay = RIPPLE_ENTER_DELAY
            val cX = ObjectAnimator.ofFloat(this, "xGravity", 1f)
            //cX.setAutoCancel(true);
            cX.duration = radiusDuration.toLong()
            cX.interpolator = LINEAR_INTERPOLATOR
            cX.startDelay = RIPPLE_ENTER_DELAY
            val cY = ObjectAnimator.ofFloat(this, "yGravity", 1f)
            //cY.setAutoCancel(true);
            cY.duration = radiusDuration.toLong()
            cY.interpolator = LINEAR_INTERPOLATOR
            cY.startDelay = RIPPLE_ENTER_DELAY
            mAnimRadius = radius
            mAnimX = cX
            mAnimY = cY

            // Enter animations always run on the UI thread, since it's unlikely
            // that anything interesting is happening until the user lifts their
            // finger.
            radius.start()
            cX.start()
            cY.start()
        }

        /**
         * Starts the exit animation.
         */
        fun exit() {
            cancel()
            val radius = lerp(0f, mOuterRadius, mTweenRadius)
            val remaining: Float = if (mAnimRadius != null && mAnimRadius!!.isRunning) {
                mOuterRadius - radius
            } else {
                mOuterRadius
            }
            val radiusDuration = (1000 * sqrt(
                (remaining / (WAVE_TOUCH_UP_ACCELERATION
                        + WAVE_TOUCH_DOWN_ACCELERATION) * mDensity).toDouble()
            ) + 0.5).toInt()
            val opacityDuration = (1000 * mOpacity / WAVE_OPACITY_DECAY_VELOCITY + 0.5f).toInt()
            exitSoftware(radiusDuration, opacityDuration)
        }


        /**
         * Jump all animations to their end state. The caller is responsible for
         * removing the ripple from the list of animating ripples.
         */
        fun jump() {
            mCanceled = true
            endSoftwareAnimations()
            mCanceled = false
        }

        private fun endSoftwareAnimations() {
            if (mAnimRadius != null) {
                mAnimRadius!!.end()
                mAnimRadius = null
            }
            if (mAnimOpacity != null) {
                mAnimOpacity!!.end()
                mAnimOpacity = null
            }
            if (mAnimX != null) {
                mAnimX!!.end()
                mAnimX = null
            }
            if (mAnimY != null) {
                mAnimY!!.end()
                mAnimY = null
            }
        }

        private fun getTempPaint(): Paint? {
            if (mTempPaint == null) {
                mTempPaint = Paint()
            }
            return mTempPaint
        }

        private fun exitSoftware(radiusDuration: Int, opacityDuration: Int) {
            val radiusAnim = ObjectAnimator.ofFloat(this, "radiusGravity", 1f)
            //radiusAnim.setAutoCancel(true);
            radiusAnim.duration = radiusDuration.toLong()
            radiusAnim.interpolator = DECEL_INTERPOLATOR
            val xAnim = ObjectAnimator.ofFloat(this, "xGravity", 1f)
            //xAnim.setAutoCancel(true);
            xAnim.duration = radiusDuration.toLong()
            xAnim.setInterpolator(DECEL_INTERPOLATOR)
            val yAnim = ObjectAnimator.ofFloat(this, "yGravity", 1f)
            //yAnim.setAutoCancel(true);
            yAnim.duration = radiusDuration.toLong()
            yAnim.interpolator = DECEL_INTERPOLATOR
            val opacityAnim = ObjectAnimator.ofFloat(this, "opacity", 0f)
            //opacityAnim.setAutoCancel(true);
            opacityAnim.duration = opacityDuration.toLong()
            opacityAnim.interpolator = LINEAR_INTERPOLATOR
            opacityAnim.addListener(mAnimationListener)
            mAnimRadius = radiusAnim
            mAnimOpacity = opacityAnim
            mAnimX = xAnim
            mAnimY = yAnim
            radiusAnim.start()
            opacityAnim.start()
            xAnim.start()
            yAnim.start()
        }

        /**
         * Cancels all animations. The caller is responsible for removing
         * the ripple from the list of animating ripples.
         */
        fun cancel() {
            mCanceled = true
            cancelSoftwareAnimations()
            mCanceled = false
        }

        private fun cancelSoftwareAnimations() {
            if (mAnimRadius != null) {
                mAnimRadius!!.cancel()
                mAnimRadius = null
            }
            if (mAnimOpacity != null) {
                mAnimOpacity!!.cancel()
                mAnimOpacity = null
            }
            if (mAnimX != null) {
                mAnimX!!.cancel()
                mAnimX = null
            }
            if (mAnimY != null) {
                mAnimY!!.cancel()
                mAnimY = null
            }
        }

        private fun removeSelf() {
            // The owner will invalidate itself.
            if (!mCanceled) {
                mOwner.removeRipple(this)
            }
        }

        private fun invalidateSelf() {
            mOwner.invalidateSelf()
        }

        private val mAnimationListener: AnimatorListenerAdapter =
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeSelf()
                }
            }
    }

    /**
     * Draws a Material ripple.
     */
    class RippleBackground(owner: RippleDrawable, bounds: Rect) {
        private val mOwner: RippleDrawable = owner

        /** Bounds used for computing max radius.  */
        private val mBounds: Rect = bounds

        /** Full-opacity color for drawing this ripple.  */
        private var mColorOpaque = 0

        /** Maximum alpha value for drawing this ripple.  */
        private var mColorAlpha = 0

        /** Maximum ripple radius.  */
        private var mOuterRadius = 0f

        /** Screen density used to adjust pixel-based velocities.  */
        private var mDensity = 0f

        // Software animators.
        private var mAnimOuterOpacity: ObjectAnimator? = null

        // Temporary paint used for creating canvas properties.
        private var mTempPaint: Paint? = null

        // Software rendering properties.
        private var mOuterOpacity = 0f
        private var mOuterX = 0f
        private var mOuterY = 0f

        /** Whether we should be drawing hardware animations.  */
        private var mHardwareAnimating = false

        /** Whether we can use hardware acceleration for the exit animation.  */
        private var mCanUseHardware = false

        /** Whether we have an explicit maximum radius.  */
        private var mHasMaxRadius = false

        fun setup(maxRadius: Int, color: Int, density: Float) {
            mColorOpaque = color or -0x1000000
            mColorAlpha = Color.alpha(color) / 2
            if (maxRadius != RADIUS_AUTO) {
                mHasMaxRadius = true
                mOuterRadius = maxRadius.toFloat()
            } else {
                val halfWidth = mBounds.width() / 2.0f
                val halfHeight = mBounds.height() / 2.0f
                mOuterRadius =
                    sqrt((halfWidth * halfWidth + halfHeight * halfHeight).toDouble())
                        .toFloat()
            }
            mOuterX = 0f
            mOuterY = 0f
            mDensity = density
        }

        fun isHardwareAnimating(): Boolean {
            return mHardwareAnimating
        }

        fun onHotspotBoundsChanged() {
            if (!mHasMaxRadius) {
                val halfWidth = mBounds.width() / 2.0f
                val halfHeight = mBounds.height() / 2.0f
                mOuterRadius =
                    Math.sqrt((halfWidth * halfWidth + halfHeight * halfHeight).toDouble())
                        .toFloat()
            }
        }

        fun setOuterOpacity(a: Float) {
            mOuterOpacity = a
            invalidateSelf()
        }

        fun getOuterOpacity(): Float {
            return mOuterOpacity
        }

        /**
         * Draws the ripple centered at (0,0) using the specified paint.
         */
        fun draw(c: Canvas, p: Paint): Boolean {
            return drawSoftware(c, p)
        }

        fun shouldDraw(): Boolean {
            val outerAlpha = (mColorAlpha * mOuterOpacity + 0.5f).toInt()
            return mCanUseHardware && mHardwareAnimating || outerAlpha > 0 && mOuterRadius > 0
        }

        private fun drawSoftware(c: Canvas, p: Paint): Boolean {
            var hasContent = false
            p.color = mColorOpaque
            val outerAlpha = (mColorAlpha * mOuterOpacity + 0.5f).toInt()
            if (outerAlpha > 0 && mOuterRadius > 0) {
                p.alpha = outerAlpha
                p.style = Paint.Style.FILL
                c.drawCircle(mOuterX, mOuterY, mOuterRadius, p)
                hasContent = true
            }
            return hasContent
        }

        /**
         * Returns the maximum bounds of the ripple relative to the ripple center.
         */
        fun getBounds(bounds: Rect) {
            val outerX = mOuterX.toInt()
            val outerY = mOuterY.toInt()
            val r = mOuterRadius.toInt() + 1
            bounds[outerX - r, outerY - r, outerX + r] = outerY + r
        }

        /**
         * Starts the enter animation.
         */
        fun enter() {
            cancel()
            val outerDuration = (1000 * 1.0f / WAVE_OUTER_OPACITY_ENTER_VELOCITY).toInt()
            val outer = ObjectAnimator.ofFloat(this, "outerOpacity", 0f, 1f)
            //outer.setAutoCancel(true);
            outer.duration = outerDuration.toLong()
            outer.interpolator = LINEAR_INTERPOLATOR
            mAnimOuterOpacity = outer

            // Enter animations always run on the UI thread, since it's unlikely
            // that anything interesting is happening until the user lifts their
            // finger.
            outer.start()
        }

        /**
         * Starts the exit animation.
         */
        fun exit() {
            cancel()

            // Scale the outer max opacity and opacity velocity based
            // on the size of the outer radius.
            val opacityDuration = (1000 / WAVE_OPACITY_DECAY_VELOCITY + 0.5f).toInt()
            val outerSizeInfluence: Float = constrain(
                (mOuterRadius - WAVE_OUTER_SIZE_INFLUENCE_MIN * mDensity)
                        / (WAVE_OUTER_SIZE_INFLUENCE_MAX * mDensity), 0f, 1f
            )
            val outerOpacityVelocity: Float = lerp(
                WAVE_OUTER_OPACITY_EXIT_VELOCITY_MIN,
                WAVE_OUTER_OPACITY_EXIT_VELOCITY_MAX, outerSizeInfluence
            )

            // Determine at what time the inner and outer opacity intersect.
            // inner(t) = mOpacity - t * WAVE_OPACITY_DECAY_VELOCITY / 1000
            // outer(t) = mOuterOpacity + t * WAVE_OUTER_OPACITY_VELOCITY / 1000
            val inflectionDuration = max(
                0, (1000 * (1 - mOuterOpacity)
                        / (WAVE_OPACITY_DECAY_VELOCITY + outerOpacityVelocity) + 0.5f).toInt()
            )
            val inflectionOpacity = (mColorAlpha * ((mOuterOpacity
                    + inflectionDuration * outerOpacityVelocity * outerSizeInfluence / 1000)) + 0.5f).toInt()
            exitSoftware(opacityDuration, inflectionDuration, inflectionOpacity)
        }

        /**
         * Jump all animations to their end state. The caller is responsible for
         * removing the ripple from the list of animating ripples.
         */
        fun jump() {
            endSoftwareAnimations()
        }

        private fun endSoftwareAnimations() {
            if (mAnimOuterOpacity != null) {
                mAnimOuterOpacity!!.end()
                mAnimOuterOpacity = null
            }
        }

        private fun getTempPaint(): Paint? {
            if (mTempPaint == null) {
                mTempPaint = Paint()
            }
            return mTempPaint
        }

        private fun exitSoftware(
            opacityDuration: Int,
            inflectionDuration: Int,
            inflectionOpacity: Int
        ) {
            val outerOpacityAnim: ObjectAnimator
            if (inflectionDuration > 0) {
                // Outer opacity continues to increase for a bit.
                outerOpacityAnim = ObjectAnimator.ofFloat(
                    this,
                    "outerOpacity", inflectionOpacity / 255.0f
                )
                //outerOpacityAnim.setAutoCancel(true);
                outerOpacityAnim.duration = inflectionDuration.toLong()
                outerOpacityAnim.interpolator = LINEAR_INTERPOLATOR

                // Chain the outer opacity exit animation.
                val outerDuration = opacityDuration - inflectionDuration
                if (outerDuration > 0) {
                    outerOpacityAnim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            val outerFadeOutAnim = ObjectAnimator.ofFloat(
                                this@RippleBackground, "outerOpacity", 0f
                            )
                            //outerFadeOutAnim.setAutoCancel(true);
                            outerFadeOutAnim.duration = outerDuration.toLong()
                            outerFadeOutAnim.interpolator = LINEAR_INTERPOLATOR
                            outerFadeOutAnim.addListener(mAnimationListener)
                            mAnimOuterOpacity = outerFadeOutAnim
                            outerFadeOutAnim.start()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            animation.removeListener(this)
                        }
                    })
                } else {
                    outerOpacityAnim.addListener(mAnimationListener)
                }
            } else {
                outerOpacityAnim = ObjectAnimator.ofFloat(this, "outerOpacity", 0f)
                //outerOpacityAnim.setAutoCancel(true);
                outerOpacityAnim.duration = opacityDuration.toLong()
                outerOpacityAnim.addListener(mAnimationListener)
            }
            mAnimOuterOpacity = outerOpacityAnim
            outerOpacityAnim.start()
        }

        /**
         * Cancel all animations. The caller is responsible for removing
         * the ripple from the list of animating ripples.
         */
        fun cancel() {
            cancelSoftwareAnimations()
        }

        private fun cancelSoftwareAnimations() {
            if (mAnimOuterOpacity != null) {
                mAnimOuterOpacity!!.cancel()
                mAnimOuterOpacity = null
            }
        }

        private fun invalidateSelf() {
            mOwner.invalidateSelf()
        }

        private val mAnimationListener: AnimatorListenerAdapter =
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mHardwareAnimating = false
                }
            }
    }

    companion object {
        private val DST_IN = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        private val SRC_ATOP = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        private val SRC_OVER = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)

        /**
         * Constant for automatically determining the maximum ripple radius.
         *
         * @see .setMaxRadius
         */
        private const val RADIUS_AUTO = -1

        /** The maximum number of ripples supported.  */
        private const val MAX_RIPPLES = 10

        private val LINEAR_INTERPOLATOR = LinearInterpolator()
        private val DECEL_INTERPOLATOR = LogInterpolator()

        private const val GLOBAL_SPEED = 1.0f
        private const val WAVE_TOUCH_DOWN_ACCELERATION = 1024.0f * GLOBAL_SPEED
        private const val WAVE_TOUCH_UP_ACCELERATION = 3400.0f * GLOBAL_SPEED
        private const val WAVE_OPACITY_DECAY_VELOCITY = 3.0f / GLOBAL_SPEED
        private const val WAVE_OUTER_OPACITY_EXIT_VELOCITY_MAX = 4.5f * GLOBAL_SPEED
        private const val WAVE_OUTER_OPACITY_EXIT_VELOCITY_MIN = 1.5f * GLOBAL_SPEED
        private const val WAVE_OUTER_OPACITY_ENTER_VELOCITY = 10.0f * GLOBAL_SPEED
        private const val WAVE_OUTER_SIZE_INFLUENCE_MAX = 200f
        private const val WAVE_OUTER_SIZE_INFLUENCE_MIN = 40f

        private const val RIPPLE_ENTER_DELAY: Long = 80

        private fun lerp(start: Float, stop: Float, amount: Float): Float {
            return start + (stop - start) * amount
        }

        private fun constrain(amount: Float, low: Float, high: Float): Float {
            return if (amount < low) low else if (amount > high) high else amount
        }
    }
}