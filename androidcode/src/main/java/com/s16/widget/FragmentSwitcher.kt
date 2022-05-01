package com.s16.widget

import android.content.Context
import android.content.res.Resources
import android.database.DataSetObserver
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import kotlin.math.min


class FragmentSwitcher : FrameLayout {

    private var mAdapter: PagerAdapter? = null
    private var mObserver: PagerObserver? = null

    private var mCurrentFragment: Fragment? = null
    private var mExpectedAdapterCount = 0
    private var mPopulatePending = false
    private var mFirstLayout = false
    private var mRestoredCurItem = 0
    private var mRestoredAdapterState: Parcelable? = null
    private var mRestoredClassLoader: ClassLoader? = null
    private var mInLayout = false
    private var mCurrentPosition = 0
    private var mOnPageChangeListener: OnPageChangeListener? = null

    private inner class PagerObserver : DataSetObserver() {
        override fun onChanged() {
            dataSetChanged()
        }

        override fun onInvalidated() {
            dataSetChanged()
        }
    }

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    interface OnPageChangeListener {
        fun onPageChanged(page: Int)
    }

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
    }

    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     *
     * @param adapter
     * Adapter to use
     */
    fun setAdapter(adapter: PagerAdapter) {
        mAdapter?.let { a ->
            a.unregisterDataSetObserver(mObserver!!)
            a.startUpdate(this)
            a.destroyItem(this, mCurrentPosition, mCurrentFragment!!)
            a.finishUpdate(this)
            mCurrentPosition = 0
        }

        mAdapter = adapter
        mExpectedAdapterCount = 0
        if (mAdapter != null) {
            if (mObserver == null) {
                mObserver = PagerObserver()
            }
            mAdapter!!.registerDataSetObserver(mObserver!!)
            mPopulatePending = false
            val wasFirstLayout = mFirstLayout
            mFirstLayout = true
            mExpectedAdapterCount = mAdapter!!.count
            if (mRestoredCurItem >= 0) {
                mAdapter!!.restoreState(
                    mRestoredAdapterState,
                    mRestoredClassLoader
                )
                setCurrentItemInternal(mRestoredCurItem, true)
                mRestoredCurItem = -1
                mRestoredAdapterState = null
                mRestoredClassLoader = null
            } else if (!wasFirstLayout) {
                populate()
            } else {
                requestLayout()
            }
        }
    }

    fun getCurrentItem(): Int {
        return mCurrentPosition
    }

    fun getCurrentFragment(): Fragment? {
        return mCurrentFragment
    }

    /**
     * Set the currently selected page.
     *
     * @param item
     * Item index to select
     */
    fun setCurrentItem(item: Int) {
        setCurrentItemInternal(item, false)
    }

    private fun setCurrentItemInternal(item: Int, always: Boolean) {
        var lItem = item
        if (mAdapter == null || mAdapter!!.count <= 0) {
            return
        }
        if (!always && mCurrentPosition == lItem && mCurrentFragment != null) {
            return
        }
        if (lItem < 0) {
            lItem = 0
        } else if (lItem >= mAdapter!!.count) {
            lItem = mAdapter!!.count - 1
        }
        if (mFirstLayout) {
            // We don't have any idea how big we are yet and shouldn't have any
            // pages either.
            // Just set things up and let the pending layout handle things.
            mCurrentPosition = lItem
            requestLayout()
        } else {
            populate(lItem)
        }
    }

    private fun addNewItem(position: Int): Fragment? {
        return try {
            mAdapter!!.instantiateItem(this, position) as Fragment
        } catch (e: ClassCastException) {
            throw RuntimeException(
                "FragmentSwitcher's adapter must instantiate fragments", e
            )
        }
    }

    private fun dataSetChanged() {
        // This method only gets called if our observer is attached, so mAdapter
        // is non-null.

        // This method only gets called if our observer is attached, so mAdapter
        // is non-null.
        val adapterCount = mAdapter!!.count
        mExpectedAdapterCount = adapterCount
        var needPopulate = mCurrentFragment == null
        var newCurrItem = mCurrentPosition

        var isUpdating = false
        val newPos = mAdapter!!.getItemPosition(mCurrentFragment!!)

        if (newPos == PagerAdapter.POSITION_NONE) {
            if (!isUpdating) {
                mAdapter!!.startUpdate(this)
                isUpdating = true
            }
            mAdapter!!.destroyItem(this, mCurrentPosition, mCurrentFragment!!)
            mCurrentFragment = null

            // Keep the current item in the valid range
            newCurrItem = Math.max(
                0,
                min(mCurrentPosition, adapterCount - 1)
            )
            needPopulate = true
        } else if (mCurrentPosition != newPos) {
            // Our current item changed position. Follow it.
            newCurrItem = newPos
            needPopulate = true
        }

        if (isUpdating) {
            mAdapter!!.finishUpdate(this)
        }

        if (needPopulate) {
            setCurrentItemInternal(newCurrItem, true)
            requestLayout()
        }
    }

    private fun populate() {
        populate(mCurrentPosition)
    }

    private fun populate(position: Int) {
        if (mAdapter == null) {
            return
        }

        // Bail now if we are waiting to populate. This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            return
        }

        // Also, don't populate until we are attached to a window. This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (windowToken == null) {
            return
        }
        val N = mAdapter!!.count
        if (N != mExpectedAdapterCount) {
            val resName: String = try {
                resources.getResourceName(id)
            } catch (e: Resources.NotFoundException) {
                Integer.toHexString(id)
            }
            throw IllegalStateException(
                "The application's PagerAdapter changed the adapter's"
                        + " contents without calling PagerAdapter#notifyDataSetChanged!"
                        + " Expected adapter item count: "
                        + mExpectedAdapterCount + ", found: " + N
                        + " Pager id: " + resName + " Pager class: "
                        + javaClass + " Problematic adapter: "
                        + mAdapter!!.javaClass
            )
        }
        mAdapter!!.startUpdate(this)
        if (mCurrentFragment != null && mCurrentPosition != position) {
            mAdapter!!.destroyItem(this, mCurrentPosition, mCurrentFragment!!)
        }

        // Locate the currently focused item or add it if needed.
        if (((mCurrentFragment == null || mCurrentPosition != position)
                    && mAdapter!!.count > 0)
        ) {
            mCurrentFragment = addNewItem(position)
            mCurrentPosition = position
            if (mOnPageChangeListener != null) {
                mOnPageChangeListener!!.onPageChanged(mCurrentPosition)
            }
        }
        mAdapter!!.setPrimaryItem(this, mCurrentPosition, (mCurrentFragment)!!)
        mAdapter!!.finishUpdate(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mFirstLayout = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mInLayout = true
        populate()
        mInLayout = false
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.position = mCurrentPosition
        if (mAdapter != null) {
            ss.adapterState = mAdapter!!.saveState()
        }
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss = state
        super.onRestoreInstanceState(ss.superState)
        if (mAdapter != null) {
            mAdapter!!.restoreState(ss.adapterState, ss.loader)
            setCurrentItemInternal(ss.position, true)
        } else {
            mRestoredCurItem = ss.position
            mRestoredAdapterState = ss.adapterState
            mRestoredClassLoader = ss.loader
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        var lParams = params
        if (!checkLayoutParams(lParams)) {
            lParams = generateLayoutParams(lParams)
        }
        if (mInLayout) {
            addViewInLayout(child, index, lParams)
        } else {
            super.addView(child, index, lParams)
        }
    }

    override fun removeView(view: View?) {
        if (mInLayout) {
            removeViewInLayout(view)
        } else {
            super.removeView(view)
        }
    }

    override fun onLayout(
        changed: Boolean, left: Int, top: Int, right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        mFirstLayout = false
    }

    /**
     * Retrieve the current adapter supplying pages.
     *
     * @return The currently registered PagerAdapter
     */
    fun getAdapter(): PagerAdapter? {
        return mAdapter
    }

    /**
     * Set a listener that will be invoked whenever the page changes or is
     * incrementally scrolled. See [OnPageChangeListener].
     *
     * @param listener
     * Listener to set
     */
    fun setOnPageChangeListener(listener: OnPageChangeListener) {
        mOnPageChangeListener = listener
    }

    /**
     * This is the persistent state that is saved by FragmentSwitcher.
     */
    class SavedState : BaseSavedState {
        var position = 0
        var adapterState: Parcelable? = null
        var loader: ClassLoader? = null

        constructor(superState: Parcelable?) : super(superState) {}

        private constructor(source: Parcel) : this(source, null) {
        }

        private constructor(source: Parcel, loader: ClassLoader?) : super(source) {
            var lLoader: ClassLoader? = loader
            if (lLoader == null) {
                lLoader = javaClass.classLoader
            }
            position = source.readInt()
            adapterState = source.readParcelable(lLoader)
            this.loader = lLoader
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(position)
            out.writeParcelable(adapterState, flags)
        }

        override fun toString(): String {
            return ("FragmentSwitcher.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " position=" + position + "}")
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
}