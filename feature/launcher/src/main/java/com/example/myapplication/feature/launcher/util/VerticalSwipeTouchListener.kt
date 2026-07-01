package com.example.myapplication.feature.launcher.util

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class VerticalSwipeTouchListener(
    private val onSwipeUp: (() -> Unit)? = null,
    private val onSwipeDown: (() -> Unit)? = null,
) : View.OnTouchListener {
    private val detector =
        GestureDetector(
            null,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    val startEvent = e1 ?: return false
                    val deltaY = e2.y - startEvent.y
                    val deltaX = e2.x - startEvent.x
                    if (abs(deltaY) <= abs(deltaX)) {
                        return false
                    }
                    if (deltaY < -96f) {
                        onSwipeUp?.invoke()
                        return onSwipeUp != null
                    }
                    if (deltaY > 96f) {
                        onSwipeDown?.invoke()
                        return onSwipeDown != null
                    }
                    return false
                }
            },
        )

    override fun onTouch(
        v: View,
        event: MotionEvent,
    ): Boolean = detector.onTouchEvent(event)
}
