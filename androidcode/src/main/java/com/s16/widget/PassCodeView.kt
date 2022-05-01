package com.s16.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.s16.android.R
import java.security.InvalidParameterException
import kotlin.math.min


class PassCodeView : AppCompatEditText {

    interface OnCompleteListener {
        fun onComplete(view: View?, s: Editable?)
    }

    private var mPinCount = DEFAULT_PIN_COUNT
    private var mDividerWidth = 0f
    private var mCodeDrawableId = 0

    private var mBackgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = DEFAULT_BACKGROUND_COLOR
    }

    private var mBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = DEFAULT_BORDER_COLOR
    }

    private var mCharPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = DEFAULT_CHAR_COLOR
    }

    private var mOnCompleteListener: OnCompleteListener? = null

    private val mTextWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (s.length == mPinCount) {
                mOnCompleteListener?.onComplete(this@PassCodeView, s)
            }
        }
    }

    constructor(context: Context) : super(context) {
        init(context, null, R.attr.passCodeViewStyle)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, R.attr.passCodeViewStyle)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs, defStyle)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PassCodeView,
            defStyle, 0
        )
        mPinCount = a.getInt(R.styleable.PassCodeView_pcvPinCount, DEFAULT_PIN_COUNT)
        mDividerWidth = a.getDimension(R.styleable.PassCodeView_pcvDividerSize, 0f)
        mCodeDrawableId = a.getResourceId(R.styleable.PassCodeView_pcvCodeBackground, 0)
        a.recycle()

        background = null
        mDividerWidth = getDimensionDip(DEFAULT_DIVIDER_WIDTH).toFloat()
        updateFilter()
        addTextChangedListener(mTextWatcher)
    }

    private fun getDimensionDip(value: Int): Int {
        val dm = resources.displayMetrics
        var result =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), dm).toInt()
        if (result == 0) {
            result = (value * dm.density).toInt()
        }
        return result
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isInEditMode) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        if (isInEditMode) {
            super.onDraw(canvas)
            return
        }
        //super.onDraw(canvas);
        canvas?.let {
            drawPasscode(it)
        }
    }

    private fun drawPasscode(canvas: Canvas) {
        val bounds = RectF(canvas.clipBounds)
        if (mPinCount < 1) return
        if (bounds.width() == 0f || bounds.height() == 0f) return
        val height = bounds.height()
        if (height < 10) return
        val width = height + mDividerWidth
        val totalWidth = width * mPinCount - mDividerWidth
        var x = bounds.left + (bounds.width() - totalWidth) * 0.5f
        val size = min(width, height)
        val charCount = if (text != null) text!!.length else 0
        var rect: Rect? = null

        for (i in 0 until mPinCount) {
            val saveCount = canvas.save()
            rect = Rect(0, 0, size.toInt(), bounds.height().toInt())
            canvas.translate(x, bounds.top)
            if (mCodeDrawableId != 0) {
                val codeBackground = ContextCompat.getDrawable(context, mCodeDrawableId)
                if (codeBackground != null) {
                    codeBackground.bounds = rect
                    codeBackground.state = drawableState
                    codeBackground.draw(canvas)
                }
            } else {
                canvas.drawRect(rect, mBackgroundPaint)
                canvas.drawRect(rect, mBorderPaint)
            }
            if (charCount > i) {
                val textSize = textSize / 2
                val radius = if (textSize > 0) textSize else size / 8
                mCharPaint.color = currentTextColor
                canvas.drawCircle(rect.centerX().toFloat(), rect.centerY().toFloat(), radius, mCharPaint)
            }
            canvas.restoreToCount(saveCount)
            x += width
        }
    }

    private fun updateFilter() {
        filters = arrayOf<InputFilter>(LengthFilter(mPinCount))
    }

    fun isPassCodeComplete(): Boolean {
        val text: CharSequence? = text
        return text != null && text.length == mPinCount
    }

    fun setPinCount(count: Int) {
        if (count < 1) {
            throw InvalidParameterException()
        }
        if (mPinCount != count) {
            mPinCount = count
            updateFilter()
            invalidate()
        }
    }

    fun setOnCompleteListener(listener: OnCompleteListener) {
        mOnCompleteListener = listener
    }

    companion object {
        private const val DEFAULT_BACKGROUND_COLOR = -0x1
        private const val DEFAULT_BORDER_COLOR = -0x1000000
        private const val DEFAULT_CHAR_COLOR = -0x1000000

        private const val DEFAULT_PIN_COUNT = 4
        private const val DEFAULT_DIVIDER_WIDTH = 10
    }
}