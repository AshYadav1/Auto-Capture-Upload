
package com.app.autocaptureandupload.camerautils.adapter

import androidx.camera.extensions.ExtensionMode

/**
 * Defines the item model for a camera extension displayed by the adapter.
 * @see CameraExtensionsSelectorAdapter
 */
data class CameraExtensionItem(
    @ExtensionMode.Mode val extensionMode: Int,
    val name: String,
    val selected: Boolean = false
)