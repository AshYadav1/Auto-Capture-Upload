package com.app.autocaptureandupload.camerautils.viewstate

/**
 * Capture Screen is the top level view state. A capture screen contains a camera preview screen
 * and a post capture screen.
 */
data class CaptureScreenViewState(
    val cameraPreviewScreenViewState: CameraPreviewScreenViewState = CameraPreviewScreenViewState(),
    val postCaptureScreenViewState: PostCaptureScreenViewState = PostCaptureScreenViewState.PostCaptureScreenHiddenViewState
) {
    fun updateCameraScreen(block: (cameraPreviewScreenViewState: CameraPreviewScreenViewState) -> CameraPreviewScreenViewState): CaptureScreenViewState =
        copy(cameraPreviewScreenViewState = block(cameraPreviewScreenViewState))

    fun updatePostCaptureScreen(block: (postCaptureScreenViewState: PostCaptureScreenViewState) -> PostCaptureScreenViewState) =
        copy(postCaptureScreenViewState = block(postCaptureScreenViewState))
}
