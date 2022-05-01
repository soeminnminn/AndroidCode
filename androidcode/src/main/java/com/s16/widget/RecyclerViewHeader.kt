package com.s16.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager


class RecyclerViewHeader : RelativeLayout {

    private var mRecycler: RecyclerView? = null

    private var mDownScroll = 0
    private var mCurrentScroll = 0
    private var mReversed = false
    private var mAlreadyAligned = false
    private var mRecyclerWantsTouchEvent = false

    constructor(context: Context)
            : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {
    }

    /**
     * Attaches `RecyclerViewHeader` to `RecyclerView`.
     * This method will perform necessary actions to properly align the header within `RecyclerView`.
     * Be sure that `setLayoutManager(...)` has been called for `RecyclerView` before calling this method.
     * Also, if you were planning to use `setOnScrollListener(...)` method for your `RecyclerView`, be sure to do it before calling this method.
     *
     * @param recycler `RecyclerView` to attach `RecyclerViewHeader` to.
     */
    fun attachTo(recycler: RecyclerView) {
        attachTo(recycler, false)
    }

    /**
     * Attaches `RecyclerViewHeader` to `RecyclerView`.
     * Be sure that `setLayoutManager(...)` has been called for `RecyclerView` before calling this method.
     * Also, if you were planning to use `setOnScrollListener(...)` method for your `RecyclerView`, be sure to do it before calling this method.
     *
     * @param recycler             `RecyclerView` to attach `RecyclerViewHeader` to.
     * @param headerAlreadyAligned If set to `false`, method will perform necessary actions to properly align
     * the header within `RecyclerView`. If set to `true` method will assume,
     * that user has already aligned `RecyclerViewHeader` properly.
     */
    fun attachTo(recycler: RecyclerView, headerAlreadyAligned: Boolean) {
        validateRecycler(recycler, headerAlreadyAligned)
        mRecycler = recycler
        mAlreadyAligned = headerAlreadyAligned
        mReversed = isLayoutManagerReversed(recycler)
        setupAlignment(recycler)
        setupHeader(recycler)
    }

    private fun isLayoutManagerReversed(recycler: RecyclerView): Boolean {
        var reversed = false
        val manager = recycler.layoutManager
        if (manager is LinearLayoutManager) {
            reversed = manager.reverseLayout
        } else if (manager is StaggeredGridLayoutManager) {
            reversed = manager.reverseLayout
        }
        return reversed
    }

    private fun setupAlignment(recycler: RecyclerView) {
        if (!mAlreadyAligned) {
            //setting alignment of header
            val currentParams = layoutParams
            val newHeaderParams: FrameLayout.LayoutParams
            val width = ViewGroup.LayoutParams.WRAP_CONTENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            val gravity =
                (if (mReversed) Gravity.BOTTOM else Gravity.TOP) or Gravity.CENTER_HORIZONTAL
            if (currentParams != null) {
                newHeaderParams = FrameLayout.LayoutParams(layoutParams) //to copy all the margins
                newHeaderParams.width = width
                newHeaderParams.height = height
                newHeaderParams.gravity = gravity
            } else {
                newHeaderParams = FrameLayout.LayoutParams(width, height, gravity)
            }
            this@RecyclerViewHeader.layoutParams = newHeaderParams

            //setting alignment of recycler
            val newRootParent = FrameLayout(recycler.context)
            newRootParent.layoutParams = recycler.layoutParams
            val currentParent = recycler.parent
            if (currentParent is ViewGroup) {
                val indexWithinParent = currentParent.indexOfChild(recycler)
                currentParent.removeViewAt(indexWithinParent)
                recycler.layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                newRootParent.addView(recycler)
                newRootParent.addView(this@RecyclerViewHeader)
                currentParent.addView(newRootParent, indexWithinParent)
            }
        }
    }

    private fun setupHeader(recycler: RecyclerView) {
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                mCurrentScroll += dy
                this@RecyclerViewHeader.translationY = -mCurrentScroll.toFloat()
            }
        })
        this@RecyclerViewHeader.viewTreeObserver.addOnGlobalLayoutListener(object :
            OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                var height = this@RecyclerViewHeader.height
                if (height > 0) {
                    this@RecyclerViewHeader.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    if (mAlreadyAligned) {
                        val params = layoutParams as MarginLayoutParams
                        height += params.topMargin
                        height += params.bottomMargin
                    }
                    recycler.addItemDecoration(
                        HeaderItemDecoration(recycler.layoutManager!!, height),
                        0
                    )
                }
            }
        })
    }

    private fun validateRecycler(recycler: RecyclerView, headerAlreadyAligned: Boolean) {
        val layoutManager = recycler.layoutManager
        //not using instanceof on purpose
        checkNotNull(layoutManager) { "Be sure to call RecyclerViewHeader constructor after setting your RecyclerView's LayoutManager." }
        require(
            !(layoutManager.javaClass != LinearLayoutManager::class.java //not using instanceof on purpose
                    && layoutManager.javaClass != GridLayoutManager::class.java && layoutManager !is StaggeredGridLayoutManager)
        ) { "Currently RecyclerViewHeader supports only LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager." }
        if (layoutManager is LinearLayoutManager) {
            require(!(layoutManager.orientation != LinearLayoutManager.VERTICAL)) { "Currently RecyclerViewHeader supports only VERTICAL orientation LayoutManagers." }
        } else if (layoutManager is StaggeredGridLayoutManager) {
            require(!(layoutManager.orientation != StaggeredGridLayoutManager.VERTICAL)) { "Currently RecyclerViewHeader supports only VERTICAL orientation StaggeredGridLayoutManagers." }
        }
        if (!headerAlreadyAligned) {
            val parent = recycler.parent
            check(
                !(parent != null &&
                        parent !is LinearLayout &&
                        parent !is FrameLayout &&
                        parent !is RelativeLayout)
            ) {
                "Currently, NOT already aligned RecyclerViewHeader " +
                        "can only be used for RecyclerView with a parent of one of types: LinearLayout, FrameLayout, RelativeLayout."
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        mRecyclerWantsTouchEvent = mRecycler!!.onInterceptTouchEvent(ev)
        if (mRecyclerWantsTouchEvent && ev.action == MotionEvent.ACTION_DOWN) {
            mDownScroll = mCurrentScroll
        }
        return mRecyclerWantsTouchEvent || super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mRecyclerWantsTouchEvent) {
            val scrollDiff = mCurrentScroll - mDownScroll
            val recyclerEvent = MotionEvent.obtain(
                event.downTime, event.eventTime, event.action,
                event.x, event.y - scrollDiff, event.metaState
            )
            mRecycler!!.onTouchEvent(recyclerEvent)
            return false
        }
        return super.onTouchEvent(event)
    }

    private inner class HeaderItemDecoration(
        layoutManager: RecyclerView.LayoutManager,
        height: Int
    ) : RecyclerView.ItemDecoration() {
        private var mHeaderHeight = height
        private var mNumberOfChildren = 0

        init {
            if (layoutManager.javaClass == LinearLayoutManager::class.java) {
                mNumberOfChildren = 1
            } else if (layoutManager.javaClass == GridLayoutManager::class.java) {
                mNumberOfChildren = (layoutManager as GridLayoutManager).spanCount
            } else if (layoutManager is StaggeredGridLayoutManager) {
                mNumberOfChildren = layoutManager.spanCount
            }
        }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            val value = if (parent.getChildLayoutPosition(view) < mNumberOfChildren) mHeaderHeight else 0
            if (mReversed) {
                outRect.bottom = value
            } else {
                outRect.top = value
            }
        }
    }

}