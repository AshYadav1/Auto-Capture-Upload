import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.autocaptureandupload.camerautils.repository.ImageCaptureRepository
import com.app.autocaptureandupload.camerautils.viewmodel.CameraExtensionsViewModel
import com.app.autocaptureandupload.viewModels.UploadImageViewModel

/**
 * Creates ViewModel instances of [CameraExtensionsViewModel] to support injection of [Application]
 * and [ImageCaptureRepository]
 */
class CameraExtensionsViewModelFactory(
    private val application: Application,
    private val imageCaptureRepository: ImageCaptureRepository,
    private val apiViewModel: UploadImageViewModel
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CameraExtensionsViewModel(application, imageCaptureRepository, apiViewModel) as T
    }
}
