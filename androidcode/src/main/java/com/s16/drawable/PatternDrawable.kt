package com.s16.drawable

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import kotlin.math.ceil


class PatternDrawable : Drawable {

    @SuppressLint("UseSparseArrays")
    private val mBitmapCache = HashMap<Int, Bitmap>()

    private val mDrawPaint: Paint = Paint()
    private val mPaint: Paint = Paint()
    private var mBitmapWidth = 0
    private var mBitmapHeight = 0
    private var mSrcBitmap: Bitmap? = null
    private var mBitmap: Bitmap? = null
    private var numRectanglesHorizontal = 0
    private var numRectanglesVertical = 0

    constructor(bitmap: Bitmap) : super() {
        mSrcBitmap = bitmap
        mBitmapWidth = mSrcBitmap!!.width
        mBitmapHeight = mSrcBitmap!!.height
    }

    constructor(res: Resources, @DrawableRes id: Int) : super() {
        if (id > 0) {
            mSrcBitmap = BitmapFactory.decodeResource(res, id)
            mBitmapWidth = mSrcBitmap!!.width
            mBitmapHeight = mSrcBitmap!!.height
        }
    }

    override fun draw(canvas: Canvas) {
        mBitmap?.let {
            canvas.drawBitmap(it, null, bounds, mDrawPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        throw UnsupportedOperationException("Alpha is not supported by this drawwable.")
    }

    override fun setColorFilter(cf: ColorFilter?) {
        throw UnsupportedOperationException("ColorFilter is not supported by this drawwable.")
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun getIntrinsicWidth(): Int {
        return mBitmapWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mBitmapHeight
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val height: Int = bounds.height()
        val width: Int = bounds.width()
        numRectanglesHorizontal = ceil((width / mBitmapWidth).toDouble()).toInt()
        numRectanglesVertical = ceil((height / mBitmapHeight).toDouble()).toInt()
        generatePatternBitmap()
    }

    private fun generatePatternBitmap() {
        if (bounds.width() <= 0 || bounds.height() <= 0) return
        if (mSrcBitmap == null) return
        val bitmapCache = mBitmapCache[numRectanglesHorizontal * numRectanglesVertical]
        if (bitmapCache != null) {
            mBitmap = bitmapCache
            return
        }
        val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), mSrcBitmap!!.config)
        val canvas = Canvas(bitmap)
        for (i in 0..numRectanglesVertical) {
            for (j in 0..numRectanglesHorizontal) {
                val left = (j * mBitmapWidth).toFloat()
                val top = (i * mBitmapHeight).toFloat()
                canvas.drawBitmap(mSrcBitmap!!, left, top, mPaint)
            }
        }
        mBitmapCache[numRectanglesHorizontal * numRectanglesVertical] = bitmap
        mBitmap = bitmap
    }
}