package com.s16.widget

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.*
import android.text.BoringLayout
import android.text.Layout
import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import android.util.AttributeSet
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.widget.EdgeEffect
import android.widget.OverScroller
import androidx.core.text.TextDirectionHeuristicCompat
import androidx.core.text.TextDirectionHeuristicsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.s16.android.R
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@SuppressLint("ClickableViewAccessibility")
class HorizontalPicker : View {

    /**
     * Determines speed during touch scrolling.
     */
    private var mVelocityTracker: VelocityTracker? = null

    /**
     * @see android.view.ViewConfiguration.getScaledMinimumFlingVelocity
     */
    private var mMinimumFlingVelocity = 0

    /**
     * @see android.view.ViewConfiguration.getScaledMaximumFlingVelocity
     */
    private var mMaximumFlingVelocity = 0

    private var mOverscrollDistance = 0

    private var mTouchSlop = 0

    private var mValues: Array<CharSequence> = arrayOf()
    private var mLayouts: Array<BoringLayout> = arrayOf()

    private var mTextPaint: TextPaint? = null
    private var mBoringMetrics: BoringLayout.Metrics? = null
    private var mEllipsize: TruncateAt? = null

    private var mItemWidth = 0
    private var mItemClipBounds: RectF? = null
    private var mItemClipBoundsOffser: RectF? = null

    private var mLastDownEventX = 0f

    private var mFlingScrollerX: OverScroller? = null
    private var mAdjustScrollerX: OverScroller? = null

    private var mPreviousScrollerX = 0

    private var mScrollingX = false
    private var mPressedItem = -1

    private var mTextColor: ColorStateList? = null

    private var mOnItemSelected: OnItemSelected? = null
    private var mOnItemClicked: OnItemClicked? = null

    private var mSelectedItem = 0

    private var mLeftEdgeEffect: EdgeEffect? = null
    private var mRightEdgeEffect: EdgeEffect? = null

    private var mMarquee: Marquee? = null
    private var mMarqueeRepeatLimit = 3

    private var mDividerSize = 0f

    private var mSideItems = 1

    private var mTextDir: TextDirectionHeuristicCompat? = null

    private var mTouchHelper: PickerTouchHelper? = null

    private class Marquee(v: HorizontalPicker, l: Layout, rtl: Boolean) : Handler() {

        private var mView: WeakReference<HorizontalPicker>? = null
        private var mLayout: WeakReference<Layout>? = null

        private var mStatus = MARQUEE_STOPPED
        private var mScrollUnit = 0f
        private var mMaxScroll = 0f
        private var mMaxFadeScroll = 0f
        private var mGhostStart = 0f
        private var mGhostOffset = 0f
        private var mFadeStop = 0f
        private var mRepeatLimit = 0

        private var mScroll = 0f

        private var mRtl = rtl

        init {
            val density = v.context.resources.displayMetrics.density
            val scrollUnit = MARQUEE_PIXELS_PER_SECOND * density / MARQUEE_RESOLUTION
            mScrollUnit = if (rtl) {
                -scrollUnit
            } else {
                scrollUnit
            }
            mView = WeakReference(v)
            mLayout = WeakReference(l)
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_START -> {
                    mStatus = MARQUEE_RUNNING
                    tick()
                }
                MESSAGE_TICK -> tick()
                MESSAGE_RESTART -> if (mStatus == MARQUEE_RUNNING) {
                    if (mRepeatLimit >= 0) {
                        mRepeatLimit--
                    }
                    start(mRepeatLimit)
                }
            }
        }

        fun tick() {
            if (mStatus != MARQUEE_RUNNING) {
                return
            }
            removeMessages(MESSAGE_TICK)
            val view = mView!!.get()
            val layout = mLayout!!.get()
            if (view != null && layout != null && (view.isFocused || view.isSelected)) {
                mScroll += mScrollUnit
                if (Math.abs(mScroll) > mMaxScroll) {
                    mScroll = mMaxScroll
                    if (mRtl) {
                        mScroll *= -1f
                    }
                    sendEmptyMessageDelayed(MESSAGE_RESTART, MARQUEE_RESTART_DELAY.toLong())
                } else {
                    sendEmptyMessageDelayed(MESSAGE_TICK, MARQUEE_RESOLUTION.toLong())
                }
                view.invalidate()
            }
        }

        fun stop() {
            mStatus = MARQUEE_STOPPED
            removeMessages(MESSAGE_START)
            removeMessages(MESSAGE_RESTART)
            removeMessages(MESSAGE_TICK)
            resetScroll()
        }

        private fun resetScroll() {
            mScroll = 0.0f
            val view = mView!!.get()
            view?.invalidate()
        }

        fun start(repeatLimit: Int) {
            if (repeatLimit == 0) {
                stop()
                return
            }
            mRepeatLimit = repeatLimit
            val view = mView!!.get()
            val layout = mLayout!!.get()
            if (view != null && layout != null) {
                mStatus = MARQUEE_STARTING
                mScroll = 0.0f
                val textWidth = view.mItemWidth
                val lineWidth = layout.getLineWidth(0)
                val gap = textWidth / 3.0f
                mGhostStart = lineWidth - textWidth + gap
                mMaxScroll = mGhostStart + textWidth
                mGhostOffset = lineWidth + gap
                mFadeStop = lineWidth + textWidth / 6.0f
                mMaxFadeScroll = mGhostStart + lineWidth + lineWidth
                if (mRtl) {
                    mGhostOffset *= -1f
                }
                view.invalidate()
                sendEmptyMessageDelayed(MESSAGE_START, MARQUEE_DELAY.toLong())
            }
        }

        fun getGhostOffset(): Float {
            return mGhostOffset
        }

        fun getScroll(): Float {
            return mScroll
        }

        fun getMaxFadeScroll(): Float {
            return mMaxFadeScroll
        }

        fun shouldDrawLeftFade(): Boolean {
            return mScroll <= mFadeStop
        }

        fun shouldDrawGhost(): Boolean {
            return mStatus == MARQUEE_RUNNING && abs(mScroll) > mGhostStart
        }

        fun isRunning(): Boolean {
            return mStatus == MARQUEE_RUNNING
        }

        fun isStopped(): Boolean {
            return mStatus == MARQUEE_STOPPED
        }

        companion object {
            private const val MARQUEE_DELTA_MAX = 0.07f
            private const val MARQUEE_DELAY = 1200
            private const val MARQUEE_RESTART_DELAY = 1200
            private const val MARQUEE_RESOLUTION = 1000 / 30
            private const val MARQUEE_PIXELS_PER_SECOND = 30

            private const val MARQUEE_STOPPED: Byte = 0x0
            private const val MARQUEE_STARTING: Byte = 0x1
            private const val MARQUEE_RUNNING: Byte = 0x2

            private const val MESSAGE_START = 0x1
            private const val MESSAGE_TICK = 0x2
            private const val MESSAGE_RESTART = 0x3
        }
    }

    private class PickerTouchHelper(picker: HorizontalPicker) : ExploreByTouchHelper(picker) {
        private val mPicker: HorizontalPicker = picker

        override fun getVirtualViewAt(x: Float, y: Float): Int {
            val itemWidth = mPicker.mItemWidth + mPicker.mDividerSize
            val position = mPicker.scrollX + x - itemWidth * mPicker.mSideItems
            val item = position / itemWidth
            return if (item < 0 || item > mPicker.mValues.size) {
                INVALID_ID
            } else item.toInt()
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int?>) {
            val itemWidth = mPicker.mItemWidth + mPicker.mDividerSize
            val position = mPicker.scrollX - itemWidth * mPicker.mSideItems
            var first = (position / itemWidth).toInt()
            var items = mPicker.mSideItems * 2 + 1
            if (position % itemWidth != 0f) { // if start next item is starting to appear on screen
                items++
            }
            if (first < 0) {
                items += first
                first = 0
            } else if (first + items > mPicker.mValues.size) {
                items = mPicker.mValues.size - first
            }
            for (i in 0 until items) {
                virtualViewIds.add(first + i)
            }
        }

        override fun onPopulateEventForVirtualView(virtualViewId: Int, event: AccessibilityEvent) {
            event.contentDescription = mPicker.mValues[virtualViewId]
        }

        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat
        ) {
            val itemWidth = mPicker.mItemWidth + mPicker.mDividerSize
            val scrollOffset = mPicker.scrollX - itemWidth * mPicker.mSideItems
            val left = (virtualViewId * itemWidth - scrollOffset).toInt()
            val right = left + mPicker.mItemWidth
            node.contentDescription = mPicker.mValues[virtualViewId]
            node.setBoundsInParent(Rect(left, 0, right, mPicker.height))
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            return false
        }
    }

    class SavedState : BaseSavedState {
        var mSelItem = 0

        constructor(superState: Parcelable?) : super(superState) {}

        private constructor(source: Parcel) : this(source, null) {
        }

        private constructor(source: Parcel, loader: ClassLoader?) : super(source) {
            mSelItem = source.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeInt(mSelItem)
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun toString(): String {
            return ("HorizontalPicker.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " selItem=" + mSelItem
                    + "}")
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    interface OnItemSelected {
        fun onItemSelected(index: Int)
    }

    interface OnItemClicked {
        fun onItemClicked(index: Int)
    }

    constructor(context: Context) : super(context) {
        init(context, null, R.attr.horizontalPickerStyle)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, R.attr.horizontalPickerStyle)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // create the selector wheel paint
        mTextPaint = TextPaint().apply {
            isAntiAlias = true
        }

        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.HorizontalPicker,
            defStyleAttr, 0
        )

        val values: Array<CharSequence>
        var ellipsize = 3 // END default value

        var sideItems = mSideItems

        try {
            mTextColor = a.getColorStateList(R.styleable.HorizontalPicker_android_textColor)
            if (mTextColor == null) {
                mTextColor = ColorStateList.valueOf(-0x1000000)
            }
            values = a.getTextArray(R.styleable.HorizontalPicker_values)
            ellipsize = a.getInt(R.styleable.HorizontalPicker_android_ellipsize, ellipsize)
            mMarqueeRepeatLimit = a.getInt(
                R.styleable.HorizontalPicker_android_marqueeRepeatLimit,
                mMarqueeRepeatLimit
            )
            mDividerSize = a.getDimension(R.styleable.HorizontalPicker_dividerSize, mDividerSize)
            sideItems = a.getInt(R.styleable.HorizontalPicker_sideItems, sideItems)
            val textSize = a.getDimension(R.styleable.HorizontalPicker_android_textSize, -1f)
            if (textSize > -1) {
                setTextSize(textSize)
            }
        } finally {
            a.recycle()
        }

        when (ellipsize) {
            1 -> setEllipsize(TruncateAt.START)
            2 -> setEllipsize(TruncateAt.MIDDLE)
            3 -> setEllipsize(TruncateAt.END)
            4 -> setEllipsize(TruncateAt.MARQUEE)
        }

        val fontMetricsInt: Paint.FontMetricsInt = mTextPaint!!.fontMetricsInt
        mBoringMetrics = BoringLayout.Metrics()
        mBoringMetrics!!.ascent = fontMetricsInt.ascent
        mBoringMetrics!!.bottom = fontMetricsInt.bottom
        mBoringMetrics!!.descent = fontMetricsInt.descent
        mBoringMetrics!!.leading = fontMetricsInt.leading
        mBoringMetrics!!.top = fontMetricsInt.top
        mBoringMetrics!!.width = mItemWidth

        setWillNotDraw(false)

        mFlingScrollerX = OverScroller(context)
        mAdjustScrollerX = OverScroller(context, DecelerateInterpolator(2.5f))

        // initialize constants

        // initialize constants
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumFlingVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumFlingVelocity = (configuration.scaledMaximumFlingVelocity
                / SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT)
        mOverscrollDistance = configuration.scaledOverscrollDistance

        mPreviousScrollerX = Int.MIN_VALUE

        setValues(values)
        setSideItems(sideItems)

        mTouchHelper = PickerTouchHelper(this)
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val height: Int
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize
        } else {
            val fontMetrics = mTextPaint!!.fontMetrics
            var heightText = (abs(fontMetrics.ascent) + abs(fontMetrics.descent)).toInt()
            heightText += paddingTop + paddingBottom
            height = if (heightMode == MeasureSpec.AT_MOST) {
                min(heightSize, heightText)
            } else {
                heightText
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val saveCount: Int = canvas.saveCount
        canvas.save()
        val selectedItem = mSelectedItem
        val itemWithPadding = mItemWidth + mDividerSize

        // translate horizontal to center
        canvas.translate(itemWithPadding * mSideItems, 0f)
        for (i in mValues.indices) {

            // set text color for item
            mTextPaint!!.color = getTextColor(i)

            // get text layout
            val layout = mLayouts[i]
            val saveCountHeight: Int = canvas.saveCount
            canvas.save()
            var x = 0f
            val lineWidth = layout.getLineWidth(0)
            if (lineWidth > mItemWidth) {
                if (isRtl(mValues[i])) {
                    x += (lineWidth - mItemWidth) / 2
                } else {
                    x -= (lineWidth - mItemWidth) / 2
                }
            }
            if (mMarquee != null && i == selectedItem) {
                x += mMarquee!!.getScroll()
            }

            // translate vertically to center
            canvas.translate(-x, ((canvas.height - layout.height) / 2).toFloat())
            var clipBounds: RectF
            if (x == 0f) {
                clipBounds = mItemClipBounds!!
            } else {
                clipBounds = mItemClipBoundsOffser!!
                clipBounds.set(mItemClipBounds!!)
                clipBounds.offset(x, 0f)
            }
            canvas.clipRect(clipBounds)
            layout.draw(canvas)
            if (mMarquee != null && i == selectedItem && mMarquee!!.shouldDrawGhost()) {
                canvas.translate(mMarquee!!.getGhostOffset(), 0f)
                layout.draw(canvas)
            }

            // restore vertical translation
            canvas.restoreToCount(saveCountHeight)

            // translate horizontal for 1 item
            canvas.translate(itemWithPadding, 0f)
        }

        // restore horizontal translation
        canvas.restoreToCount(saveCount)
        drawEdgeEffect(canvas, mLeftEdgeEffect, 270)
        drawEdgeEffect(canvas, mRightEdgeEffect, 90)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        mTextDir = getTextDirectionHeuristic()
    }

    private fun isRtl(text: CharSequence): Boolean {
        if (mTextDir == null) {
            mTextDir = getTextDirectionHeuristic()
        }
        return mTextDir!!.isRtl(text, 0, text.length)
    }

    private fun getTextDirectionHeuristic(): TextDirectionHeuristicCompat? {
        // Always need to resolve layout direction first
        val defaultIsRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        return when (textDirection) {
            TEXT_DIRECTION_FIRST_STRONG -> if (defaultIsRtl) TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL else TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR
            TEXT_DIRECTION_ANY_RTL -> TextDirectionHeuristicsCompat.ANYRTL_LTR
            TEXT_DIRECTION_LTR -> TextDirectionHeuristicsCompat.LTR
            TEXT_DIRECTION_RTL -> TextDirectionHeuristicsCompat.RTL
            TEXT_DIRECTION_LOCALE -> TextDirectionHeuristicsCompat.LOCALE
            else -> if (defaultIsRtl) TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL else TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR
        }
    }

    private fun remakeLayout() {
        if (mLayouts.isNotEmpty() && width > 0) {
            for (i in mLayouts.indices) {
                mLayouts[i].replaceOrMake(
                    mValues[i], mTextPaint, mItemWidth,
                    Layout.Alignment.ALIGN_CENTER, 1f, 1f, mBoringMetrics, false, mEllipsize,
                    mItemWidth
                )
            }
        }
    }

    private fun drawEdgeEffect(canvas: Canvas?, edgeEffect: EdgeEffect?, degrees: Int) {
        if (canvas == null || edgeEffect == null || degrees != 90 && degrees != 270) {
            return
        }
        if (!edgeEffect.isFinished) {
            val restoreCount = canvas.saveCount
            val width = width
            val height = height
            canvas.rotate(degrees.toFloat())
            if (degrees == 270) {
                canvas.translate(-height.toFloat(), max(0, scrollX).toFloat())
            } else { // 90
                canvas.translate(0f, -(max(getScrollRange().toFloat(), scaleX) + width))
            }
            edgeEffect.setSize(height, width)
            if (edgeEffect.draw(canvas)) {
                postInvalidate()
            }
            canvas.restoreToCount(restoreCount)
        }
    }

    /**
     * Calculates text color for specified item based on its position and state.
     *
     * @param item Index of item to get text color for
     * @return Item text color
     */
    private fun getTextColor(item: Int): Int {
        val scrollX = scrollX

        // set color of text
        var color = mTextColor!!.defaultColor
        val itemWithPadding = (mItemWidth + mDividerSize).toInt()
        if (scrollX > itemWithPadding * item - itemWithPadding / 2 &&
            scrollX < itemWithPadding * (item + 1) - itemWithPadding / 2
        ) {
            val position = scrollX - itemWithPadding / 2
            color = getColor(position, item)
        } else if (item == mPressedItem) {
            color = mTextColor!!.getColorForState(intArrayOf(android.R.attr.state_pressed), color)
        }
        return color
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateItemSize(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val currentMoveX = event.x
                var deltaMoveX = (mLastDownEventX - currentMoveX).toInt()
                if (mScrollingX ||
                    abs(deltaMoveX) > mTouchSlop && mValues.isNotEmpty()
                ) {
                    if (!mScrollingX) {
                        deltaMoveX = 0
                        mPressedItem = -1
                        mScrollingX = true
                        stopMarqueeIfNeeded()
                    }
                    val range: Int = getScrollRange()
                    if (overScrollBy(
                            deltaMoveX, 0, scrollX, 0, range, 0,
                            mOverscrollDistance, 0, true
                        )
                    ) {
                        mVelocityTracker!!.clear()
                    }
                    val pulledToX = (scrollX + deltaMoveX).toFloat()
                    if (pulledToX < 0) {
                        mLeftEdgeEffect!!.onPull(deltaMoveX.toFloat() / width)
                        if (!mRightEdgeEffect!!.isFinished) {
                            mRightEdgeEffect!!.onRelease()
                        }
                    } else if (pulledToX > range) {
                        mRightEdgeEffect!!.onPull(deltaMoveX.toFloat() / width)
                        if (!mLeftEdgeEffect!!.isFinished) {
                            mLeftEdgeEffect!!.onRelease()
                        }
                    }
                    mLastDownEventX = currentMoveX
                    invalidate()
                }
            }
            MotionEvent.ACTION_DOWN -> {
                if (!mAdjustScrollerX!!.isFinished) {
                    mAdjustScrollerX!!.forceFinished(true)
                } else if (!mFlingScrollerX!!.isFinished) {
                    mFlingScrollerX!!.forceFinished(true)
                } else {
                    mScrollingX = false
                }
                mLastDownEventX = event.x
                if (!mScrollingX) {
                    mPressedItem = getPositionFromTouch(event.x)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val velocityTracker = mVelocityTracker!!
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity.toFloat())
                val initialVelocityX = velocityTracker.xVelocity.toInt()
                if (mScrollingX && abs(initialVelocityX) > mMinimumFlingVelocity) {
                    flingX(initialVelocityX)
                } else {
                    val positionX = event.x
                    if (!mScrollingX) {
                        val itemPos: Int = getPositionOnScreen(positionX)
                        val relativePos = itemPos - mSideItems
                        if (relativePos == 0) {
                            selectItem()
                        } else {
                            smoothScrollBy(relativePos)
                        }
                    } else if (mScrollingX) {
                        finishScrolling()
                    }
                }
                mVelocityTracker!!.recycle()
                mVelocityTracker = null
                if (mLeftEdgeEffect != null) {
                    mLeftEdgeEffect!!.onRelease()
                    mRightEdgeEffect!!.onRelease()
                }
                mPressedItem = -1
                invalidate()
                if (mLeftEdgeEffect != null) {
                    mLeftEdgeEffect!!.onRelease()
                    mRightEdgeEffect!!.onRelease()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                mPressedItem = -1
                invalidate()
                if (mLeftEdgeEffect != null) {
                    mLeftEdgeEffect!!.onRelease()
                    mRightEdgeEffect!!.onRelease()
                }
            }
        }
        return true
    }

    private fun selectItem() {
        // post to the UI Thread to avoid potential interference with the OpenGL Thread
        if (mOnItemClicked != null) {
            post { mOnItemClicked!!.onItemClicked(getSelectedItemPosition()) }
        }
        adjustToNearestItemX()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (!isEnabled) {
            super.onKeyDown(keyCode, event)
        } else when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                selectItem()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                smoothScrollBy(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                smoothScrollBy(1)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun dispatchHoverEvent(event: MotionEvent?): Boolean {
        return if (mTouchHelper!!.dispatchHoverEvent(event!!)) {
            true
        } else super.dispatchHoverEvent(event)
    }

    override fun computeScroll() {
        computeScrollX()
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        val position = getSelectedItemPosition()
        if (direction < 0) { // left
            return position > 0
        } else if (direction > 0) { // right
            return position < mValues.size - 1
        }
        return super.canScrollHorizontally(direction)
    }

    override fun getFocusedRect(r: Rect?) {
        super.getFocusedRect(r) // TODO this should only be current item
    }

    fun setOnItemSelectedListener(onItemSelected: OnItemSelected) {
        mOnItemSelected = onItemSelected
    }

    fun setOnItemClickedListener(onItemClicked: OnItemClicked) {
        mOnItemClicked = onItemClicked
    }

    fun getSelectedItem(): CharSequence? {
        val position = getSelectedItemPosition()
        return if (position > -1 && position < mValues.size) {
            mValues[position]
        } else null
    }

    fun getSelectedItemPosition(): Int {
        val x = scrollX
        return getPositionFromCoordinates(x)
    }

    fun setSelectedItem(index: Int) {
        mSelectedItem = index
        scrollToItem(index)
    }

    fun getMarqueeRepeatLimit(): Int {
        return mMarqueeRepeatLimit
    }

    fun setMarqueeRepeatLimit(marqueeRepeatLimit: Int) {
        mMarqueeRepeatLimit = marqueeRepeatLimit
    }

    /**
     * @return Number of items on each side of current item.
     */
    fun getSideItems(): Int {
        return mSideItems
    }

    fun setSideItems(sideItems: Int) {
        require(mSideItems >= 0) { "Number of items on each side must be grater or equal to 0." }
        if (mSideItems != sideItems) {
            mSideItems = sideItems
            calculateItemSize(width, height)
        }
    }

    /**
     * @return
     */
    fun getValues(): Array<CharSequence> {
        return mValues
    }

    /**
     * Sets values to choose from
     * @param values New values to choose from
     */
    fun setValues(values: Array<CharSequence>) {
        if (mValues !== values) {
            mValues = values
            if (mValues.isNotEmpty()) {
                mLayouts = arrayOf()
                for (i in mLayouts.indices) {
                    mLayouts[i] = BoringLayout(
                        mValues[i], mTextPaint, mItemWidth, Layout.Alignment.ALIGN_CENTER,
                        1f, 1f, mBoringMetrics, false, mEllipsize, mItemWidth
                    )
                }
            } else {
                mLayouts = arrayOf()
            }

            // start marque only if has already been measured
            if (width > 0) {
                startMarqueeIfNeeded()
            }
            requestLayout()
            invalidate()
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss = state
        super.onRestoreInstanceState(ss.superState)
        setSelectedItem(ss.mSelItem)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.mSelItem = mSelectedItem
        return savedState
    }

    override fun setOverScrollMode(overScrollMode: Int) {
        if (overScrollMode != OVER_SCROLL_NEVER) {
            val context = context
            mLeftEdgeEffect = EdgeEffect(context)
            mRightEdgeEffect = EdgeEffect(context)
        } else {
            mLeftEdgeEffect = null
            mRightEdgeEffect = null
        }
        super.setOverScrollMode(overScrollMode)
    }

    fun getEllipsize(): TruncateAt? {
        return mEllipsize
    }

    fun setEllipsize(ellipsize: TruncateAt) {
        if (mEllipsize != ellipsize) {
            mEllipsize = ellipsize
            remakeLayout()
            invalidate()
        }
    }

    fun movePrevious() {
        smoothScrollBy(-1)
    }

    fun moveNext() {
        smoothScrollBy(1)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.scrollTo(scrollX, scrollY)
        if (!mFlingScrollerX!!.isFinished && clampedX) {
            mFlingScrollerX!!.springBack(scrollX, scrollY, 0, getScrollRange(), 0, 0)
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged() //TODO
    }

    private fun getPositionFromTouch(x: Float): Int {
        return getPositionFromCoordinates((scrollX - (mItemWidth + mDividerSize) * (mSideItems + .5f) + x).toInt())
    }

    private fun computeScrollX() {
        var scroller = mFlingScrollerX!!
        if (scroller.isFinished) {
            scroller = mAdjustScrollerX!!
            if (scroller.isFinished) {
                return
            }
        }
        if (scroller.computeScrollOffset()) {
            val currentScrollerX = scroller.currX
            if (mPreviousScrollerX == Int.MIN_VALUE) {
                mPreviousScrollerX = scroller.startX
            }
            val range: Int = getScrollRange()
            if (mPreviousScrollerX >= 0 && currentScrollerX < 0) {
                mLeftEdgeEffect!!.onAbsorb(scroller.currVelocity.toInt())
            } else if (range in mPreviousScrollerX until currentScrollerX) {
                mRightEdgeEffect!!.onAbsorb(scroller.currVelocity.toInt())
            }
            overScrollBy(
                currentScrollerX - mPreviousScrollerX, 0, mPreviousScrollerX, scrollY,
                getScrollRange(), 0, mOverscrollDistance, 0, false
            )
            mPreviousScrollerX = currentScrollerX
            if (scroller.isFinished) {
                onScrollerFinishedX(scroller)
            }
            postInvalidate()
            //            postInvalidateOnAnimation(); // TODO
        }
    }

    private fun flingX(velocityX: Int) {
        mPreviousScrollerX = Int.MIN_VALUE
        mFlingScrollerX!!.fling(
            scrollX, scrollY, -velocityX, 0, 0,
            (mItemWidth + mDividerSize).toInt() * (mValues.size - 1), 0, 0, width / 2, 0
        )
        invalidate()
    }

    private fun adjustToNearestItemX() {
        val x = scrollX
        var item = round(x / (mItemWidth + mDividerSize * 1f)).toInt()
        if (item < 0) {
            item = 0
        } else if (item > mValues.size) {
            item = mValues.size
        }
        mSelectedItem = item
        val itemX = (mItemWidth + mDividerSize.toInt()) * item
        val deltaX = itemX - x
        mAdjustScrollerX!!.startScroll(x, 0, deltaX, 0, SELECTOR_ADJUSTMENT_DURATION_MILLIS)
        invalidate()
    }

    private fun calculateItemSize(w: Int, h: Int) {
        val items = mSideItems * 2 + 1
        val totalPadding = mDividerSize.toInt() * (items - 1)
        mItemWidth = (w - totalPadding) / items
        mItemClipBounds = RectF(0f, 0f, mItemWidth.toFloat(), h.toFloat())
        mItemClipBoundsOffser = RectF(mItemClipBounds)
        scrollToItem(mSelectedItem)
        remakeLayout()
        startMarqueeIfNeeded()
    }

    private fun onScrollerFinishedX(scroller: OverScroller) {
        if (scroller === mFlingScrollerX) {
            finishScrolling()
        }
    }

    private fun finishScrolling() {
        adjustToNearestItemX()
        mScrollingX = false
        startMarqueeIfNeeded()
        // post to the UI Thread to avoid potential interference with the OpenGL Thread
        if (mOnItemSelected != null) {
            post { mOnItemSelected!!.onItemSelected(getPositionFromCoordinates(scrollX)) }
        }
    }

    private fun startMarqueeIfNeeded() {
        stopMarqueeIfNeeded()
        val item = getSelectedItemPosition()
        if (mLayouts.size > item) {
            val layout: Layout = mLayouts[item]
            if (mEllipsize == TruncateAt.MARQUEE
                && mItemWidth < layout.getLineWidth(0)
            ) {
                mMarquee = Marquee(this, layout, isRtl(mValues[item]))
                mMarquee!!.start(mMarqueeRepeatLimit)
            }
        }
    }

    private fun stopMarqueeIfNeeded() {
        if (mMarquee != null) {
            mMarquee!!.stop()
            mMarquee = null
        }
    }

    private fun getPositionOnScreen(x: Float): Int {
        return (x / (mItemWidth + mDividerSize)).toInt()
    }

    private fun smoothScrollBy(i: Int) {
        var deltaMoveX = (mItemWidth + mDividerSize.toInt()) * i
        deltaMoveX = getRelativeInBound(deltaMoveX)
        mFlingScrollerX!!.startScroll(scrollX, 0, deltaMoveX, 0)
        stopMarqueeIfNeeded()
        invalidate()
    }

    /**
     * Calculates color for specific position on time picker
     * @param scrollX
     * @return
     */
    private fun getColor(scrollX: Int, position: Int): Int {
        val itemWithPadding = (mItemWidth + mDividerSize).toInt()
        var proportion = abs(1f * scrollX % itemWithPadding / 2 / (itemWithPadding / 2f))
        proportion = if (proportion > .5) {
            proportion - .5f
        } else {
            .5f - proportion
        }
        proportion *= 2f
        val defaultColor: Int
        val selectedColor: Int
        if (mPressedItem == position) {
            defaultColor = mTextColor!!.getColorForState(
                intArrayOf(android.R.attr.state_pressed),
                mTextColor!!.defaultColor
            )
            selectedColor = mTextColor!!.getColorForState(
                intArrayOf(
                    android.R.attr.state_pressed,
                    android.R.attr.state_selected
                ), defaultColor
            )
        } else {
            defaultColor = mTextColor!!.defaultColor
            selectedColor = mTextColor!!.getColorForState(
                intArrayOf(android.R.attr.state_selected),
                defaultColor
            )
        }
        return (ArgbEvaluator().evaluate(proportion, selectedColor, defaultColor) as Int)
    }

    /**
     * Sets text size for items
     * @param size New item text size in px.
     */
    private fun setTextSize(size: Float) {
        if (size != mTextPaint!!.textSize) {
            mTextPaint!!.textSize = size
            requestLayout()
            invalidate()
        }
    }

    /**
     * Calculates item from x coordinate position.
     * @param x Scroll position to calculate.
     * @return Selected item from scrolling position in {param x}
     */
    private fun getPositionFromCoordinates(x: Int): Int {
        return round(x / (mItemWidth + mDividerSize)).toInt()
    }

    /**
     * Scrolls to specified item.
     * @param index Index of an item to scroll to
     */
    private fun scrollToItem(index: Int) {
        scrollTo((mItemWidth + mDividerSize.toInt()) * index, 0)
        // invalidate() not needed because scrollTo() already invalidates the view
    }

    /**
     * Calculates relative horizontal scroll position to be within our scroll bounds.
     * [com.wefika.horizontalpicker.HorizontalPicker.getInBoundsX]
     * @param x Relative scroll position to calculate
     * @return Current scroll position + {param x} if is within our scroll bounds, otherwise it
     * will return min/max scroll position.
     */
    private fun getRelativeInBound(x: Int): Int {
        val scrollX = scrollX
        return getInBoundsX(scrollX + x) - scrollX
    }

    /**
     * Calculates x scroll position that is still in range of view scroller
     * @param x Scroll position to calculate.
     * @return {param x} if is within bounds of over scroller, otherwise it will return min/max
     * value of scoll position.
     */
    private fun getInBoundsX(x: Int): Int {
        return if (x < 0) {
            0
        } else if (x > (mItemWidth + mDividerSize.toInt()) * (mValues.size - 1)) {
            (mItemWidth + mDividerSize.toInt()) * (mValues.size - 1)
        } else {
            0
        }
    }

    private fun getScrollRange(): Int {
        var scrollRange = 0
        if (mValues.isNotEmpty()) {
            scrollRange = max(
                0,
                (mItemWidth + mDividerSize.toInt()) * (mValues.size - 1)
            )
        }
        return scrollRange
    }

    companion object {
        /**
         * The coefficient by which to adjust (divide) the max fling velocity.
         */
        private const val SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 4

        /**
         * The the duration for adjusting the selector wheel.
         */
        private const val SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800
    }
}