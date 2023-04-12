package com.app.autocaptureandupload.camerautils.model

sealed interface PermissionState {
    object Granted : PermissionState
    data class Denied(val shouldShowRationale: Boolean) : PermissionState
}
