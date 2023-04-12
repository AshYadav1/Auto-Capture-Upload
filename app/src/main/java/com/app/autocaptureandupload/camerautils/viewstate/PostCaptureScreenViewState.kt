package com.app.autocaptureandupload.camerautils.viewstate

import android.net.Uri

/**
 * Represents the post capture screen view state. This can be either visible with a uri for the
 * photo captured or hidden.
 */
sealed interface PostCaptureScreenViewState {
    object PostCaptureScreenHiddenViewState : PostCaptureScreenViewState

    data class PostCaptureScreenVisibleViewState(val uri: Uri) : PostCaptureScreenViewState
}