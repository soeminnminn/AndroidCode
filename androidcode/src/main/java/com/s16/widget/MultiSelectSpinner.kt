package com.s16.widget

import android.content.Context
import android.content.DialogInterface
import android.database.DataSetObserver
import android.util.AttributeSet
import android.view.View
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import java.util.*
import kotlin.collections.ArrayList


class MultiSelectSpinner : AppCompatSpinner, DialogInterface.OnMultiChoiceClickListener {

    private class ItemData() {
        var id: Long = 0
        var checked = false
        var value: Any? = null

        constructor(id: Long, value: Any?, checked: Boolean): this() {
            this.id = id
            this.value = value
            this.checked = checked
        }

        override fun toString(): String {
            return if (value != null) {
                value.toString()
            } else ""
        }

        override fun hashCode(): Int {
            return if (value != null) {
                value.hashCode()
            } else super.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return if (value != null) {
                value == other
            } else super.equals(other)
        }
    }

    private val mDataSetObserver: DataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            rebuildItems()
        }
    }

    private val mAcceptClickListener =
        DialogInterface.OnClickListener { dialog, which -> updateSelected() }

    private var mDialog: AlertDialog? = null
    private var mItems: Array<ItemData>? = null
    private var mCheckedItems: BooleanArray? = null
    private var mSelectedIndices: IntArray? = null
    private var mSelectedItems: Array<Any>? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }

    private fun init(context: Context?) {}

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateSelected()
    }

    override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
        mCheckedItems!![which] = isChecked
    }

    override fun setAdapter(adapter: SpinnerAdapter?) {
        if (super.getAdapter() != null) {
            super.getAdapter().unregisterDataSetObserver(mDataSetObserver)
        }
        super.setAdapter(adapter)
        rebuildItems()
        adapter?.registerDataSetObserver(mDataSetObserver)
    }

    override fun setPrompt(prompt: CharSequence?) {
        super.setPrompt(prompt)
        if (childCount > 0) {
            val child: View? = getChildAt(0)
            if (child != null && child is TextView) {
                val textView = child as TextView
                textView.hint = prompt
            }
        }
    }

    override fun performClick(): Boolean {
        val adapter = adapter
        if (adapter == null || adapter.count == 0) {
            return super.performClick()
        }
        showDialog()
        return true
    }

    override fun onDetachedFromWindow() {
        val adapter = adapter
        adapter?.unregisterDataSetObserver(mDataSetObserver)
        super.onDetachedFromWindow()
        if (mDialog != null && mDialog!!.isShowing) {
            mDialog!!.dismiss()
        }
    }

    private fun updateSelected() {
        val builder = StringBuilder()
        val selectedIndices: MutableList<Int> = ArrayList()
        for (i in 0 until mCheckedItems!!.size) {
            val checked = mCheckedItems!![i]
            mItems!![i].checked = checked
            if (checked) {
                if (builder.isNotEmpty()) {
                    builder.append(", ")
                }
                builder.append(mItems!![i])
                selectedIndices.add(Integer.valueOf(i))
            }
        }
        val selectedCount = selectedIndices.size
        if (selectedCount > 0) {
            mSelectedIndices = intArrayOf()
            mSelectedItems = arrayOf()
            for (i in 0 until selectedCount) {
                val index = selectedIndices[i]
                mSelectedIndices!![i] = index
                mSelectedItems!![i] = mItems!![index].value!!
            }
        } else {
            mSelectedIndices = null
            mSelectedItems = null
        }
        if (childCount > 0) {
            val child: View? = getChildAt(0)
            if (child != null && child is TextView) {
                val textView = child as TextView?
                textView!!.text = builder.toString()
                if (mSelectedIndices == null || mSelectedIndices!!.isEmpty()) {
                    val prompt = prompt
                    if (prompt != null) {
                        textView.hint = prompt
                    }
                }
            }
        }
    }

    private fun isChecked(item: Any?): Boolean {
        var result = false
        if (mItems != null) {
            for (j in 0 until mItems!!.size) {
                if (mItems!![j] == item) {
                    result = mItems!![j].checked
                    break
                }
            }
        }
        return result
    }

    private fun rebuildItems() {
        val adapter = adapter
        if (adapter == null) {
            mItems = null
            mCheckedItems = null
            return
        }
        val itemCount = adapter.count
        if (mCheckedItems == null || mCheckedItems!!.size != itemCount) {
            mCheckedItems = BooleanArray(itemCount)
        }
        if (mSelectedIndices != null) {
            for (i in 0 until mCheckedItems!!.size) {
                mCheckedItems!![i] = false
            }
            for (i in 0 until mSelectedIndices!!.size) {
                mCheckedItems!![mSelectedIndices!![i]] = true
            }
        }
        if (mSelectedItems != null) {
            for (i in 0 until mCheckedItems!!.size) {
                mCheckedItems!![i] = false
            }
        }
        mItems = arrayOf()
        for (i in 0 until itemCount) {
            val item = adapter.getItem(i)
            if (item != null) {
                var checked = if (mCheckedItems!!.size > i) mCheckedItems!![i] else false
                if (mSelectedItems != null) {
                    val checkedIndex: Int = Arrays.binarySearch(mSelectedItems, item)
                    checked = checkedIndex > -1
                }
                mItems!![i] = ItemData(adapter.getItemId(i), item, checked)
            } else {
                mItems!![i] = ItemData()
            }
        }
    }

    private fun showDialog() {
        if (mItems == null) {
            return
        }
        val builder = AlertDialog.Builder(
            context
        )
        val itemCount: Int = mItems!!.size
        val items = arrayOfNulls<CharSequence>(itemCount)
        for (i in 0 until itemCount) {
            items[i] = mItems!![i].toString()
        }
        val prompt = prompt
        if (prompt != null) {
            builder.setTitle(prompt)
        }
        builder.setMultiChoiceItems(items, mCheckedItems, this)
        builder.setPositiveButton(android.R.string.ok, mAcceptClickListener)
        builder.setNegativeButton(android.R.string.cancel, null)
        mDialog = builder.create()
        mDialog!!.show()
    }

    fun getSelectedIds(): LongArray? {
        if (mSelectedIndices != null) {
            val result = LongArray(mSelectedIndices!!.size)
            for (i in 0 until mSelectedIndices!!.size) {
                val index = mSelectedIndices!![i]
                result[i] = mItems!![index].id
            }
            return result
        }
        return null
    }

    fun setSelectedIndices(indices: IntArray) {
        mSelectedIndices = indices
        rebuildItems()
    }

    fun getSelectedIndices(): IntArray? {
        return mSelectedIndices
    }

    fun setSelectedItems(items: Array<Any>) {
        mSelectedItems = items
        rebuildItems()
    }

    fun getSelectedItems(): Array<Any>? {
        return mSelectedItems
    }
}