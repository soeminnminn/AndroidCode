package com.s16.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import com.s16.app.StateSaver.OnStateSaveChangeListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


interface StateSaver {
    /**
     * Interface definition for a callback to be invoked when a shared
     * preference is changed.
     */
    interface OnStateSaveChangeListener {
        /**
         * Called when a shared preference is changed, added, or removed. This
         * may be called even if a preference is set to its existing value.
         *
         *
         * This callback will be run on your main thread.
         *
         * @param stateSaver The [StateSaver] that received
         * the change.
         * @param key The key of the preference that was changed, added, or
         * removed.
         */
        fun onStateSaveChanged(stateSaver: StateSaver?, key: String?)
    }

    /**
     * Interface used for modifying values in a [StateSaver]
     * object.  All changes you make in an editor are batched, and not copied
     * back to the original [StateSaver] until you call [.commit]
     * or [.apply]
     */
    interface Editor {
        /**
         * Set a String value in the preferences editor, to be written back once
         * [.commit] or [.apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putString(key: String?, value: String?): Editor?

        /**
         * Set a set of String values in the preferences editor, to be written
         * back once [.commit] is called.
         *
         * @param key The name of the preference to modify.
         * @param values The new values for the preference.
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putStringSet(key: String?, values: Set<String?>?): Editor?

        /**
         * Set an int value in the preferences editor, to be written back once
         * [.commit] or [.apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putInt(key: String?, value: Int): Editor?

        /**
         * Set a long value in the preferences editor, to be written back once
         * [.commit] or [.apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putLong(key: String?, value: Long): Editor?

        /**
         * Set a float value in the preferences editor, to be written back once
         * [.commit] or [.apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putFloat(key: String?, value: Float): Editor?

        /**
         * Set a double value in the preferences editor, to be written back once
         * [.commit] or [.apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putDouble(key: String?, value: Double): Editor?

        /**
         * Set a boolean value in the preferences editor, to be written back
         * once [.commit] or [.apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putBoolean(key: String?, value: Boolean): Editor?

        /**
         * Set a Parcelable value in the preferences editor, to be written back
         * once [.commit] or [.apply] are called.
         *
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun putParcelable(key: String?, value: Parcelable?): Editor?

        /**
         * Mark in the editor that a preference value should be removed, which
         * will be done in the actual preferences once [.commit] is
         * called.
         *
         *
         * Note that when committing back to the preferences, all removals
         * are done first, regardless of whether you called remove before
         * or after put methods on this editor.
         *
         * @param key The name of the preference to remove.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun remove(key: String?): Editor?

        /**
         * Mark in the editor to remove *all* values from the
         * preferences.  Once commit is called, the only remaining preferences
         * will be any that you have defined in this editor.
         *
         *
         * Note that when committing back to the preferences, the clear
         * is done first, regardless of whether you called clear before
         * or after put methods on this editor.
         *
         * @return Returns a reference to the same Editor object, so you can
         * chain put calls together.
         */
        fun clear(): Editor?

        /**
         * Commit your preferences changes back from this Editor to the
         * [StateSaver] object it is editing.  This atomically
         * performs the requested modifications, replacing whatever is currently
         * in the LocalPreferences.
         *
         *
         * Note that when two editors are modifying preferences at the same
         * time, the last one to call commit wins.
         *
         *
         * If you don't care about the return value and you're
         * using this from your application's main thread, consider
         * using [.apply] instead.
         *
         * @return Returns true if the new values were successfully written
         * to persistent storage.
         */
        fun commit(): Boolean

        /**
         * Commit your preferences changes back from this Editor to the
         * [StateSaver] object it is editing.  This atomically
         * performs the requested modifications, replacing whatever is currently
         * in the LocalPreferences.
         *
         *
         * Note that when two editors are modifying preferences at the same
         * time, the last one to call apply wins.
         *
         *
         * Unlike [.commit], which writes its preferences out
         * to persistent storage synchronously, [.apply]
         * commits its changes to the in-memory
         * [StateSaver] immediately but starts an
         * asynchronous commit to disk and you won't be notified of
         * any failures.  If another editor on this
         * [StateSaver] does a regular [.commit]
         * while a [.apply] is still outstanding, the
         * [.commit] will block until all async commits are
         * completed as well as the commit itself.
         *
         *
         * As [StateSaver] instances are singletons within
         * a process, it's safe to replace any instance of [.commit] with
         * [.apply] if you were already ignoring the return value.
         *
         *
         * You don't need to worry about Android component
         * lifecycles and their interaction with `apply()`
         * writing to disk.  The framework makes sure in-flight disk
         * writes from `apply()` complete before switching
         * states.
         *
         *
         * The LocalPreferences.Editor interface
         * isn't expected to be implemented directly.  However, if you
         * previously did implement it and are now getting errors
         * about missing `apply()`, you can simply call
         * [.commit] from `apply()`.
         */
        fun apply()
    }

    /**
     * Retrieve all values from the preferences.
     *
     * @return Returns a map containing a list of pairs key/value representing
     * the preferences.
     *
     * @throws NullPointerException
     */
    val all: Map<String?, *>?

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a String.
     *
     * @throws ClassCastException
     */
    fun getString(key: String?, defValue: String?): String?

    /**
     * Retrieve a set of String values from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValues Values to return if this preference does not exist.
     *
     * @return Returns the preference values if they exist, or defValues.
     * Throws ClassCastException if there is a preference with this name
     * that is not a Set.
     *
     * @throws ClassCastException
     */
    fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>?

    /**
     * Retrieve an int value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * an int.
     *
     * @throws ClassCastException
     */
    fun getInt(key: String?, defValue: Int): Int

    /**
     * Retrieve a long value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a long.
     *
     * @throws ClassCastException
     */
    fun getLong(key: String?, defValue: Long): Long

    /**
     * Retrieve a float value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a float.
     *
     * @throws ClassCastException
     */
    fun getFloat(key: String?, defValue: Float): Float

    /**
     * Retrieve a double value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a float.
     *
     * @throws ClassCastException
     */
    fun getDouble(key: String?, defValue: Double): Double

    /**
     * Retrieve a boolean value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a boolean.
     *
     * @throws ClassCastException
     */
    fun getBoolean(key: String?, defValue: Boolean): Boolean

    /**
     * Retrieve a parcelable value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a parcelable.
     *
     * @throws ClassCastException
     */
    fun <T : Parcelable?> getParcelable(key: String?): T

    /**
     * Checks whether the preferences contains a preference.
     *
     * @param key The name of the preference to check.
     * @return Returns true if the preference exists in the preferences,
     * otherwise false.
     */
    operator fun contains(key: String?): Boolean

    /**
     * Create a new Editor for these preferences, through which you can make
     * modifications to the data in the preferences and atomically commit those
     * changes back to the LocalPreferences object.
     *
     *
     * Note that you *must* call [Editor.commit] to have any
     * changes you perform in the Editor actually show up in the
     * LocalPreferences.
     *
     * @return Returns a new instance of the [Editor] interface, allowing
     * you to modify the values in this LocalPreferences object.
     */
    fun edit(): Editor?

    /**
     * Registers a callback to be invoked when a change happens to a preference.
     *
     * @param listener The callback that will run.
     * @see .unregisterOnLocalPreferenceChangeListener
     */
    fun registerOnStateSaveChangeListener(listener: OnStateSaveChangeListener?)

    /**
     * Unregisters a previous callback.
     *
     * @param listener The callback that should be unregistered.
     * @see .registerOnLocalPreferenceChangeListener
     */
    fun unregisterOnStateSaveChangeListener(listener: OnStateSaveChangeListener?)
}

@Suppress("UNCHECKED_CAST")
class StateSaverImpl private constructor(private val context: Context) : StateSaver {

    // Return value from EditorImpl#commitToMemory()
    private data class MemoryCommitResult (
        // any keys different?
        var changesMade: Boolean,
        // may be null
        var keysModified : MutableList<String?>?,
        // may be null
        var listeners : Set<OnStateSaveChangeListener>?
    )

    inner class EditorImpl : StateSaver.Editor {
        private val mModified: MutableMap<String?, Any?> = mutableMapOf()
        private var mClear = false

        override fun putString(key: String?, value: String?): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = value
                return this
            }
        }

        override fun putStringSet(key: String?, values: Set<String?>?): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = values
                return this
            }
        }

        override fun putInt(key: String?, value: Int): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = value
                return this
            }
        }

        override fun putLong(key: String?, value: Long): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = value
                return this
            }
        }

        override fun putFloat(key: String?, value: Float): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = value
                return this
            }
        }

        override fun putDouble(key: String?, value: Double): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = value
                return this
            }
        }

        override fun putBoolean(key: String?, value: Boolean): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = value
                return this
            }
        }

        override fun putParcelable(key: String?, value: Parcelable?): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = value
                return this
            }
        }

        override fun remove(key: String?): StateSaver.Editor {
            synchronized(this) {
                mModified[key] = this
                return this
            }
        }

        override fun clear(): StateSaver.Editor {
            synchronized(this) {
                mClear = true
                return this
            }
        }

        override fun apply() {
            val mcr = commitToMemory()
            notifyListeners(mcr)
        }

        // Returns true if any changes were made
        private fun commitToMemory(): MemoryCommitResult {
            val mcr = MemoryCommitResult(false, null, null)

            synchronized(this@StateSaverImpl) {
                val hasListeners: Boolean = mListeners.size > 0
                if (hasListeners) {
                    mcr.keysModified = ArrayList()
                    mcr.listeners = HashSet<OnStateSaveChangeListener>(mListeners.keys)
                }
                synchronized(this) {
                    if (mClear) {
                        if (mMap.isNotEmpty()) {
                            mcr.changesMade = true
                            mMap.clear()
                        }
                        mClear = false
                    }
                    for ((k, v) in mModified) {
                        if (v === this) {  // magic value for a removal mutation
                            if (!mMap.containsKey(k)) {
                                continue
                            }
                            mMap.remove(k)
                        } else {
                            if (mMap.containsKey(k)) {
                                val existingValue = mMap[k]
                                if (existingValue != null && existingValue == v) {
                                    continue
                                }
                            }
                            mMap[k] = v
                        }
                        mcr.changesMade = true
                        if (hasListeners) {
                            mcr.keysModified!!.add(k)
                        }
                    }
                    mModified.clear()
                }
            }
            return mcr
        }

        override fun commit(): Boolean {
            val mcr = commitToMemory()
            notifyListeners(mcr)
            return true
        }

        private fun notifyListeners(mcr: MemoryCommitResult) {
            if (mcr.listeners == null || mcr.keysModified == null || mcr.keysModified!!.size == 0) {
                return
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (i in mcr.keysModified!!.indices.reversed()) {
                    val key = mcr.keysModified!![i]
                    for (listener in mcr.listeners!!) {
                        listener.onStateSaveChanged(this@StateSaverImpl, key)
                    }
                }
            } else {
                // Run this function on the main thread.
                mUiHandler.post(Runnable { notifyListeners(mcr) })
            }
        }
    }

    private val mUiHandler: Handler = Handler(Looper.getMainLooper())
    private val mMap: MutableMap<String?, Any?> = mutableMapOf()

    private val mListeners: WeakHashMap<OnStateSaveChangeListener, Any> =
        WeakHashMap<OnStateSaveChangeListener, Any>()

    fun put(key: String?, value: Any?) {
        synchronized(this) { mMap.put(key, value) }
    }

    override fun contains(key: String?): Boolean {
        synchronized(this) { return mMap.containsKey(key) }
    }

    override fun edit(): StateSaver.Editor {
        return EditorImpl()
    }

    override val all: Map<String?, *>
        get() {
            synchronized(this) { return HashMap(mMap) }
        }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        synchronized(this) {
            val v = mMap[key] as Boolean?
            return v ?: defValue
        }
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        synchronized(this) {
            val v = mMap[key] as Float?
            return v ?: defValue
        }
    }

    override fun getDouble(key: String?, defValue: Double): Double {
        synchronized(this) {
            val v = mMap[key] as Double?
            return v ?: defValue
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        synchronized(this) {
            val v = mMap[key] as Int?
            return v ?: defValue
        }
    }

    override fun getLong(key: String?, defValue: Long): Long {
        synchronized(this) {
            val v = mMap[key] as Long?
            return v ?: defValue
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        synchronized(this) {
            val v = mMap[key] as String?
            return v ?: defValue
        }
    }

    override fun getStringSet(key: String?, defValues: Set<String?>?): Set<String?>? {
        synchronized(this) {
            val v =
                mMap[key] as Set<String?>?
            return v ?: defValues
        }
    }

    override fun <T : Parcelable?> getParcelable(key: String?): T {
        val o = mMap[key] ?: return null as T
        return try {
            o as T
        } catch (e: ClassCastException) {
            null as T
        }
    }

    override fun registerOnStateSaveChangeListener(listener: OnStateSaveChangeListener?) {
        synchronized(this) { mListeners.put(listener, context) }
    }

    override fun unregisterOnStateSaveChangeListener(listener: OnStateSaveChangeListener?) {
        synchronized(this) { mListeners.remove(listener) }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: StateSaverImpl? = null

        fun from(context: Context): StateSaver? {
            if (INSTANCE == null) {
                INSTANCE = StateSaverImpl(context)
            }
            return INSTANCE
        }

        fun from(context: Context, prefs: SharedPreferences?): StateSaver? {
            if (INSTANCE == null) {
                INSTANCE = StateSaverImpl(context)
            }
            if (prefs != null) {
                val srcMap = prefs.all
                for ((k, value) in srcMap) {
                    val v = value!!
                    INSTANCE!!.put(k, v)
                }
            }
            return INSTANCE
        }
    }
}