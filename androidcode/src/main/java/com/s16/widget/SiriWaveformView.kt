package com.s16.widget

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin


class SiriWaveformView : View {

    private var mNumberOfWaves = kDefaultNumberOfWaves
    private var mWaveColor = 0xFFFFFF
    private var mPrimaryWaveLineWidth = kDefaultPrimaryLineWidth
    private var mSecondaryWaveLineWidth = kDefaultSecondaryLineWidth
    private var mIdleAmplitude = kDefaultIdleAmplitude
    private var mFrequency = kDefaultFrequency
    private var mAmplitude = kDefaultAmplitude
    private var mDensity = kDefaultDensity
    private var mPhaseShift = kDefaultPhaseShift
    private var mPhase = 0f

    private var mPaint: Paint? = null

    private var mHandler: Handler? = null
    private var mRunnable: Runnable? = null

    constructor(context: Context)
            : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {
    }

    init {
        mPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    /*
     * The total number of waves
     * Default: 5
     */
    fun setNumberOfWaves(numberOfWaves: Int) {
        mNumberOfWaves = numberOfWaves
        invalidate()
    }

    /*
     * Color to use when drawing the waves
     * Default: white
     */
    fun setWaveColor(waveColor: Int) {
        mWaveColor = waveColor
        invalidate()
    }

    /*
     * Line width used for the proeminent wave
     * Default: 3.0f
     */
    fun setPrimaryWaveLineWidth(primaryWaveLineWidth: Float) {
        mPrimaryWaveLineWidth = primaryWaveLineWidth
        invalidate()
    }

    /*
     * Line width used for all secondary waves
     * Default: 1.0f
     */
    fun setSecondaryWaveLineWidth(secondaryWaveLineWidth: Float) {
        mSecondaryWaveLineWidth = secondaryWaveLineWidth
        invalidate()
    }

    /*
     * The amplitude that is used when the incoming amplitude is near zero.
     * Setting a value greater 0 provides a more vivid visualization.
     * Default: 0.01
     */
    fun setIdleAmplitude(idleAmplitude: Float) {
        mIdleAmplitude = idleAmplitude
        invalidate()
    }

    /*
     * The frequency of the sinus wave. The higher the value, the more sinus wave peaks you will have.
     * Default: 1.5
     */
    fun setFrequency(frequency: Float) {
        mFrequency = frequency
        invalidate()
    }

    /*
     * The current amplitude
     */
    fun setAmplitude(amplitude: Float) {
        mAmplitude = amplitude
        invalidate()
    }

    /*
     * The lines are joined stepwise, the more dense you draw, the more CPU power is used.
     * Default: 5
     */
    fun setDensity(density: Float) {
        this.mDensity = density
        invalidate()
    }

    /*
     * The phase shift that will be applied with each level setting
     * Change this to modify the animation speed or direction
     * Default: -0.15
     */
    fun setPhaseShift(phaseShift: Float) {
        mPhaseShift = phaseShift
        invalidate()
    }

    fun startAnimate() {
        this.startAnimate(100)
    }

    fun startAnimate(miliseconds: Int) {
        mHandler = Handler()
        val random = Random()
        mRunnable = object : Runnable {
            override fun run() {
                val level: Float = random.nextFloat() * 0.5f
                //Log.i(TAG, "level: " + level);
                updateWithLevel(level)
                handler.postDelayed(this, miliseconds.toLong())
            }
        }
        mHandler!!.postDelayed(mRunnable!!, miliseconds.toLong())
    }

    fun stopAnimate() {
        if (mRunnable != null) {
            mHandler?.removeCallbacks(mRunnable!!)
        }
    }

    private fun dpToPixel(context: Context, dp: Float): Float {
        val metrics = context.resources.displayMetrics
        val screenDensity = metrics.density
        return dp * screenDensity
    }

    private fun colorWithAlphaComponent(alpha: Float, color: Int): Int {
        val a = (alpha * 100 * (255 / 100)).toInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    /*
     * Tells the waveform to redraw itself using the given level (normalized value)
     */
    fun updateWithLevel(level: Float) {
        mPhase += mPhaseShift
        mAmplitude = Math.max(level, mIdleAmplitude)
        this.invalidate()
    }

    // Thanks to Raffael Hannemann https://github.com/raffael/SISinusWaveView
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val bounds: Rect = canvas.clipBounds
        val paint = mPaint

        // We draw multiple sinus waves, with equal phases but altered amplitudes, multiplied by a parable function.
        for (i in 0 until mNumberOfWaves) {
            val strokeLineWidth = if (i == 0) mPrimaryWaveLineWidth else mSecondaryWaveLineWidth

            paint!!.strokeWidth = dpToPixel(context, strokeLineWidth)
            val halfHeight: Float = bounds.height() / 2.0f
            val width: Float = bounds.width().toFloat()
            val mid = width / 2.0f
            val maxAmplitude = halfHeight - strokeLineWidth * 2
            val progress = 1.0f - i.toFloat() / mNumberOfWaves
            val normedAmplitude = (1.5f * progress - 2.0f / mNumberOfWaves) * mAmplitude
            val multiplier = min(1.0f, progress / 3.0f * 2.0f + 1.0f / 3.0f)
            val color = colorWithAlphaComponent(multiplier, mWaveColor)
            paint.color = color

            val path = Path()
            var x = 0.0f
            while (x < width + mDensity) {

                // We use a parable to scale the sinus wave, that has its peak in the middle of the view.
                val scaling = (-(1f / mid * (x - mid)).toDouble().pow(2.0)).toFloat() + 1f
                val y =
                    (scaling * maxAmplitude * normedAmplitude * sin(2 * Math.PI * (x / width) * mFrequency + mPhase) + halfHeight).toFloat()
                if (x == 0f) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                x += mDensity
            }
            canvas.drawPath(path, paint)
            path.close()
        }
    }

    companion object {
        private val kDefaultAmplitude = 1.0f
        private val kDefaultFrequency = 1.5f
        private val kDefaultIdleAmplitude = 0.01f
        private val kDefaultNumberOfWaves = 5
        private val kDefaultPhaseShift = -0.15f
        private val kDefaultDensity = 5.0f
        private val kDefaultPrimaryLineWidth = 3.0f
        private val kDefaultSecondaryLineWidth = 1.0f
    }
}