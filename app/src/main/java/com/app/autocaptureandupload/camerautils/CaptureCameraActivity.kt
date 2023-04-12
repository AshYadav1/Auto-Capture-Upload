
package com.app.autocaptureandupload.camerautils

import CameraExtensionsViewModelFactory
import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.extensions.ExtensionMode
import androidx.core.app.ActivityCompat
import androidx.lifecycle.*
import com.app.autocaptureandupload.Camera
import com.app.autocaptureandupload.R
import com.app.autocaptureandupload.api.RetrofitService
import com.app.autocaptureandupload.camerautils.adapter.CameraExtensionItem
import com.app.autocaptureandupload.camerautils.model.CameraState
import com.app.autocaptureandupload.camerautils.model.CameraUiAction
import com.app.autocaptureandupload.camerautils.model.CaptureState
import com.app.autocaptureandupload.camerautils.model.PermissionState
import com.app.autocaptureandupload.camerautils.repository.ImageCaptureRepository
import com.app.autocaptureandupload.camerautils.ui.CameraExtensionsScreen
import com.app.autocaptureandupload.camerautils.ui.doOnLaidOut
import com.app.autocaptureandupload.camerautils.viewmodel.CameraExtensionsViewModel
import com.app.autocaptureandupload.camerautils.viewstate.CaptureScreenViewState
import com.app.autocaptureandupload.camerautils.viewstate.PostCaptureScreenViewState
import com.app.autocaptureandupload.repository.MainRepository
import com.app.autocaptureandupload.repository.MyViewModelFactory
import com.app.autocaptureandupload.viewModels.UploadImageViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

/**
 * Displays the camera preview with camera controls and available extensions. Tapping on the shutter
 * button will capture a photo and display the photo.
 */
class CaptureCameraActivity : AppCompatActivity() {
    private val extensionName = mapOf(
        ExtensionMode.AUTO to R.string.camera_mode_auto,
        ExtensionMode.NIGHT to R.string.camera_mode_night,
        ExtensionMode.HDR to R.string.camera_mode_hdr,
        ExtensionMode.FACE_RETOUCH to R.string.camera_mode_face_retouch,
        ExtensionMode.BOKEH to R.string.camera_mode_bokeh,
        ExtensionMode.NONE to R.string.camera_mode_none,
    )

    // tracks the current view state
    private val captureScreenViewState = MutableStateFlow(CaptureScreenViewState())

    // handles back press if the current screen is the photo post capture screen
//    private val postCaptureBackPressedCallback = object : OnBackPressedCallback(false) {
//        override fun handleOnBackPressed() {
//            lifecycleScope.launch {
//                closePhotoPreview()
//            }
//        }
//    }

    private lateinit var cameraExtensionsScreen: CameraExtensionsScreen

    // view model for operating on the camera and capturing a photo
    private lateinit var cameraExtensionsViewModel: CameraExtensionsViewModel

    // monitors changes in camera permission state
    private lateinit var permissionState: MutableStateFlow<PermissionState>
    private var timeSet: Long = 0
    private lateinit var timer: Timer
    private val retrofitService = RetrofitService.getInstance()
    private lateinit var apiViewModel: UploadImageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        // Api view model
        apiViewModel =
            ViewModelProvider(
                this,
                MyViewModelFactory(MainRepository(retrofitService))
            )[UploadImageViewModel::class.java]

        cameraExtensionsViewModel = ViewModelProvider(
            this,
            CameraExtensionsViewModelFactory(
                application,
                ImageCaptureRepository.create(applicationContext),
                apiViewModel
            )
        )[CameraExtensionsViewModel::class.java]

        // Get time interval from user
        getTimeInterval()


        // capture screen abstracts the UI logic and exposes simple functions on how to interact
        // with the UI layer.
        cameraExtensionsScreen = CameraExtensionsScreen(findViewById(R.id.root))


        // consume and dispatch the current view state to update the camera extensions screen
        lifecycleScope.launch {
            captureScreenViewState.collectLatest { state ->
                cameraExtensionsScreen.setCaptureScreenViewState(state)
//                postCaptureBackPressedCallback.isEnabled =
//                    state.postCaptureScreenViewState is PostCaptureScreenViewState.PostCaptureScreenVisibleViewState
            }
        }

//        onBackPressedDispatcher.addCallback(this, postCaptureBackPressedCallback)

        // initialize the permission state flow with the current camera permission status
        permissionState = MutableStateFlow(getCurrentPermissionState())

        val requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    lifecycleScope.launch { permissionState.emit(PermissionState.Granted) }
                } else {
                    lifecycleScope.launch { permissionState.emit(PermissionState.Denied(true)) }
                }
            }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // check the current permission state every time upon the activity is resumed
                permissionState.emit(getCurrentPermissionState())
            }
        }

        // Consumes actions emitted by the UI and performs the appropriate operation associated with
        // the view model or permission flow.
        // Note that this flow is a shared flow and will not emit the last action unlike the state
        // flows exposed by the view model for consuming UI state.
        lifecycleScope.launch {
            cameraExtensionsScreen.action.collectLatest { action ->
                when (action) {
                    is CameraUiAction.SelectCameraExtension -> {
                        cameraExtensionsViewModel.setExtensionMode(action.extension)
                    }
                    CameraUiAction.ShutterButtonClick -> {
                        cameraExtensionsViewModel.capturePhoto()
                    }
                    CameraUiAction.SwitchCameraClick -> {
                        cameraExtensionsViewModel.switchCamera()
                    }
//                    CameraUiAction.ClosePhotoPreviewClick -> {
//                        closePhotoPreview()
//                    }
                    CameraUiAction.RequestPermissionClick -> {
                        requestPermissionsLauncher.launch(Manifest.permission.CAMERA)
                    }
                    is CameraUiAction.Focus -> {
                        cameraExtensionsViewModel.focus(action.meteringPoint)
                    }
                    is CameraUiAction.Scale -> {
                        cameraExtensionsViewModel.scale(action.scaleFactor)
                    } else -> {

                    }
                }
            }
        }

        // Consume state emitted by the view model to render the Photo Capture state.
        // Upon collecting this state, the last emitted state will be immediately received.
        lifecycleScope.launch {
            cameraExtensionsViewModel.captureUiState.collectLatest { state ->
                when (state) {
                    CaptureState.CaptureNotReady -> {
                        captureScreenViewState.emit(
                            captureScreenViewState.value
                                .updatePostCaptureScreen {
                                    PostCaptureScreenViewState.PostCaptureScreenHiddenViewState
                                }
                                .updateCameraScreen {
                                    it.enableCameraShutter(true)
                                        .enableSwitchLens(true)
                                }
                        )
                    }
                    CaptureState.CaptureReady -> {
                        captureScreenViewState.emit(
                            captureScreenViewState.value
                                .updateCameraScreen {
                                    it.enableCameraShutter(true)
                                        .enableSwitchLens(true)
                                }
                        )
                    }
                    CaptureState.CaptureStarted -> {
                        captureScreenViewState.emit(
                            captureScreenViewState.value
                                .updateCameraScreen {
                                    it.enableCameraShutter(false)
                                        .enableSwitchLens(false)
                                }
                        )
                    }
                    is CaptureState.CaptureFinished -> {
//                        cameraExtensionsViewModel.stopPreview()
                        captureScreenViewState.emit(
                            captureScreenViewState.value
                                .updatePostCaptureScreen {
                                    val uri = state.outputResults.savedUri
                                    if (uri != null) {

                                        PostCaptureScreenViewState.PostCaptureScreenVisibleViewState(
                                            uri
                                        )
                                    } else {
                                        PostCaptureScreenViewState.PostCaptureScreenHiddenViewState
                                    }
                                }
                                .updateCameraScreen {
                                    it.enableCameraShutter(true)
                                        .enableSwitchLens(true)
                                }
                        )
                        cameraExtensionsScreen.tapToFocus()
                    }
                    is CaptureState.CaptureFailed -> {
                        cameraExtensionsScreen.showCaptureError("Couldn't take photo")
                        cameraExtensionsViewModel.startPreview(
                            this@CaptureCameraActivity as LifecycleOwner, cameraExtensionsScreen.previewView
                        )
                        captureScreenViewState.emit(
                            captureScreenViewState.value
                                .updateCameraScreen {
                                    it.showCameraControls()
                                        .enableCameraShutter(true)
                                        .enableSwitchLens(true)
                                }
                        )
                    }
                }
            }
        }

        // Because camera state is dependent on the camera permission status, we combine both camera
        // UI state and permission state such that each emission accurately captures the current
        // permission status and camera UI state.
        // The camera permission is always checked to see if it's granted. If it isn't then stop
        // interacting with the camera and display the permission request screen. The user can tap
        // on "Turn On" to request permissions.
        lifecycleScope.launch {
            permissionState.combine(cameraExtensionsViewModel.cameraUiState) { permissionState, cameraUiState ->
                Pair(permissionState, cameraUiState)
            }
                .collectLatest { (permissionState, cameraUiState) ->
                    when (permissionState) {
                        PermissionState.Granted -> {
                            cameraExtensionsScreen.hidePermissionsRequest()
                        }
                        is PermissionState.Denied -> {
                            if (cameraUiState.cameraState != CameraState.PREVIEW_STOPPED) {
                                cameraExtensionsScreen.showPermissionsRequest(permissionState.shouldShowRationale)
                                return@collectLatest
                            }
                        }
                    }

                    when (cameraUiState.cameraState) {
                        CameraState.NOT_READY -> {
                            captureScreenViewState.emit(
                                captureScreenViewState.value
                                    .updatePostCaptureScreen {
                                        PostCaptureScreenViewState.PostCaptureScreenHiddenViewState
                                    }
                                    .updateCameraScreen {
                                        it.showCameraControls()
                                            .enableCameraShutter(false)
                                            .enableSwitchLens(false)
                                    }
                            )
                            cameraExtensionsViewModel.initializeCamera()
                        }
                        CameraState.READY -> {
                            cameraExtensionsScreen.previewView.doOnLaidOut {
                                cameraExtensionsViewModel.startPreview(
                                    this@CaptureCameraActivity as LifecycleOwner,
                                    cameraExtensionsScreen.previewView
                                )
                            }
                            captureScreenViewState.emit(
                                captureScreenViewState.value
                                    .updateCameraScreen { s ->
                                        s.showCameraControls()
                                            .setAvailableExtensions(
                                                cameraUiState.availableExtensions.map {
                                                    CameraExtensionItem(
                                                        it,
                                                        getString(extensionName[it]!!),
                                                        cameraUiState.extensionMode == it
                                                    )
                                                }
                                            )
                                    }
                            )
                        }
                        CameraState.PREVIEW_STOPPED -> Unit
                    }
                }
        }

        startTimer()
    }

//    private fun uploadDataToApi(uri: Uri) {
//        val msg = "Photo capture succeeded: $uri"
//        //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//        Toast.makeText(baseContext, "Image Captured", Toast.LENGTH_SHORT).show()
//        Log.e("Image Captured", msg)
//        val sound = MediaActionSound()
//        sound.play(MediaActionSound.SHUTTER_CLICK)
//    }


    override fun onResume() {
        if (this::timer.isInitialized){
            timer.cancel()
        }
        startTimer()
        super.onResume()
    }


    override fun onPause() {
        if (this::timer.isInitialized){
            timer.cancel()
        }
        super.onPause()
    }



    private fun getTimeInterval() {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(
            packageName,
            Context.MODE_PRIVATE
        )
        val mInterval = sharedPreferences.getInt("time_interval", 5)
        timeSet = (mInterval * 1000).toLong()
    }

    private fun startTimer() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                cameraExtensionsViewModel.capturePhoto()
            }
        }, timeSet, timeSet)
    }
//    private suspend fun closePhotoPreview() {
//        captureScreenViewState.emit(
//            captureScreenViewState.value
//                .updateCameraScreen { state ->
//                    state.showCameraControls()
//                }
//                .updatePostCaptureScreen {
//                    PostCaptureScreenViewState.PostCaptureScreenHiddenViewState
//                }
//        )
//        cameraExtensionsViewModel.startPreview(
//            this as LifecycleOwner,
//            cameraExtensionsScreen.previewView
//        )
//    }

    private fun getCurrentPermissionState(): PermissionState {
        val status = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return if (status == PackageManager.PERMISSION_GRANTED) {
            PermissionState.Granted
        } else {
            PermissionState.Denied(
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            )
        }
    }
}
