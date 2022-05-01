package com.s16.drawable

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round


/**
 * A square Drawable that renders a single glyph from a Typeface resource. The drawable has a
 * content area defined by its bounds and its padding. The glyph is scaled so that its largest
 * dimension fills this area. The smaller dimension is then centered.
 *
 * @param typeface Configurable: typeface to select the glyph from.
 *
 */
class IconFontDrawable(private var typeface: Typeface) : Drawable() {

    /**
     * Configurable: alpha channel for the foreground color (default unset). If not set, the alpha
     * value from the [.color] or [.colorStateList] is used. Once set, this overrides
     * the alpha information in the assigned color, including with state changes. The unset value
     * is `-1`.
     */
    private var mAlpha = -1

    /**
     * Configurable: foreground color, simple case (default black). Any changes to [.alpha]
     * are reflected in this variable.
     */
    private var mColor: Int = Color.BLACK

    /**
     * Configurable: foreground color, for state-aware rendering (optional, no default). Any changes
     * to [.alpha] are reflected in this variable. Prevails over [.color] if set.
     */
    private var mColorStateList: ColorStateList? = null

    /**
     * Configurable: glyph to display in the drawable (required).
     */
    private var mGlyph = charArrayOf('\u0000')

    /**
     * Configurable: intrinsic size of the icon (optional, default -1 for no intrinsic size).
     */
    private var mIntrinsicSize = -1

    /**
     * Configurable: padding around the icon glyph within the bounds (optional, default zero).
     */
    private var mPadding = 0

    /**
     * Configurable: the rotation in degrees of the canvas when drawing the glyph. Zero is straight
     * up, positive values rotate the glyph clockwise (optional, default zero).
     */
    private var mRotation = 0f

    /**
     * Internal: a rectangle used as a temporary value during layout.
     */
    private var mDrawableArea: Rect = Rect()

    /**
     * Internal: the Paint used to draw [.glyphPath] onto our canvas.
     */
    private var mGlyphPaint: Paint = Paint().apply {
        isAntiAlias = true
        setTypeface(typeface)
    }

    /**
     * Internal: the font glyph path to draw onto our canvas.
     */
    private var mGlyphPath: Path = Path()

    /**
     * Internal: a float rectangle used as a temporary value during layout.
     */
    private var mGlyphPathBounds: RectF = RectF()

    /**
     * Internal: the transformation matrix to use for scaling and centering the glyph.
     */
    private var mGlyphPathTransform: Matrix = Matrix()

    /**
     * Internal: glyph rendering color, calculated from state, alpha and color values.
     */
    private var mRenderingColor: Int = Color.BLACK

    /**
     * Fully initializing constructor to support the Builder pattern.
     */
    constructor(alpha: Int, color: Int, colorStateList: ColorStateList, glyph: Char,
        intrinsicSize: Int, padding: Int, rotation: Float, typeface: Typeface) : this(typeface) {

        mAlpha = alpha
        mColor = color
        mColorStateList = colorStateList
        mGlyph[0] = glyph
        mIntrinsicSize = intrinsicSize
        mPadding = padding
        mRotation = rotation
        computeRenderingColor()
    }

    /**
     * Construct an icon font drawable without an intrinsic size in a solid color.
     *
     * @param typeface (nullable) typeface to select the glyph from.
     * @param glyph    the glyph to use.
     * @param color    the color in which to render the glyph.
     */
    constructor(typeface: Typeface, glyph: Char, color: Int) : this(typeface) {
        mGlyph[0] = glyph
        mColor = color
        computeRenderingColor()
    }

    /**
     * Construct an icon font drawable with an intrinsic size in a solid color.
     *
     * @param typeface      (nullable) typeface to select the glyph from.
     * @param glyph         the glyph to use.
     * @param color         the color in which to render the glyph.
     * @param intrinsicSize the intrinsic size in pixels.
     */
    constructor(typeface: Typeface, glyph: Char, color: Int, intrinsicSize: Int) : this(typeface) {
        mGlyph[0] = glyph
        mColor = color
        mIntrinsicSize = intrinsicSize
        computeRenderingColor()
    }

    /**
     * Sets the alpha value, triggering a repaint if the value changed.
     *
     * @param alpha an alpha value.
     */
    override fun setAlpha(alpha: Int) {
        val newAlpha = alpha and 0xFF
        if (mAlpha !== newAlpha) {
            mAlpha = newAlpha
            computeRenderingColor()
        }
    }

    /**
     * Unsets the alpha value, thus reverting the transparency to the level encoded in the glyph
     * color value. This method triggers a repaint if needed.
     */
    fun unsetAlpha() {
        mAlpha = -1
    }

    /**
     * Sets the icon color to a single color, triggering a repaint if the value changed. Note that
     * if [.colorStateList] is set to a non-null value, it prevails.
     *
     * @param color a color value. The alpha bits are ignored.
     * @see .setAlpha
     * @see .setColor
     */
    fun setColor(color: Int) {
        val newColor = color and 0x00FFFFFF
        if (mColor != newColor) {
            mColor = newColor
            computeRenderingColor()
        }
    }

    /**
     * Sets the icon color to a color state list, triggering a repaint if the value changed.
     *
     * @param stateColors a color state list. The alpha value is ignored.
     * @see .setAlpha
     * @see .setColor
     */
    fun setColor(stateColors: ColorStateList?) {
        mColorStateList = stateColors
        computeRenderingColor()
    }

    /**
     * Sets the displayed glyph, triggering a layout and repaint if the value changed.
     *
     * @param glyph the glyph.
     */
    fun setGlyph(glyph: Char) {
        if (glyph != mGlyph[0]) {
            mGlyph[0] = glyph
            computeGlyphPath()
        }
    }

    /**
     * Sets the intrinsic size of the drawable, triggering a layout and repaint if the value
     * changed. The drawable is constrained to square.
     *
     * @param intrinsicSize the intrinsic size in pixels.
     */
    fun setIntrinsicSize(intrinsicSize: Int) {
        if (mIntrinsicSize != intrinsicSize) {
            mIntrinsicSize = intrinsicSize
            computeGlyphPath()
        }
    }

    /**
     * Sets the padding of the drawable area, triggering a layout and repaint if the value changed.
     *
     * @param padding the padding value in pixels.
     */
    fun setPadding(padding: Int) {
        if (mPadding != padding) {
            mPadding = padding
            computeGlyphPath()
        }
    }

    /**
     * Sets the rotation of the drawable, triggering a repaint if the value changed.
     *
     * @param rotation the rotation in degrees. Zero is straight up, positive values rotate the
     * glyph clockwise.
     */
    fun setRotation(rotation: Float) {
        if (mRotation != rotation) {
            mRotation = rotation
            invalidateSelf()
        }
    }

    /**
     * Sets the typeface asset from which the glyph is taken, triggering a layout and repaint if
     * the value changed.
     *
     * @param typeface the typeface asset.
     */
    fun setTypeface(typeface: Typeface) {
        if (this.typeface !== typeface) {
            this.typeface = typeface
            mGlyphPaint.typeface = typeface
            computeGlyphPath()
        }
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mGlyphPaint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun isStateful(): Boolean {
        return mColorStateList != null && mColorStateList!!.isStateful
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        computeGlyphPath()
    }

    override fun getIntrinsicWidth(): Int {
        return mIntrinsicSize
    }

    override fun getIntrinsicHeight(): Int {
        return mIntrinsicSize
    }

    override fun onStateChange(state: IntArray?): Boolean {
        if (mColorStateList != null) {
            computeRenderingColor()
            return true
        }
        return false
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(mRotation, mDrawableArea.exactCenterX(), mDrawableArea.exactCenterY())
        canvas.drawPath(mGlyphPath, mGlyphPaint)
        canvas.restore()
    }

    private fun computeGlyphPath() {
        mDrawableArea.set(bounds)
        mDrawableArea.inset(mPadding, mPadding)

        mGlyphPaint.getTextPath(mGlyph, 0, 1, 0f, 0f, mGlyphPath)
        // Add an extra path point to fix the icon remaining blank on a Galaxy Note 2 running 4.1.2.
        mGlyphPath.computeBounds(mGlyphPathBounds, false)

        val centerX = mGlyphPathBounds.centerX()
        val centerY = mGlyphPathBounds.centerY()
        mGlyphPath.moveTo(centerX, centerY)
        mGlyphPath.lineTo(centerX + 0.001f, centerY + 0.001f)

        val areaWidthF = mDrawableArea.width().toFloat()
        val areaHeightF = mDrawableArea.height().toFloat()
        val scaleX = areaWidthF / mGlyphPathBounds.width()
        val scaleY = areaHeightF / mGlyphPathBounds.height()
        val scaleFactor = min(scaleX, scaleY)
        mGlyphPathTransform.setScale(scaleFactor, scaleFactor)
        mGlyphPath.transform(mGlyphPathTransform)

        // TODO this two pass calculation irks me.
        // It has to be possible to push this into a single Matrix transform; what makes it hard is
        // that the origin of Text is not top-left, but baseline-left so need to account for that.
        mGlyphPath.computeBounds(mGlyphPathBounds, false)

        val areaLeftF = mDrawableArea.left.toFloat()
        val areaTopF = mDrawableArea.top.toFloat()
        var transX = areaLeftF - mGlyphPathBounds.left
        transX += 0.5f * abs(areaWidthF - mGlyphPathBounds.width())

        var transY = areaTopF - mGlyphPathBounds.top
        transY += 0.5f * abs(areaHeightF - mGlyphPathBounds.height())
        mGlyphPath.offset(transX, transY)
        invalidateSelf()
    }

    private fun computeRenderingColor() {
        val newColor: Int = if (mColorStateList != null) {
            mColorStateList!!.getColorForState(state, mRenderingColor)
        } else {
            mColor
        }

        var colorWithAlpha = newColor
        if (alpha >= 0) {
            colorWithAlpha = newColor and 0x00FFFFFF or (mAlpha shl 24)
        }

        if (colorWithAlpha != mRenderingColor) {
            mRenderingColor = colorWithAlpha
            mGlyphPaint.color = mRenderingColor
            invalidateSelf()
        }
    }

    /**
     * Fluent API builder for font icons.
     *
     *
     * Instances of this class can be reused to construct multiple font icons. All properties are
     * kept between builds.
     *
     */
    class Builder internal constructor(private val context: Context) {
        private var mAlpha = -1
        private var mColor = 0
        private var mColorStateList: ColorStateList? = null
        private var mGlyph = 0.toChar()
        private var mIntrinsicSize = -1
        private var mPadding = 0
        private var mRotation = 0f
        private var mTypeface: Typeface? = null

        /**
         * Transparency value, [0..255].
         *
         * @param alpha an alpha value.
         */
        fun setAlphaValue(alpha: Int): Builder {
            mAlpha = alpha
            return this
        }

        /**
         * Resets the transparency value defined by [.setAlphaValue] or
         * [.setOpacity].
         */
        fun unsetAlphaValue(): Builder {
            mAlpha = -1
            return this
        }

        /**
         * Color value, rgb, [0..255] for each channel. If a color StateList is set, it is cleared.
         *
         * @param color a color value. The alpha bits are ignored.
         */
        fun setColorValue(color: Int): Builder {
            mColor = color
            mColorStateList = null
            return this
        }

        /**
         * Color state list, nullable.
         *
         * @param colorStateList color statelist.
         */
        fun setColorStateList(colorStateList: ColorStateList?): Builder {
            mColorStateList = colorStateList
            return this
        }

        /**
         * Color from resources. If a color StateList is set, it is cleared.
         *
         * @param colorResId `R.color` resource ID.
         */
        fun setColorResource(@ColorRes colorResId: Int): Builder {
            mColor = ContextCompat.getColor(context, colorResId)
            mColorStateList = null
            return this
        }

        /**
         * Color StateList from resources.
         *
         * @param colorResId `R.color` resource ID.
         */
        fun setColorStateListResource(colorResId: Int): Builder {
            mColorStateList = ContextCompat.getColorStateList(context, colorResId)
            return this
        }

        /**
         * Font glyph to render.
         *
         * @param glyph the chosen glyph.
         */
        fun setGlyph(glyph: Char): Builder {
            mGlyph = glyph
            return this
        }

        /**
         * Intrinsic size in pixels.
         *
         * @param pixels size in px.
         */
        fun setIntrinsicSizeInPixels(pixels: Int): Builder {
            mIntrinsicSize = pixels
            return this
        }

        /**
         * Intrinsic size in density-independent pixels.
         *
         * @param dips size in dip.
         */
        fun setIntrinsicSizeInDip(dips: Float): Builder {
            return setIntrinsicSize(dips, TypedValue.COMPLEX_UNIT_DIP)
        }

        /**
         * Intrinsic size from resources.
         *
         * @param dimensionResId `R.dimen` resource ID.
         */
        fun setIntrinsicSizeResource(dimensionResId: Int): Builder {
            mIntrinsicSize = context.resources.getDimensionPixelSize(dimensionResId)
            return this
        }

        /**
         * Intrinsic size in a specified unit.
         *
         * @param size the size.
         * @param unit one of `TypedValue.COMPLEX_UNIT_*`.
         */
        fun setIntrinsicSize(size: Float, unit: Int): Builder {
            val dimension = TypedValue.applyDimension(unit, size, context.resources.displayMetrics)
            mIntrinsicSize = round(dimension).toInt()
            return this
        }

        /**
         * Un-sets the intrinsic size (value will be -1).
         */
        fun setNoIntrinsicSize(): Builder {
            mIntrinsicSize = -1
            return this
        }

        /**
         * Transparency value, [0.0f..1.0f].
         *
         * @param opacity an opacity percentage.
         */
        fun setOpacity(opacity: Float): Builder {
            mAlpha = round(opacity * 255).toInt()
            return this
        }

        /**
         * Padding in pixels.
         *
         * @param pixels padding in px.
         */
        fun setPaddingInPixels(pixels: Int): Builder {
            mPadding = pixels
            return this
        }

        /**
         * Padding in density-independent pixels.
         *
         * @param dips padding in dip.
         */
        fun setPaddingInDip(dips: Float): Builder {
            return setPadding(dips, TypedValue.COMPLEX_UNIT_DIP)
        }

        /**
         * Padding from resources.
         *
         * @param dimensionResId `R.dimen` resource ID.
         */
        fun setPaddingResource(dimensionResId: Int): Builder {
            mPadding = context.resources.getDimensionPixelSize(dimensionResId)
            return this
        }

        /**
         * Padding in a specified unit.
         *
         * @param size the size.
         * @param unit one of `TypedValue.COMPLEX_UNIT_*`.
         */
        fun setPadding(size: Float, unit: Int): Builder {
            val dimension = TypedValue.applyDimension(unit, size, context.resources.displayMetrics)
            mPadding = Math.round(dimension)
            return this
        }

        /**
         * Rotation in degrees, where zero is straight up and positive values go clockwise.
         *
         * @param rotation the rotation in degrees.
         */
        fun setRotation(rotation: Float): Builder {
            mRotation = rotation
            return this
        }

        /**
         * The typeface asset to select the glyph from. No caching is done.
         *
         * @param typeface the typeface.
         */
        fun setTypeface(typeface: Typeface): Builder {
            mTypeface = typeface
            return this
        }

        /**
         * Build an `IconFontDrawable` from the current builder state.
         *
         * @return the requested drawable.
         */
        fun build(): IconFontDrawable {
            return IconFontDrawable(
                mAlpha, mColor,
                mColorStateList!!, mGlyph, mIntrinsicSize, mPadding, mRotation, mTypeface!!
            )
        }
    }

    companion object {
        /**
         * Obtain a builder.
         *
         * @param context a context from which to resolve resources.
         * @return a builder.
         */
        fun builder(context: Context): Builder = Builder(context)
    }
}