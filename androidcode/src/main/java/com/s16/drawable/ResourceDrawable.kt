package com.s16.drawable

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


open class ResourceDrawable(protected val context: Context) : Drawable() {

    internal class OpenResourceIdResult {
        var r: Resources? = null
        var id = 0
    }

    private var mDrawable: Drawable? = null
    private var mResource = 0
    private var mUri: Uri? = null

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

    override fun draw(canvas: Canvas) {
        mDrawable?.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        mDrawable?.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mDrawable?.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return mDrawable?.opacity ?: PixelFormat.TRANSLUCENT
    }

    fun getDrawable() = mDrawable

    fun setImageResource(@DrawableRes resId: Int) {
        if (mUri != null || mResource != resId) {
            mResource = resId
            mUri = null
            resolveUri()
            invalidateSelf()
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
            invalidateSelf()
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
            invalidateSelf()
        }
    }

    val isEmpty: Boolean
        get() = mDrawable == null
}