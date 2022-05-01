package com.s16.drawable

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue


class ActionBarBackgroundDrawable(private val context: Context, private val styles: Styles) : Drawable() {

    enum class Styles(color1: Int, color2: Int, color3: Int, isBottom: Boolean) {
        SOLID_DARK(-0xd2d2d3, -0xddddde, -0xe4e4e5, false), SOLID_LIGHT(
            -0x202021,
            -0x222223,
            -0x2d2d2e,
            false
        ),
        TRANSPARENT_DARK(0x0dfefefe, 0x0dfefefe, -0xcc4a1b, false), TRANSPARENT_LIGHT(
            0x00000000,
            0x00000000,
            -0xcc4a1b,
            false
        ),
        STACKED_SOLID_DARK(-0xe2e2e3, -0xeeeeef, -0xf1f1f2, false), STACKED_SOLID_LIGHT(
            -0x2d2d2e,
            -0x323233,
            -0x3c3c3d,
            false
        ),
        STACKED_SOLID_INVERSE(-0xa1a1a2, -0xaaaaab, -0xb3b3b4, false), STACKED_TRANSPARENT_DARK(
            0x1afefefe,
            0x1afefefe,
            0x48fefefe,
            false
        ),
        STACKED_TRANSPARENT_LIGHT(
            0x0d000000,
            0x0d000000,
            0x3d000000,
            false
        ),
        BOTTOM_SOLID_DARK(-0xe4e4e5, -0xddddde, -0xddddde, true), BOTTOM_SOLID_LIGHT(
            -0x4e4e4f,
            -0x222223,
            -0x222223,
            true
        ),
        BOTTOM_SOLID_INVERSE(-0xb7b7b8, -0xaaaaab, -0xaaaaab, true), BOTTOM_TRANSPARENT_DARK(
            0x33ffffff,
            0x00000000,
            0x00000000,
            true
        ),
        BOTTOM_TRANSPARENT_LIGHT(0x33000000, 0x00000000, 0x00000000, true);

        var colors: IntArray? = null
        var isBottom = false

        init {
            colors = intArrayOf(color1, color2, color3)
            this.isBottom = isBottom
        }
    }

    private val mPaints: Array<Paint> = arrayOf()

    // mdpi		24 -> 1, 21, 2 | 2, 22
    // hdpi		36 -> 1, 32, 3 | 2, 34
    // xhdpi  	48 -> 1, 44, 3 | 3, 45
    private val mSizes: IntArray = IntArray(2)

    init {
        for (i in 0..2) {
            mPaints[i] = Paint().apply {
                style = Paint.Style.FILL
            }
        }
        setup()
    }

    private fun setup() {
        // Sizes
        val dm = context.resources.displayMetrics
        val screenLayout =
            context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        if (styles.isBottom) {
            when (screenLayout) {
                1, 2 -> {
                    mSizes[0] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm).toInt()
                    mSizes[1] = 0
                }
                3, 4 -> {
                    mSizes[0] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm).toInt()
                    mSizes[1] = 0
                }
                else -> {
                    mSizes[0] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm).toInt()
                    mSizes[1] = 0
                }
            }
        } else {
            when (screenLayout) {
                1 -> {
                    mSizes[0] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm).toInt()
                    mSizes[1] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm).toInt()
                }
                2, 3, 4 -> {
                    mSizes[0] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm).toInt()
                    mSizes[1] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm).toInt()
                }
                else -> {
                    mSizes[0] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm).toInt()
                    mSizes[1] =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm).toInt()
                }
            }
        }

        // Colors
        mPaints[0].color = styles.colors!![0]
        mPaints[1].color = styles.colors!![1]
        mPaints[2].color = styles.colors!![2]
    }

    override fun draw(canvas: Canvas) {
        val bounds: Rect = bounds
        var y: Int = bounds.top

        if (mPaints[0].color !== 0x0) {
            val rect = Rect(bounds.left, y, bounds.right, y + mSizes[0])
            canvas.drawRect(rect, mPaints[0])
        }
        y += mSizes[0]
        if (mPaints[1].color !== 0x0) {
            val rect = Rect(bounds.left, y, bounds.right, bounds.bottom - mSizes[1])
            canvas.drawRect(rect, mPaints[1])
        }

        if (mSizes[1] !== 0 && mPaints[2].color !== 0x0) {
            y = bounds.bottom - mSizes[1]
            val rect = Rect(bounds.left, y, bounds.right, bounds.bottom)
            canvas.drawRect(rect, mPaints[2])
        }
    }

    override fun setAlpha(p0: Int) {
    }

    override fun setColorFilter(p0: ColorFilter?) {
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE

}