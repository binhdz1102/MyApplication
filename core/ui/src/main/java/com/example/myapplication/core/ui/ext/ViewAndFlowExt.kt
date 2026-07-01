package com.example.myapplication.core.ui.ext

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun View.playEntrance(delayMs: Long = 0L) {
    alpha = 0f
    translationY = 28f
    animate()
        .alpha(1f)
        .translationY(0f)
        .setInterpolator(DecelerateInterpolator())
        .setStartDelay(delayMs)
        .setDuration(320L)
        .start()
}

fun <T> Fragment.collectLatestState(
    flow: Flow<T>,
    collector: suspend (T) -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(collector)
        }
    }
}
