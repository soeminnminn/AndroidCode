package com.s16.drawable

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


class FilledDrawable(private val context: Context): Drawable() {

    enum class ScaleType(val nativeInt: Int) {
        NONE(0),

        /**
         * Scale the image using [Matrix.ScaleToFit.FILL].
         * From XML, use this syntax: `android:scaleType="fitXY"`.
         */
        FIT_XY(1),

        /**
         * Scale the image using [Matrix.ScaleToFit.START].
         * From XML, use this syntax: `android:scaleType="fitStart"`.
         */
        FIT_START(2),

        /**
         * Scale the image using [Matrix.ScaleToFit.CENTER].
         * From XML, use this syntax:
         * `android:scaleType="fitCenter"`.
         */
        FIT_CENTER(3),

        /**
         * Scale the image using [Matrix.ScaleToFit.END].
         * From XML, use this syntax: `android:scaleType="fitEnd"`.
         */
        FIT_END(4), FIT_WIDTH(5), FIT_HEIGHT(6);
    }

    private class OpenResourceIdResult {
        var r: Resources? = null
        var id = 0
    }

    private var mResource: Int? = null
    private var mUri: Uri? = null

    private var mDrawable: Drawable? = null
    private var mDrawMatrix: Matrix? = null
    private var mDrawableWidth = 0
    private var mDrawableHeight = 0
    private var mScaleType = ScaleType.NONE

    constructor(context: Context, @DrawableRes resId: Int) : this(context) {
        mResource = resId
        mUri = null
        resolveUri()
    }

    constructor(context: Context, uri: Uri) : this(context) {
        mUri = uri
        resolveUri()
    }

    constructor(context: Context, drawable: Drawable) : this(context) {
        mDrawable = drawable
        mResource = 0
        mUri = null
    }

    constructor(context: Context, bitmap: Bitmap) : this(context) {
        mDrawable = BitmapDrawable(context.resources, bitmap)
        mResource = 0
        mUri = null
    }

    private fun resolveUri() {
        if (mUri != null) {
            val scheme = mUri!!.scheme
            if (ContentResolver.SCHEME_ANDROID_RESOURCE == scheme) {
                // android.resource://[package]/[res id]
                // android.resource://[package]/[res type]/[res name]
                try {
                    // Load drawable through Resources, to get the source density information
                    val r = getResourceId(context, mUri!!)
                    mDrawable = ContextCompat.getDrawable(context, r.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (ContentResolver.SCHEME_CONTENT == scheme || ContentResolver.SCHEME_FILE == scheme) {
                var stream: InputStream? = null
                try {
                    stream = context.contentResolver.openInputStream(mUri!!)
                    mDrawable = createFromStream(stream, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (stream != null) {
                        try {
                            stream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                mDrawable = createFromPath(mUri.toString())
            }
            if (mDrawable == null) {
                println("resolveUri failed on bad bitmap uri: $mUri")
                // Don't try again.
                mUri = null
            }
        } else if (mResource != 0) {
            mDrawable = ContextCompat.getDrawable(context, mResource!!)
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getResourceId(context: Context, uri: Uri): OpenResourceIdResult {
        val authority = uri.authority
        val r: Resources = if (TextUtils.isEmpty(authority)) {
            throw FileNotFoundException("No authority: $uri")
        } else {
            try {
                context.packageManager.getResourcesForApplication(authority!!)
            } catch (ex: PackageManager.NameNotFoundException) {
                throw FileNotFoundException("No package found for authority: $uri")
            }
        }

        val path = uri.pathSegments ?: throw FileNotFoundException("No path: $uri")
        val id: Int = when (path.size) {
            1 -> {
                try {
                    path[0].toInt()
                } catch (e: NumberFormatException) {
                    throw FileNotFoundException("Single path segment is not a resource ID: $uri")
                }
            }
            2 -> {
                r.getIdentifier(path[1], path[0], authority)
            }
            else -> {
                throw FileNotFoundException("More than two path segments: $uri")
            }
        }

        if (id == 0) {
            throw FileNotFoundException("No resource found for: $uri")
        }
        val res = OpenResourceIdResult()
        res.r = r
        res.id = id
        return res
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            setImageDrawable(BitmapDrawable(context.resources, bitmap))
        } else {
            setImageDrawable(null)
        }
    }

    fun setImageDrawable(drawable: Drawable?) {
        if (mDrawable != drawable) {
            mResource = 0
            mUri = null
            mDrawable = drawable
            invalidateSelf()
        }
    }

    fun setImageResource(@DrawableRes resId: Int) {
        if (mResource != resId) {
            mResource = resId
            mUri = null
            resolveUri()
            invalidateSelf()
        }
    }

    fun setImageURI(uri: Uri?) {
        if (mResource != 0 ||
            (mUri != uri &&
                    (uri == null || mUri == null || uri != mUri))) {
            mResource = 0
            mUri = uri
            resolveUri()
            invalidateSelf()
        }
    }

    fun getDrawable() = mDrawable

    fun setScaleType(value: ScaleType) {
        if (mScaleType != value) {
            mScaleType = value
            invalidateSelf()
        }
    }

    private fun configureBounds(bounds: Rect) {
        mDrawable?.let { d ->
            val dwidth = mDrawableWidth
            val dheight = mDrawableHeight
            val vwidth: Int = bounds.width()
            val vheight: Int = bounds.height()

            if (mScaleType === ScaleType.FIT_XY) {
                d.setBounds(0, 0, vwidth, vheight)
                mDrawMatrix = null
            } else {
                // We need to do the scaling ourself, so have the drawable
                // use its native size.
                d.setBounds(0, 0, dwidth, dheight)
                mDrawMatrix = Matrix()

                if (mScaleType === ScaleType.NONE) {
                    mDrawMatrix!!.setTranslate(
                        ((vwidth - dwidth) * 0.5f + 0.5f),
                        ((vheight - dheight) * 0.5f + 0.5f)
                    )

                } else if (mScaleType === ScaleType.FIT_HEIGHT) {
                    val scale = vheight.toFloat() / dheight.toFloat()
                    val dx: Float = ((vwidth - dwidth * scale) * 0.5f + 0.5f)
                    val dy = 0f
                    mDrawMatrix!!.setScale(scale, scale)
                    mDrawMatrix!!.postTranslate(dx, dy)

                } else if (mScaleType === ScaleType.FIT_WIDTH) {
                    val scale = vwidth.toFloat() / dwidth.toFloat()
                    val dx = 0f
                    val dy: Float = ((vheight - dheight * scale) * 0.5f + 0.5f)
                    mDrawMatrix!!.setScale(scale, scale)
                    mDrawMatrix!!.postTranslate(dx, dy)

                } else {
                    mDrawMatrix!!.setRectToRect(
                        RectF(0f, 0f, dwidth.toFloat(), dheight.toFloat()), RectF(
                            0f, 0f,
                            vwidth.toFloat(), vheight.toFloat()
                        ),
                        scaleTypeToScaleToFit(mScaleType)
                    )
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        mDrawable?.let { d ->
            val saveCount: Int = canvas.save()
            if (mDrawMatrix != null) {
                canvas.concat(mDrawMatrix)
            }
            d.draw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        configureBounds(bounds!!)
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        if (mDrawable != null) {
            mDrawable!!.alpha = alpha
            invalidateSelf()
        }
    }

    override fun setColorFilter(cf: ColorFilter?) {
        if (mDrawable != null) {
            mDrawable!!.colorFilter = cf
            invalidateSelf()
        }
    }

    override fun getOpacity(): Int {
        return if (mDrawable != null) {
            mDrawable!!.opacity
        } else PixelFormat.TRANSLUCENT
    }

    companion object {
        private val sS2FArray: Array<Matrix.ScaleToFit> = arrayOf(
            Matrix.ScaleToFit.FILL,
            Matrix.ScaleToFit.START,
            Matrix.ScaleToFit.CENTER,
            Matrix.ScaleToFit.END
        )

        private fun scaleTypeToScaleToFit(st: ScaleType): Matrix.ScaleToFit {
            // ScaleToFit enum to their corresponding Matrix.ScaleToFit values
            return sS2FArray[st.nativeInt - 1]
        }
    }
}