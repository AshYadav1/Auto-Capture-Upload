package com.app.autocaptureandupload.repository

import com.app.autocaptureandupload.MyApplication
import com.app.autocaptureandupload.api.RetrofitService
import okhttp3.RequestBody


class MainRepository(private val retrofitService: RetrofitService

) {

    fun uploadImage(path:String,qrCode: RequestBody)= retrofitService.uploadImage(path,qrCode)


}