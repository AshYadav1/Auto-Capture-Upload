package com.app.autocaptureandupload.camerautils.ui

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.view.ViewCompat

/**
 * Apply the action when this view is attached to the window and has been measured.
 * If the view is already attached and measured then the action is immediately invoked.
 *
 * @param action The action to apply when the view is laid out
 */
fun View.doOnLaidOut(action: () -> Unit) {
    if (isAttachedToWindow && ViewCompat.isLaidOut(this)) {
        action()
    } else {
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                action()
            }
        })
    }
}