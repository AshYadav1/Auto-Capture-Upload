package com.app.autocaptureandupload

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.app.autocaptureandupload.api.RetrofitService
import com.app.autocaptureandupload.repository.MainRepository
import com.app.autocaptureandupload.repository.MyViewModelFactory
import com.app.autocaptureandupload.viewModels.UploadImageViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class RectOverlay constructor(context: Context?, attributeSet: AttributeSet?) :
        View(context, attributeSet) {
    private val rectBounds: MutableList<RectF> = mutableListOf()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context!!, android.R.color.holo_green_light)
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Pass it a list of RectF (rectBounds)
        rectBounds.forEach { canvas.drawRect(it, paint) }
    }

    fun drawRectBounds(rectBounds: List<RectF>) {
        this.rectBounds.clear()
        this.rectBounds.addAll(rectBounds)
        invalidate()
    }
}

class Camera : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var rect_overlay: RectOverlay
    private var imageCapture: ImageCapture? = null
    private lateinit var viewModel: UploadImageViewModel
    private val retrofitService = RetrofitService.getInstance()
    private lateinit var timer: Timer

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var timeSet: Long = 0

    // public class variables
    var base_dir: String = "Camera"
    var dir_name: String? = "images"
    var rectSize = 100

    var storge_permissions: Array<String?> = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    var storge_permissions_33: Array<String?> = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        viewFinder = findViewById(R.id.viewFinder)
        rect_overlay = findViewById(R.id.rect_overlay)
        getTimeInterval()

        viewModel =
            ViewModelProvider(
                this,
                MyViewModelFactory(MainRepository(retrofitService))
            )[UploadImageViewModel::class.java]

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
            startTimer()
        } else {
            permissions()?.let {
                ActivityCompat.requestPermissions(this,
                    it,
                    1)
            };
//            ActivityCompat.requestPermissions(
//                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
//        camera_capture_button.setOnClickListener { takePhoto() }

        // view image
//        viewimages.setOnClickListener{viewImage()}



        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        supportActionBar!!.setTitle("Camera App")
    }

    fun permissions(): Array<String?>? {
        val p: Array<String?>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            p = storge_permissions_33
        } else {
            p = storge_permissions
        }
        return p
    }


    private fun startTimer() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                takePhoto()
            }
        }, timeSet, timeSet)
    }


    private fun getTimeInterval() {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(
            packageName,
            Context.MODE_PRIVATE
        )
        val mInterval = sharedPreferences.getInt("time_interval", 2)
        timeSet = (mInterval * 1000).toLong()
    }


    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        val imageFilePath = imageFile.getAbsolutePath();

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(imageFile)
                val msg = "Photo capture succeeded: $savedUri"
                //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Toast.makeText(baseContext, "Image Captured", Toast.LENGTH_SHORT).show()
                Log.e(TAG, msg)
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
                viewModel.uploadImage(File(imageFilePath.toString()))
            }
        })

    }

//    @Throws(IOException::class)
//    private fun createImageFile(): File? {
//        // Create an image file name
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val imageFileName = "JPEG_" + timeStamp + "_"
//        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        val image = File.createTempFile(
//            imageFileName,  /* prefix */
//            ".jpg",  /* suffix */
//            storageDir /* directory */
//        )
//
//        // Save a file: path for use with ACTION_VIEW intents
//        mCurrentPhotoPath = image.absolutePath
//        return image
//    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val useCaseGroup = UseCaseGroup.Builder()
                .setViewPort(viewFinder.viewPort!!)
                .addUseCase(imageCapture!!)
                .addUseCase(preview)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                preview.setSurfaceProvider(viewFinder.surfaceProvider)


                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(this@Camera as LifecycleOwner, cameraSelector, preview, imageCapture)


                //touch to focus listener
                viewFinder.setOnTouchListener { _, event ->
                    if (event.action != MotionEvent.ACTION_UP) {
                        return@setOnTouchListener true
                    }

                    val factory = viewFinder.getMeteringPointFactory()
                    val point = factory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point).build()
                    camera.cameraControl.startFocusAndMetering(action)

                    val focusRects = listOf(RectF(event.x-rectSize, event.y-rectSize, event.x+rectSize, event.y+rectSize))
                    rect_overlay.post { rect_overlay.drawRectBounds(focusRects) }

                    Log.e(TAG, "Focus Coordinates: " + event.x+" , "+event.y)
                    Log.e(TAG, "preview view dimensions: " + viewFinder.width+" x "+viewFinder.height)

                    return@setOnTouchListener true
                }


            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }


        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }


    private fun getOutputDirectory(): File {
        // ugly way to do this, fix if needed
        var dir = File(Environment.getExternalStorageDirectory(), base_dir)
        if (!dir.exists()) {
            dir.mkdirs()
            Log.e(TAG, "Output directory Created")
        }
        dir = File(Environment.getExternalStorageDirectory(), base_dir + '/' + dir_name)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return dir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

}