package com.s16.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageButton


class CheckableImageButton : AppCompatImageButton, Checkable {

    private var mAutoCheckable = true
    private var mChecked = false
    private var mBroadcasting = false
    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null
    private var mOnCheckedChangeWidgetListener: OnCheckedChangeListener? = null

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param isChecked  The new checked state of buttonView.
         */
        fun onCheckedChanged(buttonView: CheckableImageButton?, isChecked: Boolean)
    }

    constructor(context: Context) : this(context, null) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        val a: TypedArray = context.obtainStyledAttributes(attrs, ATTRS)
        val checked = a.getBoolean(0, false)
        a.recycle()
        isChecked = checked
    }

    override fun performClick(): Boolean {
        /*
         * XXX: These are tiny, need some surrounding 'expanded touch area',
         * which will need to be implemented in Button if we only override
         * performClick()
         */

        /* When clicked, toggle the state */
        if (mAutoCheckable) {
            toggle()
        }
        return super.performClick()
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = CheckableImageButton::class.java.name
        event.isChecked = mChecked
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = CheckableImageButton::class.java.name
        info.isCheckable = true
        info.isChecked = mChecked
    }

    override fun setOnClickListener(l: OnClickListener?) {
        mAutoCheckable = l == null
        super.setOnClickListener(l)
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        mOnCheckedChangeListener = listener
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes. This callback is used for internal purpose only.
     *
     * @param listener the callback to call on checked state change
     * @hide
     */
    fun setOnCheckedChangeWidgetListener(listener: OnCheckedChangeListener?) {
        mOnCheckedChangeWidgetListener = listener
    }

    override fun setChecked(checked: Boolean) {
        if (mChecked != checked) {
            mChecked = checked
            refreshDrawableState()
            //notifyViewAccessibilityStateChangedIfNeeded(AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);

            // Avoid infinite recursions if setChecked() is called from a listener
            if (mBroadcasting) {
                return
            }
            mBroadcasting = true
            if (mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener!!.onCheckedChanged(this, mChecked)
            }
            if (mOnCheckedChangeWidgetListener != null) {
                mOnCheckedChangeWidgetListener!!.onCheckedChanged(this, mChecked)
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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    internal class SavedState : BaseSavedState {
        var checked = false

        /**
         * Constructor called from [CheckableImageButton.onSaveInstanceState]
         */
        constructor(superState: Parcelable?) : super(superState) {}

        /**
         * Constructor called from [.CREATOR]
         */
        private constructor(source: Parcel) : super(source) {
            checked = (source.readValue(null) as Boolean?)!!
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

        override fun describeContents(): Int {
            return 0
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
        private val ATTRS = intArrayOf(android.R.attr.checked)
    }
}