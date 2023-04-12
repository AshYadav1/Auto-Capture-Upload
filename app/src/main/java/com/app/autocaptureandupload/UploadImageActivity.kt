package com.app.autocaptureandupload


import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.app.autocaptureandupload.api.RetrofitService
import com.app.autocaptureandupload.repository.MainRepository
import com.app.autocaptureandupload.repository.MyViewModelFactory
import com.app.autocaptureandupload.viewModels.UploadImageViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class UploadImageActivity : AppCompatActivity() {
    private var mManualFocusEngaged: Boolean = false
    private var timeSet: Long = 0
    private lateinit var timer: Timer
    private lateinit var viewModel: UploadImageViewModel
    private val retrofitService = RetrofitService.getInstance()

    private var ivBack: ImageView? = null
    private lateinit var capReq: CaptureRequest.Builder
    private lateinit var cameraManager: CameraManager
    private lateinit var captureSession: CameraCaptureSession
    private var cameraDevice: CameraDevice? = null
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var texterView: TextureView
    private lateinit var imageReader: ImageReader
    private var cameraId: String = ""

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        val buffer = image!!.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val pictureFile: File? = getOutputMediaFile()
        if (pictureFile != null) {
            val fos = FileOutputStream(pictureFile)
            fos.write(bytes)
            fos.close()
            viewModel.uploadImage(pictureFile)
        }
        image?.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_image2)
        startBackgroundThread()
        cameraSetup()
        ivBack = findViewById(R.id.iv_back)
        ivBack?.visibility = View.VISIBLE
        ivBack?.setOnClickListener {
            onBackPressed()
        }
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(
            packageName,
            Context.MODE_PRIVATE
        )
        val mInterval = sharedPreferences.getInt("time_interval", 2)
        timeSet = (mInterval * 1000).toLong()

        viewModel =
            ViewModelProvider(
                this,
                MyViewModelFactory(MainRepository(retrofitService))
            )[UploadImageViewModel::class.java]

    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }

    fun cameraSetup() {

        texterView = findViewById(R.id.texterView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds: Array<String> = cameraManager.cameraIdList
        for (id in cameraIds) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            //If we want to choose the rear facing camera instead of the front facing one
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                continue


            }
            val previewSize =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
            imageReader =
                ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(onImageAvailableListener, handler)
            cameraId = id
        }

        texterView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }
        }
    }


    private fun startBackgroundThread() {
        handlerThread = HandlerThread("CameraVideoThread")
        handlerThread.start()
        handler = Handler(
            handlerThread.looper
        )
    }

    private fun stopBackgroundThread() {
        handlerThread.quitSafely()
        handlerThread.join()
    }

    private fun openCamera() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    cameraDevice = p0

                    val surfaceTexture: SurfaceTexture? = texterView.surfaceTexture
                    val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId) //2
                    val previewSize =
                        cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                            .getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
                    surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)

                    val surface = Surface(texterView.surfaceTexture)


                    capReq = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    capReq.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )
                    capReq.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO
                    )
                    capReq.set(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_START
                    )

                    capReq.addTarget(surface)

                    cameraDevice!!.createCaptureSession(
                        listOf(surface, imageReader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                captureSession = p0
                                captureSession.setRepeatingRequest(capReq.build(), null, handler)
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {

                            }
                        },
                        handler
                    )
                }

                override fun onDisconnected(p0: CameraDevice) {
                    closeCameraDevice()
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                    TODO("Not yet implemented")
                }

            },
            handler
        )
    }


    @MainThread
    private fun closeCameraDevice() {
        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    private fun startCamera() {

        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val orientations: SparseIntArray = SparseIntArray(4).apply {
                    append(Surface.ROTATION_0, 0)
                    append(Surface.ROTATION_90, 90)
                    append(Surface.ROTATION_180, 180)
                    append(Surface.ROTATION_270, 270)
                }

                capReq = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.set(
                    CaptureRequest.CONTROL_MODE,
                    CameraMetadata.CONTROL_MODE_AUTO
                )
                capReq.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO
                )
                capReq.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START
                )

                capReq.addTarget(imageReader.surface)

                val rotation = windowManager.defaultDisplay.rotation
                capReq.set(CaptureRequest.JPEG_ORIENTATION, orientations.get(rotation))
                captureSession.capture(capReq.build(), null, null)

            }
        }, timeSet, timeSet)

    }

    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(
            Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            packageName
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory")
                return null
            }
        }
        // Create a media file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss")
            .format(Date())
        return File(
            mediaStorageDir.path + File.separator
                    + "IMG_" + timeStamp + ".jpg"
        )
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    private fun isMeteringAreaAFSupported(cameraCharacteristics: CameraCharacteristics): Boolean {
        return cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)!! >= 1
    }

    override fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        val actionMasked = motionEvent!!.actionMasked
        if (actionMasked != MotionEvent.ACTION_DOWN) {
            return false
        }
        if (mManualFocusEngaged) {
            Log.d("TAG", "Manual focus already engaged")
            return true
        }
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId) //2
        val sensorArraySize: Rect? =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

        //TODO: here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
        val y = (motionEvent.x / texterView.width.toFloat() * sensorArraySize!!.height()
            .toFloat()).toInt()
        val x = (motionEvent.y / texterView.height.toFloat() * sensorArraySize.width()
            .toFloat()).toInt()
        val halfTouchWidth =
            150 //(int)motionEvent.getTouchMajor(); //TODO: this doesn't represent actual touch size in pixel. Values range in [3, 10]...
        val halfTouchHeight = 150 //(int)motionEvent.getTouchMinor();
        val focusAreaTouch = MeteringRectangle(
            Math.max(x - halfTouchWidth, 0),
            Math.max(y - halfTouchHeight, 0),
            halfTouchWidth * 2,
            halfTouchHeight * 2,
            MeteringRectangle.METERING_WEIGHT_MAX - 1
        )
        val captureCallbackHandler: CaptureCallback = object : CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                mManualFocusEngaged = false
                if (request.tag === "FOCUS_TAG") {
                    //the focus trigger is complete -
                    //resume repeating (preview surface will get frames), clear AF trigger
                    capReq.set(CaptureRequest.CONTROL_AF_TRIGGER, null)
                    captureSession.setRepeatingRequest(capReq.build(), null, null)
                }
            }

            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure
            ) {
                super.onCaptureFailed(session, request, failure)
                Log.e("TAG", "Manual AF failure: $failure")
                mManualFocusEngaged = false
            }
        }

        //first stop the existing repeating request
        captureSession.stopRepeating()

        //cancel any existing AF trigger (repeated touches, etc.)
        capReq.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
        )
        capReq.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_OFF
        )
        captureSession.capture(
            capReq.build(),
            captureCallbackHandler,
            null
        )

        //Now add a new AF trigger with focus region
        if (isMeteringAreaAFSupported(cameraCharacteristics)) {
            capReq.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusAreaTouch))
        }
        capReq.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        capReq.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_AUTO
        )
        capReq.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_START
        )
        capReq.setTag("FOCUS_TAG") //we'll capture this later for resuming the preview

        //then we ask for a single request (not repeating!)
        captureSession.capture(
            capReq.build(),
            captureCallbackHandler,
            null
        )
        mManualFocusEngaged = true
        return true
    }

}