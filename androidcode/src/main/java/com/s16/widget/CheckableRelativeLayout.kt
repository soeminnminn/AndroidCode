package com.s16.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat


/**
 * Created by SMM on 9/12/2016.
 */
class CheckableRelativeLayout : RelativeLayout, Checkable {

    private var mChecked = false
    private var mBroadcasting = false
    private var mButtonDrawable: Drawable? = null
    private var mButtonTintList: ColorStateList? = null
    private var mButtonTintMode: PorterDuff.Mode? = null
    private var mHasButtonTint = false
    private var mHasButtonTintMode = false
    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a layout view changed.
     */
    interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a layout view has changed.
         *
         * @param view The layout view whose state has changed.
         * @param isChecked  The new checked state of layout view.
         */
        fun onCheckedChanged(view: CheckableRelativeLayout?, isChecked: Boolean)
    }

    internal class SavedState : BaseSavedState {
        var checked = false

        /**
         * Constructor called from [CheckableRelativeLayout.onSaveInstanceState]
         */
        constructor(superState: Parcelable?) : super(superState) {}

        /**
         * Constructor called from [.CREATOR]
         */
        private constructor(`in`: Parcel) : super(`in`) {
            checked = (`in`.readValue(null) as Boolean?)!!
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeValue(checked)
        }

        override fun toString(): String {
            return ("CheckableImageButton.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " checked=" + checked + "}")
        }

        companion object CREATOR : Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    constructor(context: Context) : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        var a: TypedArray = context.obtainStyledAttributes(attrs, ATTR_CHECKED)
        val checked = a.getBoolean(0, false)
        a.recycle()
        a = context.obtainStyledAttributes(attrs, ATTR_BUTTON)
        val d = a.getDrawable(0)
        d?.let { setButtonDrawable(it) }
        a.recycle()
        isChecked = checked
        applyButtonTint()
    }

    private val isLayoutRtl: Boolean
        get() = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL

    /**
     * Sets a drawable as the compound button image given its resource
     * identifier.
     *
     * @param resId the resource identifier of the drawable
     * @attr ref android.R.styleable#CompoundButton_button
     */
    fun setButtonDrawable(@DrawableRes resId: Int) {
        val d: Drawable? = if (resId != 0) {
            ContextCompat.getDrawable(context, resId)
        } else {
            null
        }
        setButtonDrawable(d)
    }

    /**
     * @return the drawable used as the compound button image
     * @see #setButtonDrawable(Drawable)
     * @see #setButtonDrawable(int)
     */
    fun getButtonDrawable() = mButtonDrawable

    /**
     * Sets a drawable as the compound button image.
     *
     * @param drawable the drawable to set
     * @attr ref android.R.styleable#CompoundButton_button
     */
    fun setButtonDrawable(drawable: Drawable?) {
        if (mButtonDrawable !== drawable) {
            if (mButtonDrawable != null) {
                mButtonDrawable!!.callback = null
                unscheduleDrawable(mButtonDrawable)
            }
            mButtonDrawable = drawable
            if (drawable != null) {
                drawable.callback = this
                val layoutDirection = ViewCompat.getLayoutDirection(this)
                DrawableCompat.setLayoutDirection(drawable, layoutDirection)
                if (drawable.isStateful) {
                    drawable.state = drawableState
                }
                drawable.setVisible(visibility == VISIBLE, false)
                // setMinHeight(drawable.getIntrinsicHeight());
            }
        }
    }

    /**
     * @return the tint applied to the button drawable
     * @attr ref android.R.styleable#CompoundButton_buttonTint
     * @see .setButtonTintList
     */
    fun getButtonTintList(): ColorStateList? = mButtonTintList

    /**
     * Applies a tint to the button drawable. Does not modify the current tint
     * mode, which is [PorterDuff.Mode.SRC_IN] by default.
     *
     *
     * Subsequent calls to [.setButtonDrawable] will
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using
     * [Drawable.setTintList].
     *
     * @param tint the tint to apply, may be `null` to clear tint
     *
     * @attr ref android.R.styleable#CompoundButton_buttonTint
     * @see .setButtonTintList
     * @see Drawable.setTintList
     */
    fun setButtonTintList(tint : ColorStateList?) {
        mButtonTintList = tint
        mHasButtonTint = true
        applyButtonTint()
    }

    /**
     * @return the blending mode used to apply the tint to the button drawable
     * @attr ref android.R.styleable#CompoundButton_buttonTintMode
     * @see .setButtonTintMode
     */
    fun getButtonTintMode(): PorterDuff.Mode? = mButtonTintMode

    /**
     * Specifies the blending mode used to apply the tint specified by
     * [.setButtonTintList]} to the button drawable. The
     * default mode is [PorterDuff.Mode.SRC_IN].
     *
     * @param tintMode the blending mode used to apply the tint, may be
     * `null` to clear tint
     * @attr ref android.R.styleable#CompoundButton_buttonTintMode
     * @see .getButtonTintMode
     * @see Drawable.setTintMode
     */
    fun setButtonTintMode(tintMode: PorterDuff.Mode?) {
        mButtonTintMode = tintMode
        mHasButtonTintMode = true
        applyButtonTint()
    }

    private fun applyButtonTint() {
        if (mButtonDrawable != null && (mHasButtonTint || mHasButtonTintMode)) {
            mButtonDrawable = mButtonDrawable!!.mutate()
            if (mHasButtonTint) {
                DrawableCompat.setTintList(mButtonDrawable!!, mButtonTintList)
            }
            if (mHasButtonTintMode) {
                DrawableCompat.setTintMode(mButtonDrawable!!, mButtonTintMode!!)
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mButtonDrawable!!.isStateful) {
                mButtonDrawable!!.state = drawableState
            }
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked != checked) {
            mChecked = checked
            refreshDrawableState()

            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return
            }
            mBroadcasting = true
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener!!.onCheckedChanged(this, mChecked)
            }
            mBroadcasting = false
        }
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        isChecked = !mChecked
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val myDrawableState = drawableState

        // Set the state of the Drawable
        mButtonDrawable?.state = myDrawableState
        invalidate()
    }

    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)
        if (mButtonDrawable != null) {
            DrawableCompat.setHotspot(mButtonDrawable!!, x, y)
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who === mButtonDrawable
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        mButtonDrawable?.jumpToCurrentState()
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = CheckableRelativeLayout::class.java.name
        event.isChecked = mChecked
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = CheckableRelativeLayout::class.java.name
        info.isCheckable = true
        info.isChecked = mChecked
    }

    override fun dispatchDraw(canvas: Canvas) {
        val buttonDrawable = mButtonDrawable
        if (buttonDrawable != null) {
            val drawableHeight = buttonDrawable.intrinsicHeight
            val drawableWidth = buttonDrawable.intrinsicWidth
            val top = (height - drawableHeight) / 2
            val bottom = top + drawableHeight
            val left = if (isLayoutRtl) width - drawableWidth else 0
            val right = if (isLayoutRtl) width else drawableWidth
            buttonDrawable.setBounds(left, top, right, bottom)

            val background = background
            if (background != null) {
                DrawableCompat.setHotspotBounds(background, left, top, right, bottom)
            }
        }
        super.dispatchDraw(canvas)
        if (buttonDrawable != null) {
            val scrollX = scrollX.toFloat()
            val scrollY = scrollY.toFloat()

            if (scrollX == 0f && scrollY == 0f) {
                buttonDrawable.draw(canvas)
            } else {
                canvas.translate(scrollX, scrollY)
                buttonDrawable.draw(canvas)
                canvas.translate(-scrollX, -scrollY)
            }
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this layout view
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        mOnCheckedChangeListener = listener
    }

    public override fun onSaveInstanceState(): Parcelable {
        // Force our ancestor class to save its state
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.checked = isChecked
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        isChecked = ss.checked
        requestLayout()
    }

    companion object {
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
        private val ATTR_CHECKED = intArrayOf(android.R.attr.checked)
        private val ATTR_BUTTON = intArrayOf(android.R.attr.button)
    }
}