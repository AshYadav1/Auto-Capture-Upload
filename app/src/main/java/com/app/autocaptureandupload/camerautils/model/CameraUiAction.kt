package com.app.autocaptureandupload.camerautils.model

import androidx.camera.core.MeteringPoint
import androidx.camera.extensions.ExtensionMode

/**
 * User initiated actions related to camera operations.
 */
sealed class CameraUiAction {
    object RequestPermissionClick : CameraUiAction()
    object SwitchCameraClick : CameraUiAction()
    object ShutterButtonClick : CameraUiAction()
    object ClosePhotoPreviewClick : CameraUiAction()
    data class SelectCameraExtension(@ExtensionMode.Mode val extension: Int) : CameraUiAction()
    data class Focus(val meteringPoint: MeteringPoint) : CameraUiAction()
    data class Scale(val scaleFactor: Float) : CameraUiAction()
}