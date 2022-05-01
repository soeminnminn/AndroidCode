package com.s16.drawable

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min


class ScaleTypeDrawable(private val context: Context): Drawable() {
    internal class OpenResourceIdResult {
        var r: Resources? = null
        var id = 0
    }

    private var mDrawable: Drawable? = null
    private var mResource = 0
    private var mUri: Uri? = null

    enum class ScaleType(val nativeInt: Int) {
        MATRIX(0), FIT_XY(1), FIT_START(2), FIT_CENTER(3), FIT_END(4), FIT_WIDTH(5), FIT_HEIGHT(6), TILE(
            7
        ),
        CENTER(8), CENTER_CROP(9), CENTER_INSIDE(10), SQUARE(11), SQUARE_CENTER(12), CIRCLE(13), CIRCLE_CENTER(
            14
        );
    }

    private val mDrawableWidth: Int
        get() = mDrawable?.intrinsicWidth ?: 0

    private val mDrawableHeight : Int
        get() = mDrawable?.intrinsicHeight ?: 0

    private var mMatrix = Matrix()
    private var mDrawMatrix: Matrix? = null

    private var mPath: Path? = null
    private var mCornerRadius = 0f
    private val mBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = TRANSPARENT
    }
    private var mBorderColor = 0
    private var mBorderWidth = 0f

    private var mBackgroundColor = 0x0
    private var mScaleType = ScaleType.CENTER

    private val sS2FArray: Array<Matrix.ScaleToFit> = arrayOf(
        Matrix.ScaleToFit.FILL,
        Matrix.ScaleToFit.START,
        Matrix.ScaleToFit.CENTER,
        Matrix.ScaleToFit.END
    )

    constructor(context: Context, uri: Uri?) : this(context) {
        mUri = uri
        resolveUri()
    }

    constructor(context: Context, @DrawableRes resId: Int) : this(context) {
        mResource = resId
        mUri = null
        resolveUri()
    }

    constructor(context: Context, drawable: Drawable?) : this(context) {
        mDrawable = drawable
        mResource = 0
        mUri = null
    }

    constructor(context: Context, bitmap: Bitmap?) : this(context) {
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
            mDrawable = ContextCompat.getDrawable(context, mResource)
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

    @SuppressLint("ObsoleteSdkInt")
    fun setHardwareAccelerated(view: View, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (enabled) view.setLayerType(View.LAYER_TYPE_HARDWARE, null) else view.setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
            )
        }
    }

    private fun scaleTypeToScaleToFit(st: ScaleType): Matrix.ScaleToFit {
        // ScaleToFit enum to their corresponding Matrix.ScaleToFit values
        return sS2FArray[st.nativeInt - 1]
    }

    override fun draw(canvas: Canvas) {
        if (mDrawable == null) {
            return  // couldn't resolve the URI
        }

        if (mDrawableWidth == 0 || mDrawableHeight == 0) {
            return  // nothing to draw (empty bounds)
        }

        val rectPaint = RectF()
        rectPaint.set(bounds)
        if (rectPaint.width() == 0f || rectPaint.height() == 0f) {
            return
        }

        var saveCount = canvas.save()
        if (mBackgroundColor != 0) {
            canvas.drawColor(mBackgroundColor)
        }

        if (mBorderWidth > 0.0f) {
            val rectBorder: RectF = getDrawRect(
                rectPaint.width(),
                rectPaint.height(),
                mDrawableWidth.toFloat(),
                mDrawableHeight.toFloat(),
                0.0f
            )
            val borderPath: Path = getDrawPath(
                rectBorder,
                rectBorder.width(),
                rectBorder.height(),
                mCornerRadius,
                0.0f
            )
            canvas.translate(rectBorder.left, rectBorder.top)
            canvas.drawPath(borderPath, mBorderPaint)
            canvas.restoreToCount(saveCount)
            saveCount = canvas.save()
            val bitmapPath: Path =
                getDrawPath(rectPaint, mDrawableWidth.toFloat(), mDrawableHeight.toFloat(), mCornerRadius, mBorderWidth)
            canvas.clipPath(bitmapPath)
        } else {
            val bitmapPath: Path =
                getDrawPath(rectPaint, mDrawableWidth.toFloat(), mDrawableHeight.toFloat(), mCornerRadius, 0.0f)
            canvas.clipPath(bitmapPath)
        }
        if (mDrawMatrix != null) {
            canvas.concat(mDrawMatrix)
        }
        if (ScaleType.TILE === mScaleType) {
            drawTileBitmap(
                canvas, rectPaint, mDrawableWidth.toFloat(),
                mDrawableHeight.toFloat()
            )
        } else {
            mDrawable!!.draw(canvas)
        }
        canvas.restoreToCount(saveCount)
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        bounds?.let {
            configureBounds(it)
        }
    }

    private fun drawTileBitmap(canvas: Canvas, bounds: RectF, dwidth: Float, dheight: Float) {
        if (mDrawable == null) {
            return
        }
        val vwidth = bounds.width()
        val vheight = bounds.height()
        var x = bounds.left
        var y = bounds.top
        while (x < bounds.left + vwidth) {
            while (y < bounds.top + vheight) {
                val saveCount = canvas.save()
                canvas.translate(x, y)
                mDrawable!!.draw(canvas)
                canvas.restoreToCount(saveCount)
                y += dheight
            }
            y = bounds.top
            x += dwidth
        }
    }

    private fun updateDrawable() {
        val bounds = bounds
        configureBounds(bounds)
        invalidateSelf()
    }

    private fun configureBounds(bounds: Rect) {
        if (mDrawable == null) {
            return
        }
        val dwidth = mDrawableWidth
        val dheight = mDrawableHeight
        val vwidth = bounds.width()
        val vheight = bounds.height()
        val fits = (dwidth < 0 || vwidth == dwidth) &&
                (dheight < 0 || vheight == dheight)
        if (dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY === mScaleType) {
            /* If the drawable has no intrinsic size, or we're told to
                scaletofit, then we just fill our entire view.
            */
            mDrawable!!.setBounds(0, 0, vwidth, vheight)
            mDrawMatrix = null
        } else {
            // We need to do the scaling ourself, so have the drawable
            // use its native size.
            mDrawable!!.setBounds(0, 0, dwidth, dheight)
            if (mScaleType === ScaleType.MATRIX) {
                // Use the specified matrix as-is.
                mDrawMatrix = if (mMatrix.isIdentity) {
                    null
                } else {
                    mMatrix
                }
            } else if (fits) {
                // The bitmap fits exactly, no transform needed.
                mDrawMatrix = null
            } else if (mScaleType === ScaleType.CENTER) {
                // Center bitmap in view, no scaling.
                mDrawMatrix = mMatrix
                mDrawMatrix!!.setTranslate(
                    ((vwidth - dwidth) * 0.5f + 0.5f),
                    ((vheight - dheight) * 0.5f + 0.5f)
                )
            } else if (ScaleType.CENTER_CROP === mScaleType) {
                mDrawMatrix = mMatrix
                val scale: Float
                var dx = 0f
                var dy = 0f
                if (dwidth * vheight > vwidth * dheight) {
                    scale = vheight.toFloat() / dheight.toFloat()
                    dx = (vwidth - dwidth * scale) * 0.5f
                } else {
                    scale = vwidth.toFloat() / dwidth.toFloat()
                    dy = (vheight - dheight * scale) * 0.5f
                }
                mDrawMatrix!!.setScale(scale, scale)
                mDrawMatrix!!.postTranslate(
                    (dx + 0.5f),
                    (dy + 0.5f)
                )
            } else if (mScaleType === ScaleType.CENTER_INSIDE) {
                mDrawMatrix = mMatrix
                val scale: Float = if (dwidth <= vwidth && dheight <= vheight) {
                    1.0f
                } else {
                    min(
                        vwidth.toFloat() / dwidth.toFloat(),
                        vheight.toFloat() / dheight.toFloat()
                    )
                }
                val dx = ((vwidth - dwidth * scale) * 0.5f + 0.5f)
                val dy = ((vheight - dheight * scale) * 0.5f + 0.5f)
                mDrawMatrix!!.setScale(scale, scale)
                mDrawMatrix!!.postTranslate(dx, dy)
            } else if (mScaleType === ScaleType.FIT_HEIGHT) { // Fit Height
                mDrawMatrix = mMatrix
                val scale = vheight.toFloat() / dheight.toFloat()
                val dx: Float = ((vwidth - dwidth * scale) * 0.5f + 0.5f)
                val dy = 0f
                mDrawMatrix!!.setScale(scale, scale)
                mDrawMatrix!!.postTranslate(dx, dy)
            } else if (mScaleType === ScaleType.FIT_WIDTH) { // Fit Width
                mDrawMatrix = mMatrix
                val scale = vwidth.toFloat() / dwidth.toFloat()
                val dx = 0f
                val dy: Float = ((vheight - dheight * scale) * 0.5f + 0.5f)
                mDrawMatrix!!.setScale(scale, scale)
                mDrawMatrix!!.postTranslate(dx, dy)
            } else if (mScaleType === ScaleType.TILE) {
                mDrawMatrix = null
            } else if (mScaleType === ScaleType.CIRCLE || mScaleType === ScaleType.SQUARE) {
                mDrawMatrix = mMatrix
                val size = min(vwidth, vheight).toFloat()
                val scale: Float = if (dwidth > dheight) {
                    size / dheight.toFloat()
                } else {
                    size / dwidth.toFloat()
                }
                val dx = ((vwidth - dwidth * scale) * 0.5f + 0.5f)
                val dy = ((vheight - dheight * scale) * 0.5f + 0.5f)
                mDrawMatrix!!.setScale(scale, scale)
                mDrawMatrix!!.postTranslate(dx, dy)
            } else if (mScaleType === ScaleType.CIRCLE_CENTER || mScaleType === ScaleType.SQUARE_CENTER) {
                val size = Math.min(vwidth, vheight).toFloat()
                val scale = if (dwidth <= vwidth && dheight <= vheight) {
                    1.0f
                } else {
                    if (dwidth > dheight) {
                        size / dheight.toFloat()
                    } else {
                        size / dwidth.toFloat()
                    }
                }
                val dx: Float = ((vwidth - dwidth * scale) * 0.5f + 0.5f)
                val dy: Float = ((vheight - dheight * scale) * 0.5f + 0.5f)
                mDrawMatrix = mMatrix
                mDrawMatrix!!.setScale(scale, scale)
                mDrawMatrix!!.postTranslate(dx, dy)
            } else {
                // Generate the required transform.
                mDrawMatrix = mMatrix
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

    private fun getDrawPath(
        bounds: RectF,
        dwidth: Float,
        dheight: Float,
        cornerRadius: Float,
        padding: Float
    ): Path {
        return if (mPath != null) {
            Path(mPath)
        } else {
            val path = Path()
            val vwidth = bounds.width()
            val vheight = bounds.height()
            if (mScaleType === ScaleType.CIRCLE) {
                val size = min(vwidth, vheight)
                path.addCircle(
                    (vwidth - 1) / 2,
                    (vheight - 1) / 2,
                    size / 2 - padding,
                    Path.Direction.CCW
                )
            } else if (mScaleType === ScaleType.CIRCLE_CENTER) {
                val size = min(min(vwidth, vheight), min(dwidth, dheight))
                path.addCircle(
                    (vwidth - 1) / 2,
                    (vheight - 1) / 2,
                    size / 2 - padding,
                    Path.Direction.CCW
                )
            } else {
                var scale = 1.0f
                scale = if (dwidth * vheight > vwidth * dheight) {
                    vwidth / dwidth
                } else {
                    vheight / dheight
                }
                var rect = RectF(bounds)
                if (mScaleType === ScaleType.FIT_START) {
                    rect = RectF(0f, 0f, dwidth * scale, dheight * scale)
                } else if (mScaleType === ScaleType.FIT_CENTER) {
                    val dx = (vwidth - dwidth * scale) * 0.5f
                    val dy = (vheight - dheight * scale) * 0.5f
                    rect = RectF(dx, dy, dx + dwidth * scale, dy + dheight * scale)
                } else if (mScaleType === ScaleType.FIT_END) {
                    val dx = vwidth - dwidth * scale
                    val dy = vheight - dheight * scale
                    rect = RectF(dx, dy, dx + dwidth * scale, dy + dheight * scale)
                } else if (mScaleType === ScaleType.FIT_WIDTH) {
                    scale = vwidth / dwidth
                    val dx = 0f
                    val dy = max(0f, (vheight - dheight * scale) * 0.5f + 0.5f)
                    rect = RectF(
                        dx, dy,
                        dx + min(vwidth, dwidth * scale),
                        dy + min(vheight, dheight * scale)
                    )
                } else if (mScaleType === ScaleType.FIT_HEIGHT) {
                    scale = vheight / dheight
                    val dx = max(0f, (vwidth - dwidth * scale) * 0.5f + 0.5f)
                    val dy = 0f
                    rect = RectF(
                        dx, dy,
                        dx + min(vwidth, dwidth * scale),
                        dy + min(vheight, dheight * scale)
                    )
                } else if (mScaleType === ScaleType.CENTER) {
                    var dx = 0f
                    var dy = 0f
                    if (dwidth <= vwidth && dheight <= vheight) {
                        dy = (vheight - dheight) * 0.5f
                        dx = (vwidth - dwidth) * 0.5f
                    } else {
                        if (dwidth * vheight > vwidth * dheight) {
                            dy = (vheight - dheight) * 0.5f
                        } else {
                            dx = (vwidth - dwidth) * 0.5f
                        }
                    }
                    rect = RectF(
                        dx,
                        dy,
                        dx + min(vwidth, dwidth),
                        dy + min(vheight, dheight)
                    )
                } else if (mScaleType === ScaleType.CENTER_INSIDE) {
                    if (dwidth <= vwidth && dheight <= vheight) {
                        scale = 1.0f
                    }
                    val dx = (vwidth - dwidth * scale) * 0.5f
                    val dy = (vheight - dheight * scale) * 0.5f
                    rect = RectF(dx, dy, dx + dwidth * scale, dy + dheight * scale)
                } else if (mScaleType === ScaleType.SQUARE) {
                    val size = min(vwidth, vheight)
                    val dx = (vwidth - size) * 0.5f
                    val dy = (vheight - size) * 0.5f
                    rect = RectF(dx, dy, dx + size, dy + size)
                } else if (mScaleType === ScaleType.SQUARE_CENTER) {
                    val size = min(min(vwidth, vheight), min(dwidth, dheight))
                    val dx = (vwidth - size) * 0.5f
                    val dy = (vheight - size) * 0.5f
                    rect = RectF(dx, dy, dx + size, dy + size)
                }
                if (padding > 0.0f) {
                    val rectWithPadding = RectF(
                        rect.left + padding,
                        rect.top + padding,
                        rect.right - padding,
                        rect.bottom - padding
                    )
                    if (cornerRadius > 0) {
                        path.addRoundRect(
                            rectWithPadding,
                            cornerRadius,
                            cornerRadius,
                            Path.Direction.CCW
                        )
                    } else {
                        path.addRect(rectWithPadding, Path.Direction.CCW)
                    }
                } else {
                    if (cornerRadius > 0) {
                        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CCW)
                    } else {
                        path.addRect(rect, Path.Direction.CCW)
                    }
                }
            }
            path
        }
    }

    private fun getDrawRect(
        vwidth: Float,
        vheight: Float,
        dwidth: Float,
        dheight: Float,
        padding: Float
    ): RectF {
        var x = 0.0f
        var y = 0.0f
        var width = 0.0f
        var height = 0.0f
        var scale = if (dwidth * vheight > vwidth * dheight) {
            vwidth / dwidth
        } else {
            vheight / dheight
        }

        if (mScaleType === ScaleType.FIT_START) {
            if (vwidth < vheight) {
                width = vwidth
                height = dheight * scale
            } else {
                width = dwidth * scale
                height = vheight
            }
        } else if (mScaleType === ScaleType.FIT_END) {
            if (vwidth < vheight) {
                width = vwidth
                height = dheight * scale
                y += vheight - height
            } else {
                width = dwidth * scale
                height = vheight
                x += vwidth - width
            }
        } else if (mScaleType === ScaleType.FIT_CENTER) {
            if (vwidth < vheight) {
                width = vwidth
                height = dheight * scale
                y += (vheight - height) * 0.5f
            } else {
                width = dwidth * scale
                height = vheight
                x += (vwidth - width) * 0.5f
            }
        } else if (mScaleType === ScaleType.FIT_WIDTH) {
            scale = vwidth / dwidth
            width = vwidth
            height = min(vheight, dheight * scale)
            y += (vheight - height) * 0.5f
        } else if (mScaleType === ScaleType.FIT_HEIGHT) {
            scale = vheight / dheight
            width = min(vwidth, dwidth * scale)
            height = vheight
            x += (vwidth - width) * 0.5f
        } else if (mScaleType === ScaleType.CENTER) {
            width = min(vwidth, dwidth)
            height = min(vheight, dheight)
            y += (vheight - height) * 0.5f
            x += (vwidth - width) * 0.5f
        } else if (mScaleType === ScaleType.CENTER_INSIDE) {
            if (dwidth <= vwidth && dheight <= vheight) {
                width = dwidth
                height = dheight
                y += (vheight - height) * 0.5f
                x += (vwidth - width) * 0.5f
            } else if (vwidth < vheight) {
                width = vwidth
                height = dheight * scale
                y += (vheight - height) * 0.5f
                x = 0.0f
            } else {
                width = dwidth * scale
                height = vheight
                x += (vwidth - width) * 0.5f
                y = 0.0f
            }
        } else if (mScaleType === ScaleType.SQUARE || mScaleType === ScaleType.CIRCLE) {
            val size = min(dwidth, dheight)
            scale = if (dwidth * vheight > vwidth * dheight) {
                vwidth / size
            } else {
                vheight / size
            }
            width = Math.min(vwidth, dwidth * scale)
            height = Math.min(vheight, dheight * scale)
            y += (vheight - height) * 0.5f
            x += (vwidth - width) * 0.5f
        } else if (mScaleType === ScaleType.SQUARE_CENTER || mScaleType === ScaleType.CIRCLE_CENTER) {
            height = min(min(vwidth, vheight), min(dwidth, dheight))
            width = height
            y += (vheight - height) * 0.5f
            x += (vwidth - width) * 0.5f
        } else {
            width = vwidth
            height = vheight
            x = 0.0f
            y = 0.0f
        }
        return if (padding > 0.0f) {
            RectF(x + padding, y + padding, x + width - padding, y + height - padding)
        } else RectF(x, y, x + width, y + height)
    }

    override fun setAlpha(alpha: Int) {
        mDrawable?.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mDrawable?.colorFilter = cf
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return mDrawable?.opacity ?: PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return mDrawableWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mDrawableHeight
    }

    fun getDrawable() = mDrawable

    fun setImageResource(@DrawableRes resId: Int) {
        if (mUri != null || mResource != resId) {
            mResource = resId
            mUri = null
            resolveUri()
            updateDrawable()
        }
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) {
            setImageDrawable(BitmapDrawable(context.resources, bitmap))
        } else {
            setImageDrawable(null)
        }
    }

    fun setImageDrawable(drawable: Drawable?) {
        if (mDrawable !== drawable) {
            mResource = 0
            mUri = null
            mDrawable = drawable
            updateDrawable()
        }
    }

    fun setImageURI(uri: Uri?) {
        if (mResource != 0 ||
            mUri != uri &&
            (uri == null || mUri == null || uri != mUri)
        ) {
            mResource = 0
            mUri = uri
            resolveUri()
            updateDrawable()
            invalidateSelf()
        }
    }

    val isEmpty: Boolean
        get() = mDrawable == null

    fun setBorderColor(borderColor: Int) {
        if (borderColor != mBorderColor) {
            mBorderColor = borderColor
            mBorderPaint.color = mBorderColor
            invalidateSelf()
        }
    }

    fun setBorderWidth(borderWidth: Float) {
        if (borderWidth != mBorderWidth) {
            mBorderWidth = borderWidth
            mBorderPaint.strokeWidth = mBorderWidth
            invalidateSelf()
        }
    }

    fun setBackgroundColor(color: Int) {
        if (mBackgroundColor != color) {
            mBackgroundColor = color
            invalidateSelf()
        }
    }

    fun getBackgroundColor(): Int {
        return mBackgroundColor
    }

    fun setScaleType(value: ScaleType) {
        if (mScaleType !== value) {
            mScaleType = value
            updateDrawable()
        }
    }

    fun setDrawPath(path: Path) {
        mPath = path
        updateDrawable()
    }

    fun setCornerRadius(radius: Float) {
        if (mCornerRadius != radius) {
            mCornerRadius = radius
            updateDrawable()
        }
    }

    fun getImageMatrix(): Matrix? {
        return if (mDrawMatrix == null) {
            Matrix()
        } else mDrawMatrix
    }

    fun setImageMatrix(matrix: Matrix?) {
        // collaps null and identity to just null
        var lMatrix = matrix
        if (lMatrix != null && lMatrix.isIdentity) {
            lMatrix = null
        }

        // don't invalidate unless we're actually changing our matrix
        if (lMatrix == null && !mMatrix.isIdentity ||
            lMatrix != null && mMatrix != lMatrix
        ) {
            mMatrix.set(lMatrix)
            updateDrawable()
        }
    }

    fun destroy() {
        if (mDrawable != null) {
            mDrawable = null
        }
        System.gc()
    }

    companion object {
        private const val TRANSPARENT = 0x00000000
    }
}