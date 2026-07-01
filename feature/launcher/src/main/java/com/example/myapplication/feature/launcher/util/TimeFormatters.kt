package com.example.myapplication.feature.launcher.util

fun Int.toTimestamp(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}
