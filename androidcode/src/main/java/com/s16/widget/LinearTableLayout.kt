package com.s16.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow


class LinearTableLayout : LinearLayout {

    private val mBorderPaint = Paint().apply {
        color = BORDER_COLOR
        style = Paint.Style.FILL
    }
    private var mHeader: TableLayout? = null
    private var mBody: TableLayout? = null
    private var mScrollView: ViewGroup? = null
    private var mColWidths: MutableList<Int> = mutableListOf()

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (childCount >= 2) {
            if (mHeader == null) {
                mHeader = getChildAt(0) as TableLayout
            }
            if (mBody == null) {
                mScrollView = getChildAt(1) as ViewGroup
                if (mScrollView != null) {
                    mBody = mScrollView!!.getChildAt(0) as TableLayout
                }
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (mHeader != null && mBody != null && mScrollView != null) {
            mColWidths = mutableListOf()
            if (mBody!!.childCount > 0) {
                for (rownum in 0..0) {
                    val row: TableRow = mBody!!.getChildAt(rownum) as TableRow
                    for (cellnum in 0 until row.childCount) {
                        val cell: View = row.getChildAt(cellnum)
                        val params: TableRow.LayoutParams =
                            cell.layoutParams as TableRow.LayoutParams
                        val cellWidth = if (params.span == 1) cell.width else 0
                        if (mColWidths.size <= cellnum) {
                            mColWidths.add(cellWidth)
                        } else {
                            val current = mColWidths[cellnum]
                            if (cellWidth > current) {
                                mColWidths.removeAt(cellnum)
                                mColWidths.add(cellnum, cellWidth)
                            }
                        }
                    }
                }
                for (rownum in 0 until mHeader!!.childCount) {
                    val row: TableRow = mHeader!!.getChildAt(rownum) as TableRow
                    for (cellnum in 0 until row.childCount) {
                        val cell: View = row.getChildAt(cellnum)
                        val params: TableRow.LayoutParams =
                            cell.layoutParams as TableRow.LayoutParams
                        params.width = 0
                        for (span in 0 until params.span) {
                            params.width += mColWidths[cellnum + span]
                        }
                    }
                }
                mHeader!!.requestLayout()
            } else if (mHeader!!.childCount > 0) {
                for (rownum in 0..0) {
                    val row: TableRow = mHeader!!.getChildAt(rownum) as TableRow
                    for (cellnum in 0 until row.childCount) {
                        val cell: View = row.getChildAt(cellnum)
                        val params: TableRow.LayoutParams =
                            cell.layoutParams as TableRow.LayoutParams
                        val cellWidth = if (params.span == 1) cell.width else 0
                        if (mColWidths.size <= cellnum) {
                            mColWidths.add(cellWidth)
                        } else {
                            val current = mColWidths[cellnum]
                            if (cellWidth > current) {
                                mColWidths.removeAt(cellnum)
                                mColWidths.add(cellnum, cellWidth)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mHeader != null && mBody != null && mScrollView != null) {
            if (mHeader!!.childCount > 0) {
                val x = mHeader!!.x
                val y = mHeader!!.y
                // Draw Header Border
                canvas.drawLine(x, y, mHeader!!.right.toFloat(), y, mBorderPaint)
                canvas.drawLine(x, y, x, mHeader!!.bottom.toFloat(), mBorderPaint)
                canvas.drawLine(mHeader!!.right.toFloat(), y, mHeader!!.right.toFloat(), mHeader!!.bottom.toFloat(), mBorderPaint)
                canvas.drawLine(
                    x,
                    mHeader!!.bottom.toFloat(),
                    mHeader!!.right.toFloat(),
                    mHeader!!.bottom.toFloat(),
                    mBorderPaint
                )
                var left = x
                for (i in 0 until mColWidths.size - 1) {
                    left += mColWidths[i].toFloat()
                    canvas.drawLine(left, y, left, mHeader!!.bottom.toFloat(), mBorderPaint)
                }

                // Draw Body Border
                val childCount = mBody!!.childCount
                if (childCount > 0) {
                    val scrollY = mScrollView!!.scrollY.toFloat()
                    val by = mScrollView!!.y
                    for (i in 0 until childCount) {
                        val row = mBody!!.getChildAt(i)
                        val dy = row.bottom + by - scrollY
                        if (dy > by && dy < bottom) {
                            canvas.drawLine(x, dy, mBody!!.right.toFloat(), dy, mBorderPaint)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val BORDER_COLOR = -0x7f7f80
    }
}