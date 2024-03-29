package com.s16.ext

inline fun <reified T> IIF(condition: Boolean, truePart: T?, falsePart: T?): T? {
    return if (condition) truePart else falsePart
}

inline fun <reified T: Any> Any.letTo(fn: (value: T) -> Unit) = fn.invoke(this as T)


fun <T1: Any, T2: Any> let2(v1: T1?, v2: T2?, fn: (val1: T1, val2: T2) -> Unit) {
    if (v1 != null && v2 != null) {
        fn.invoke(v1, v2)
    }
}

fun <T1: Any, T2: Any, T3: Any> let3(v1: T1?, v2: T2?, v3: T3?, fn: (val1: T1, val2: T2, val3: T3) -> Unit) {
    if (v1 != null && v2 != null && v3 != null) {
        fn.invoke(v1, v2, v3)
    }
}

fun LOWORD(value: Int): Int {
    return value and 0xffff
}

fun HIWORD(value: Int): Int {
    return value shr 0x10 and 0xffff
}

fun MAKELPARAM(low: Int, high: Int): Int {
    return high shl 0x10 or (low and 0xffff)
}