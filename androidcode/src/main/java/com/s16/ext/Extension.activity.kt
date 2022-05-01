package com.s16.ext

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf

/*
 * Activity
 */
val Activity.context: Context
    get() = this

val Activity.decorView: View
    get() {
        return window.decorView
    }

val Activity.contentView: ViewGroup
    get() {
        val decorView = window.decorView as ViewGroup
        return decorView.findViewById(android.R.id.content)
    }

val Activity.rootView: ViewGroup
    get() {
        val decorView = window.decorView as ViewGroup
        val view = decorView.findViewById<ViewGroup>(android.R.id.content)
        return view.getChildAt(0) as ViewGroup
    }


inline fun <reified T: Activity> Activity.startActivityForResult(requestCode: Int, extras: Bundle? = null) {
    val intent = Intent(this, T::class.java)
    if (extras != null) {
        intent.putExtras(extras)
    }
    startActivityForResult(intent, requestCode)
}

inline fun <reified T: Activity> Activity.startActivityForResult(requestCode: Int, vararg params: Pair<String, Any?>) {
    val intent = Intent(this, T::class.java)
    if (params.isNotEmpty()) {
        intent.putExtras(bundleOf(*params))
    }
    startActivityForResult(intent, requestCode)
}

fun Activity.showInputMethod(v: EditText) {
    v.requestFocus()
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED)
}

fun Activity.hideInputMethod() = window.peekDecorView()?.let {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(window.peekDecorView().windowToken, 0)
}

@Suppress("DEPRECATION")
@SuppressLint("ObsoleteSdkInt")
fun Activity.translucentStatusBar(value: Boolean) = window.let {
    if (Build.VERSION.SDK_INT >= 19) {
        if (value) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }
}

@Suppress("DEPRECATION")
@SuppressLint("ObsoleteSdkInt")
fun Activity.translucentNavigationBar(value: Boolean) = window.let {
    if (Build.VERSION.SDK_INT >= 19) {
        if (value) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }
}

@Suppress("DEPRECATION")
@SuppressLint("ObsoleteSdkInt")
fun Activity.lightStatusBar(light: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val appearance = if (light) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0
        window.insetsController?.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        window.decorView.systemUiVisibility = if (light) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
    }
}

@SuppressLint("ObsoleteSdkInt")
fun Activity.fullScreenWindow(value: Boolean) = window.let {
    if (Build.VERSION.SDK_INT >= 19) {
        if (value) {
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }
}

/**
 * Convenient method to avoid the Navigation Bar to blink during transitions on some devices
 * @see <a href="https://stackoverflow.com/questions/26600263/how-do-i-prevent-the-status-bar-and-navigation-bar-from-animating-during-an-acti">stackoverflow.com</a>
 */
@SuppressLint("ObsoleteSdkInt")
fun Activity.makeSceneTransitionAnimation(vararg pairs: Pair<View, String>): ActivityOptionsCompat {
    val updatedPairs = pairs.toMutableList()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val navBar = findViewById<View>(android.R.id.navigationBarBackground)
        if (navBar != null) {
            updatedPairs.add(Pair(navBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME))
        }
    }
    val newPairs = updatedPairs.map {
        androidx.core.util.Pair.create(it.first, it.second)
    }
    return ActivityOptionsCompat.makeSceneTransitionAnimation(this, *newPairs.toTypedArray())
}
