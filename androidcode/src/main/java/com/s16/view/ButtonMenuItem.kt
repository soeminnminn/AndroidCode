package com.s16.view

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView


class ButtonMenuItem(context: Context) : MenuItem {

    private var mView: View? = null
    private val mContext: Context = context

    constructor(view: View) : this(view.context) {
        mView = view
    }

    override fun getItemId(): Int {
        return mView?.id ?: 0
    }

    override fun getGroupId(): Int {
        return 0
    }

    override fun getOrder(): Int {
        return 0
    }

    override fun setTitle(title: CharSequence?): MenuItem {
        if (mView != null && mView is TextView) {
            (mView as TextView).text = title
        }
        return this
    }

    override fun setTitle(resid: Int): MenuItem {
        if (mView != null && mView is TextView) {
            (mView as TextView).setText(resid)
        }
        return this
    }

    override fun getTitle(): CharSequence? {
        return if (mView != null && mView is TextView) {
            (mView as TextView).text
        } else null
    }

    override fun setTitleCondensed(title: CharSequence?): MenuItem? {
        return null
    }

    override fun getTitleCondensed(): CharSequence? {
        return null
    }

    override fun setIcon(icon: Drawable?): MenuItem {
        if (mView != null) {
            if (mView is CompoundButton) {
                (mView as CompoundButton).buttonDrawable = icon
            } else if (mView is ImageView) {
                (mView as ImageView).setImageDrawable(icon)
            } else if (mView is TextView) {
                (mView as TextView).setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            }
        }
        return this
    }

    override fun setIcon(iconRes: Int): MenuItem {
        if (mView != null) {
            if (mView is CompoundButton) {
                (mView as CompoundButton).setButtonDrawable(iconRes)
            } else if (mView is ImageView) {
                (mView as ImageView).setImageResource(iconRes)
            } else if (mView is TextView) {
                (mView as TextView).setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
            }
        }
        return this
    }

    override fun getIcon(): Drawable? {
        if (mView != null) {
            if (mView is CompoundButton) {
                val compoundDrawables = (mView as CompoundButton).compoundDrawables
                return compoundDrawables[0]
            } else if (mView is ImageView) {
                return (mView as ImageView).drawable
            } else if (mView is TextView) {
                val compoundDrawables = (mView as TextView).compoundDrawables
                return compoundDrawables[0]
            }
        }
        return null
    }

    override fun setIntent(intent: Intent?): MenuItem {
        return this
    }

    override fun getIntent(): Intent? {
        return null
    }

    override fun setShortcut(numericChar: Char, alphaChar: Char): MenuItem {
        return this
    }

    override fun setNumericShortcut(numericChar: Char): MenuItem {
        return this
    }

    override fun getNumericShortcut(): Char {
        return '\u0000'
    }

    override fun setAlphabeticShortcut(alphaChar: Char): MenuItem {
        return this
    }

    override fun getAlphabeticShortcut(): Char {
        return '\u0000'
    }

    override fun setCheckable(checkable: Boolean): MenuItem {
        return this
    }

    override fun isCheckable(): Boolean {
        return mView != null && mView is Checkable
    }

    override fun setChecked(checked: Boolean): MenuItem {
        if (mView != null && mView is Checkable) {
            (mView as Checkable).isChecked = checked
        }
        return this
    }

    override fun isChecked(): Boolean {
        return if (mView != null && mView is Checkable) {
            (mView as Checkable).isChecked
        } else false
    }

    override fun setVisible(visible: Boolean): MenuItem {
        mView?.visibility = if (visible) View.VISIBLE else View.GONE
        return this
    }

    override fun isVisible(): Boolean {
        return mView?.visibility == View.VISIBLE ?: false
    }

    override fun hasSubMenu(): Boolean {
        return false
    }

    override fun getSubMenu(): SubMenu? {
        return null
    }

    override fun setOnMenuItemClickListener(menuItemClickListener: MenuItem.OnMenuItemClickListener?): MenuItem {
        if (menuItemClickListener != null) {
            mView?.setOnClickListener { menuItemClickListener.onMenuItemClick(this@ButtonMenuItem) }
        }
        return this
    }

    override fun getMenuInfo(): ContextMenuInfo? {
        return null
    }

    override fun setShowAsAction(actionEnum: Int) {}

    override fun setShowAsActionFlags(actionEnum: Int): MenuItem {
        return this
    }

    override fun setActionView(view: View): MenuItem {
        mView = view
        return this
    }

    override fun setActionView(resId: Int): MenuItem {
        val inflater = LayoutInflater.from(mContext)
        mView = inflater.inflate(resId, null, false)
        return this
    }

    override fun getActionView(): View? {
        return mView
    }

    override fun setActionProvider(actionProvider: ActionProvider?): MenuItem {
        return this
    }

    override fun getActionProvider(): ActionProvider? {
        return null
    }

    override fun expandActionView(): Boolean {
        return false
    }

    override fun collapseActionView(): Boolean {
        return false
    }

    override fun isActionViewExpanded(): Boolean {
        return false
    }

    override fun setOnActionExpandListener(listener: MenuItem.OnActionExpandListener?): MenuItem {
        return this
    }

    override fun setEnabled(enabled: Boolean): MenuItem {
        mView?.isEnabled = enabled
        return this
    }

    override fun isEnabled(): Boolean {
        return mView?.isEnabled ?: false
    }
}