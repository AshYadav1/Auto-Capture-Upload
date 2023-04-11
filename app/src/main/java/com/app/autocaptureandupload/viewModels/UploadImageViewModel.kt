package com.app.autocaptureandupload.viewModels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.autocaptureandupload.MyApplication
import com.app.autocaptureandupload.models.ReadQrCodeResponse
import com.app.autocaptureandupload.repository.MainRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Calendar

class UploadImageViewModel  (private val repository: MainRepository) : ViewModel() {

    val movieList =  MutableLiveData<String>()
    val errorMessage = MutableLiveData<String>()
    val failure = MutableLiveData<Int>()
    val doneObserver =  MutableLiveData<String>()



    fun readQrDetails(qrCode: String) {
//        val response = repository.readQrCode(qrCode)
//        response.enqueue(object : Callback<com.app.autocaptureandupload.models.ReadQrCodeResponse> {
//            override fun onResponse(call: Call<com.app.autocaptureandupload.models.ReadQrCodeResponse>, response: Response<com.app.autocaptureandupload.models.ReadQrCodeResponse>) {
////                movieList.postValue(response.body())
//                if(response.code()==200 && response.body() !=null && response.body()!!.success==true)
//
//                {
//                    movieList.postValue(response.body())
//                }
//                else if(response.code()==401){
//                    failure.postValue(response.code())
//                }
//                else{
//                    errorMessage.postValue(response.message())
//                }
//
//            }
//
//            override fun onFailure(call: Call<com.app.autocaptureandupload.models.ReadQrCodeResponse>, t: Throwable) {
//
//            }
//
//        })
    }

    fun uploadImage(file: File) {
        val sharedPreferences: SharedPreferences = MyApplication.appContext.getSharedPreferences(MyApplication.appContext.packageName, Context.MODE_PRIVATE)

        var mUrl = sharedPreferences.getString("entered_url", "")
        mUrl=mUrl.plus("/"+Calendar.getInstance().timeInMillis+".jpeg")

        val mCONTENTIMAGE = "image/jpeg"
println("URL:--- $mUrl")
//        val file = File(mOutputFilePath)
        val requestBody = file.asRequestBody(mCONTENTIMAGE.toMediaTypeOrNull())
        val response = mUrl.let { repository.uploadImage(it,requestBody) }
        response.enqueue(object : Callback<ReadQrCodeResponse> {
            override fun onResponse(call: Call<ReadQrCodeResponse>, response: Response<ReadQrCodeResponse>) {
    //                movieList.postValue(response.body())
                if(response.code()==200 && response.body() !=null && response.body()!!.success==true) {
                    movieList.postValue(response?.body()?.message!!)
                } else if(response.code()==401){
                    failure.postValue(response.code())
                } else{
                    errorMessage.postValue(response.message())
                }

            }

            override fun onFailure(call: Call<ReadQrCodeResponse>, t: Throwable) {

            }

        })

    }


}