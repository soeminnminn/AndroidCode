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
import kotlin.math.min


class CircleDrawable(private val context: Context) : Drawable() {

    private var mDrawable : Drawable? = null
    private var mResource: Int? = null
    private var mUri: Uri? = null

    private var mImageMatrix = Matrix()
    private var mDrawablePath = Path()

    internal class OpenResourceIdResult {
        var r: Resources? = null
        var id = 0
    }

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

    override fun draw(canvas: Canvas) {
        mDrawable?.let { d ->
            val saveCount = canvas.save()
            canvas.setMatrix(mImageMatrix)
            canvas.clipPath(mDrawablePath)
            d.draw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        bounds?.let {
            val radius = min(it.height() / 2.0f, it.width() / 2.0f)
            mDrawablePath = Path().apply {
                addCircle(it.centerX().toFloat(), it.centerY().toFloat(), radius, Path.Direction.CW)
            }
            mImageMatrix = calculateMatrix(mDrawable, it.width().toFloat(), it.height().toFloat()) ?: Matrix()
            invalidateSelf()
        }
    }

    private fun calculateMatrix(drawable: Drawable?, viewWidth: Float, viewHeight: Float): Matrix? {
        return drawable?.let { d ->
            val drawableWidth = d.intrinsicWidth.toFloat()
            val drawableHeight = d.intrinsicHeight.toFloat()

            val drawableRect = RectF(0f, 0f, drawableWidth, drawableHeight)
            var distRect: RectF

            // Height fix
            val viewHalfWidth = viewWidth / 2f
            var scale = viewHeight / drawableHeight
            val scaledWidth = drawableWidth * scale
            val scaledHalfWidth = scaledWidth / 2f
            distRect = RectF(
                viewHalfWidth - scaledHalfWidth,
                0f,
                viewHalfWidth + scaledHalfWidth,
                viewHeight
            )

            if (distRect.width() < viewWidth) {
                // Width fix
                val viewHalfHeight = viewHeight / 2f
                scale = viewWidth / drawableWidth
                val scaledHeight = drawableHeight * scale
                val scaledHalfHeight = scaledHeight / 2
                distRect = RectF(
                    0f,
                    viewHalfHeight - scaledHalfHeight,
                    viewWidth,
                    viewHalfHeight + scaledHalfHeight
                )
            }

            Matrix().apply {
                setRectToRect(
                    drawableRect,
                    distRect,
                    Matrix.ScaleToFit.CENTER
                )
            }
        }
    }

    override fun getIntrinsicHeight(): Int = mDrawable?.intrinsicHeight ?: 0

    override fun getIntrinsicWidth(): Int = mDrawable?.intrinsicWidth ?: 0

    override fun setAlpha(alpha: Int) {
        mDrawable?.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mDrawable?.colorFilter = cf
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

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
}