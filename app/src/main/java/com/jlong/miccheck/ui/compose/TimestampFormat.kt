package com.jlong.miccheck.ui.compose

/**
Utility function to format Long's representing milliseconds into timestamp Strings.
 */

fun Long.toTimestamp(): String {
    val hours = this / 1000 / 60 / 60
    val minutes = (this / 1000 / 60) % 60
    val seconds = (this / 1000) % 60
    var stamp = ""

    if (hours > 0)
        stamp += "$hours:"
    if (hours > 0 && minutes < 10)
        stamp += "0"
    stamp += "$minutes:"
    if (seconds < 10)
        stamp += "0"
    stamp += "$seconds"

    return stamp
}