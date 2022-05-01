package com.s16.widget

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.s16.android.R

class Backdrop(context: Context,
               attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {

    private var toolbar: Toolbar? = null
    private var openIcon: Drawable? = ResourcesCompat.getDrawable(resources, R.drawable.ic_menu, null)
    private var closeIcon: Drawable? = ResourcesCompat.getDrawable(resources, R.drawable.ic_close, null)
    private var backdropSize: Int = 0
    private var animationDuration: Long = 200L

    private var backdropShown = false
    private val interpolator: Interpolator = LinearInterpolator()
    private val animatorSet = AnimatorSet()

    private var mcToolbarId: Int
    private lateinit var navIconClickListener: View.OnClickListener

    init {
        val customProperties = context.obtainStyledAttributes(attributeSet, R.styleable.Backdrop)

        try {
            val moIcon: Drawable? = customProperties.getDrawable(R.styleable.Backdrop_openIcon)
            val mcIcon: Drawable? = customProperties.getDrawable(R.styleable.Backdrop_closeIcon)
            moIcon?.let { openIcon = moIcon }
            mcIcon?.let { closeIcon = mcIcon }

            animationDuration = customProperties.getInt(R.styleable.Backdrop_animationDuration, animationDuration.toInt()).toLong()
            backdropSize = customProperties.getDimensionPixelSize(R.styleable.Backdrop_backViewSize, backdropSize)
            mcToolbarId = customProperties.getResourceId(R.styleable.Backdrop_toolbar, -1)

        } finally {
            customProperties.recycle()
        }
    }

    /**
     * Build the backdrop view.
     *
     * NOTE: Require Toolbar is initialized with reference
     */
    private fun build() {
        setToolbarWithReference()
        val backView = getBackView()
        val sheet = getFrontView()

        navIconClickListener = OnClickListener {
            backdropShown = !backdropShown

            // reduce backdrop folded height by reduce the toolbarNavIcon with & status bar height
            val size = backView.measuredHeight

            // if no backdrop size provided, divide by the screen height
            val translateY = if (backdropSize != 0) backdropSize else size

            // Cancel the existing animations
            animatorSet.removeAllListeners()
            animatorSet.end()
            animatorSet.cancel()

            // menu icon update
            if (openIcon != null && closeIcon != null) {
                toolbar?.navigationIcon = if (backdropShown) closeIcon else openIcon
            }

            // [START] animation
            val animator = ObjectAnimator.ofFloat(sheet, "translationY", (if (backdropShown) translateY else 0).toFloat())
            animator.duration = animationDuration
            interpolator.let{ interpolator ->
                animator.interpolator = interpolator
            }

            // play the animation
            animatorSet.play(animator)
            animator.start()
        }

        toolbar?.apply {
            if (openIcon != null) {
                navigationIcon = openIcon
            }
            setNavigationOnClickListener(navIconClickListener)
        }
    }

    private fun setToolbarWithReference() {
        val view: View? = rootView.findViewById(mcToolbarId)
        view?.let{
            toolbar = view as Toolbar
        }
    }

    private fun setToolbar(toolbar: Toolbar) {
        this.toolbar = toolbar
    }

    fun toggle() {
        if (::navIconClickListener.isInitialized) {
            navIconClickListener.onClick(toolbar)
        }
    }

    /**
     * Call this function will open the backdrop.
     *
     * NOTE: this will open, only if it is currently closed.
     */
    fun openBackdrop() : Boolean = if(::navIconClickListener.isInitialized && !backdropShown){
        navIconClickListener.onClick(toolbar)
        true
    } else { false }

    /**
     * Call this function will close the backdrop
     *
     * NOTE: this will close, nly if it is currently opened.
     */
    fun closeBackdrop() : Boolean = if(::navIconClickListener.isInitialized && backdropShown){
        navIconClickListener.onClick(toolbar)
        true
    } else { false }

    /**
     * Here whe check if there is more than two child views.
     * If true, we throw an exception in runtime.
     * @throws IllegalArgumentException if there is more than two child views.
     *
     * And change the front view background color.
     */
    override fun onFinishInflate() {
        super.onFinishInflate()

        // if there is more than two views, crash the execution
        if (childCount <= 1 || childCount > 2) {
            throw IllegalArgumentException(" ${this.javaClass.simpleName} must contain two child views!")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        build()
    }

    /**
     * Function to return the back view.
     * @return the first view in this layout.
     */
    private fun getBackView(): View = getChildAt(0)

    /**
     * Function to return the backdrop view.
     * @return the second view in this layout.
     */
    private fun getFrontView(): View = getChildAt(1)

}