package com.s16.view

import android.database.DataSetObservable
import android.database.DataSetObserver
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.SpinnerAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.*

abstract class HolderListAdapter<VH: RecyclerView.ViewHolder>: ListAdapter, SpinnerAdapter {

    companion object {
        const val NO_POSITION: Int = -1
        const val NO_ID: Long = -1
        const val INVALID_TYPE: Int = -1
    }

    private var mHolderList: Map<View, VH> = mutableMapOf()
    private var mHasStableIds: Boolean = false
    private var mDataSetObservable = DataSetObservable()

    final override fun getCount(): Int = getItemCount()

    final override fun hasStableIds(): Boolean = mHasStableIds

    override fun getItem(position: Int): Any = position

    override fun getItemId(position: Int): Long = NO_ID

    fun setHasStableIds(hasStableIds: Boolean) {
        mHasStableIds = hasStableIds
    }

    final override fun areAllItemsEnabled(): Boolean = true

    final override fun isEnabled(position: Int): Boolean = true

    final override fun getAutofillOptions(): Array<CharSequence>? = null

    override fun getItemViewType(position: Int): Int = 0

    override fun getViewTypeCount(): Int = 1

    fun hasObservers(): Boolean = false

    final override fun registerDataSetObserver(observer: DataSetObserver) {
        mDataSetObservable.registerObserver(observer)
    }

    final override fun unregisterDataSetObserver(observer: DataSetObserver) {
        mDataSetObservable.unregisterObserver(observer)
    }

    fun registerAdapterDataObserver(observer: DataSetObserver) {
        registerDataSetObserver(observer)
    }

    fun unregisterAdapterDataObserver(observer: DataSetObserver) {
        unregisterDataSetObserver(observer)
    }

    fun notifyDataSetChanged() {
        mDataSetObservable.notifyChanged()
    }

    fun notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated()
    }

    fun notifyItemChanged(position: Int) {
        notifyDataSetChanged()
    }

    fun notifyItemChanged(position: Int, payload: Objects) {
        notifyItemChanged(position)
    }

    fun notifyItemRangeChanged(positionStart: Int, itemCount: Int) {
        notifyDataSetChanged()
    }

    fun notifyItemRangeChanged(positionStart: Int, itemCount: Int, payload: Objects) {
        notifyItemRangeChanged(positionStart, itemCount)
    }

    fun notifyItemInserted(position: Int) {
        notifyDataSetChanged()
    }

    fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
        notifyDataSetChanged()
    }

    fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
        notifyDataSetChanged()
    }

    fun notifyItemRemoved(position: Int) {
        notifyDataSetChanged()
    }

    fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int) {
        notifyDataSetChanged()
    }

    final override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder = if (convertView == null) {
            val h = onCreateViewHolder(parent!!, 0)
            mHolderList + Pair(h.itemView, h)
            h
        } else {
            mHolderList[convertView]
        }

        onBindViewHolder(holder!!, position)
        return holder.itemView
    }

    final override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent)
    }

    abstract fun getItemCount(): Int

    abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

    abstract fun onBindViewHolder(holder: VH, position: Int)

    open fun onBindViewHolder(holder: VH, position: Int, payloads: List<Objects>) {
        onBindViewHolder(holder, position)
    }

    open fun onViewRecycled(holder: VH) {}

    open fun onFailedToRecycleView(holder: VH): Boolean = false

    open fun onViewAttachedToWindow(holder: VH) {}

    open fun onViewDetachedFromWindow(holder: VH) {}

    open fun onAttachedToRecyclerView(recyclerView: RecyclerView) {}

    open fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {}
}