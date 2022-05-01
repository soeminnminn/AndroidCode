package com.s16.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class CollageLayout : FrameLayout {

    constructor(context: Context)
            : super(context) { }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {}

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount <= 1 || childCount > 3) {
            throw IllegalArgumentException("CollageLayout must contain three child views!")
        }
    }

    var orientation : Int = VERTICAL
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val initialWidth = MeasureSpec.getSize(widthMeasureSpec)
        var initialHeight = MeasureSpec.getSize(heightMeasureSpec)
        var firstChild : View? = null
        var secondChild : View? = null
        var thirdChild : View? = null

        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == View.GONE) {
                continue
            }
            when {
                firstChild == null -> firstChild = c
                secondChild == null -> secondChild = c
                thirdChild == null -> thirdChild = c
            }
        }

        if (orientation == VERTICAL) {
            val childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(
                widthMeasureSpec,
                paddingLeft + paddingRight,
                LayoutParams.MATCH_PARENT
            )
            var childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(
                heightMeasureSpec,
                paddingTop + paddingBottom,
                LayoutParams.WRAP_CONTENT
            )

            if (firstChild != null) {
                firstChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                initialHeight = firstChild.measuredHeight
            }

            if (secondChild != null && thirdChild != null) {
                val childSpec = MeasureSpec.makeMeasureSpec(initialWidth / 2, MeasureSpec.EXACTLY)
                childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(
                    childSpec,
                    paddingTop + paddingBottom,
                    LayoutParams.MATCH_PARENT
                )

                secondChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                thirdChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                initialHeight += secondChild.measuredHeight
            } else if (secondChild != null) {
                secondChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                initialHeight += secondChild.measuredHeight
            }

        } else {
            val childSpec = MeasureSpec.makeMeasureSpec(initialWidth / 2, MeasureSpec.EXACTLY)
            var childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(
                childSpec,
                paddingLeft + paddingRight,
                LayoutParams.MATCH_PARENT
            )
            var childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(
                heightMeasureSpec,
                paddingTop + paddingBottom,
                LayoutParams.WRAP_CONTENT
            )

            if (firstChild != null && secondChild != null && thirdChild != null) {
                firstChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(
                    childSpec,
                    paddingTop + paddingBottom,
                    LayoutParams.MATCH_PARENT
                )

                secondChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                thirdChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                initialHeight = secondChild.measuredHeight + thirdChild.measuredHeight

            } else if (firstChild != null && secondChild != null) {
                firstChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                secondChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                initialHeight = firstChild.measuredHeight

            } else if (firstChild != null) {
                childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(
                    widthMeasureSpec,
                    paddingLeft + paddingRight,
                    LayoutParams.MATCH_PARENT
                )

                firstChild.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                initialHeight = firstChild.measuredHeight
            }
        }

        initialHeight += paddingTop + paddingBottom
        val heightSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)

        super.onMeasure(widthMeasureSpec, heightSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (orientation == VERTICAL && layoutVertical(l, t, r, b)) {
            return
        } else if (layoutHorizontal(l, t, r, b)) {
            return
        }
        super.onLayout(changed, l, t, r, b)
    }

    private fun layoutVertical(left: Int, top: Int, right: Int, bottom: Int) : Boolean {
        var initialWidth = right - left
        var initialHeight = bottom - top

        var visibleViewsCount = 0
        var firstChild : View? = null
        var secondChild : View? = null
        var thirdChild : View? = null

        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == View.GONE) {
                continue
            }
            visibleViewsCount += 1
            when (visibleViewsCount) {
                1 -> { firstChild = c }
                2 -> { secondChild = c }
                3 -> { thirdChild = c }
            }
        }

        if (firstChild != null) {
            val horizPadding = paddingLeft + paddingRight
            val vertPadding = paddingTop + paddingBottom
            initialWidth -= horizPadding
            initialHeight -= vertPadding
            val halfWidth = initialWidth / 2
            val firstChildWidth = initialWidth
            val firstChildHeight = firstChild.measuredHeight

            if (secondChild != null && thirdChild != null) {
                secondChild.layout(paddingLeft, paddingTop + firstChildHeight,
                    paddingLeft + halfWidth, paddingTop + firstChildHeight + halfWidth)

                thirdChild.layout(paddingLeft + halfWidth, paddingTop + firstChildHeight,
                    paddingLeft + halfWidth + halfWidth, paddingTop + firstChildHeight + halfWidth)

            } else if (secondChild != null) {
                val secondChildHeight = secondChild.measuredHeight
                secondChild.layout(paddingLeft, paddingTop + firstChildHeight,
                    paddingLeft + initialWidth, paddingTop + firstChildHeight + secondChildHeight)
            }

            firstChild.layout(paddingLeft, paddingTop, paddingLeft + firstChildWidth, paddingTop + firstChildHeight)
        }

        return visibleViewsCount > 1
    }

    private fun layoutHorizontal(left: Int, top: Int, right: Int, bottom: Int) : Boolean {
        var initialWidth = right - left
        var initialHeight = bottom - top

        var visibleViewsCount = 0
        var firstChild : View? = null
        var secondChild : View? = null
        var thirdChild : View? = null

        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == View.GONE) {
                continue
            }
            visibleViewsCount += 1
            when (visibleViewsCount) {
                1 -> { firstChild = c }
                2 -> { secondChild = c }
                3 -> { thirdChild = c }
            }
        }

        if (firstChild != null) {
            val horizPadding = paddingLeft + paddingRight
            val vertPadding = paddingTop + paddingBottom
            initialWidth -= horizPadding
            initialHeight -= vertPadding
            val halfWidth = initialWidth / 2
            var firstChildWidth = initialWidth
            var firstChildHeight = firstChild.measuredHeight

            if (secondChild != null && thirdChild != null) {
                firstChildWidth = halfWidth
                firstChildHeight = halfWidth * 2

                secondChild.layout(paddingLeft + firstChildWidth, paddingTop,
                    paddingLeft + firstChildWidth + halfWidth, paddingTop + halfWidth)

                thirdChild.layout(paddingLeft + firstChildWidth, paddingTop + halfWidth,
                    paddingLeft + firstChildWidth + halfWidth, paddingTop + initialWidth)

            } else if (secondChild != null) {
                firstChildWidth = halfWidth

                secondChild.layout(paddingLeft + firstChildWidth, paddingTop,
                    paddingLeft + firstChildWidth + halfWidth, paddingTop + firstChildHeight)
            }

            firstChild.layout(paddingLeft, paddingTop, paddingLeft + firstChildWidth, paddingTop + firstChildHeight)
        }

        return visibleViewsCount > 1
    }

    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }
}