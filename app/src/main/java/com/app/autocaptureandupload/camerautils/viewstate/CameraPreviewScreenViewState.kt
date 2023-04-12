package com.app.autocaptureandupload.camerautils.viewstate

import com.app.autocaptureandupload.camerautils.adapter.CameraExtensionItem


/**
 * Represents the camera preview screen view state. The camera preview screen shows camera controls
 * and the camera preview.
 */
data class CameraPreviewScreenViewState(
    val shutterButtonViewState: ShutterButtonViewState = ShutterButtonViewState(),
    val switchLensButtonViewState: SwitchLensButtonViewState = SwitchLensButtonViewState(),
    val extensionsSelectorViewState: CameraExtensionSelectorViewState = CameraExtensionSelectorViewState()
) {
    fun hideCameraControls(): CameraPreviewScreenViewState =
        copy(
            shutterButtonViewState = shutterButtonViewState.copy(isVisible = false),
            switchLensButtonViewState = switchLensButtonViewState.copy(isVisible = false),
            extensionsSelectorViewState = extensionsSelectorViewState.copy(isVisible = false)
        )

    fun showCameraControls(): CameraPreviewScreenViewState =
        copy(
            shutterButtonViewState = shutterButtonViewState.copy(isVisible = true),
            switchLensButtonViewState = switchLensButtonViewState.copy(isVisible = false),
            extensionsSelectorViewState = extensionsSelectorViewState.copy(isVisible = false)
        )

    fun enableCameraShutter(isEnabled: Boolean): CameraPreviewScreenViewState =
        copy(shutterButtonViewState = shutterButtonViewState.copy(isEnabled = isEnabled))

    fun enableSwitchLens(isEnabled: Boolean): CameraPreviewScreenViewState =
        copy(switchLensButtonViewState = switchLensButtonViewState.copy(isEnabled = false))

    fun setAvailableExtensions(extensions: List<CameraExtensionItem>): CameraPreviewScreenViewState =
        copy(extensionsSelectorViewState = extensionsSelectorViewState.copy(extensions = extensions))
}

data class CameraExtensionSelectorViewState(
    val isVisible: Boolean = false,
    val extensions: List<CameraExtensionItem> = emptyList()
)

data class ShutterButtonViewState(
    val isVisible: Boolean = false,
    val isEnabled: Boolean = false
)

data class SwitchLensButtonViewState(
    val isVisible: Boolean = false,
    val isEnabled: Boolean = false
)