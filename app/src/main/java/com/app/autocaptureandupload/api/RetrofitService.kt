package com.app.autocaptureandupload.api





import com.app.autocaptureandupload.models.ReadQrCodeResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitService {



    @PUT
    fun uploadImage(@Url url: String, @Body file: okhttp3.RequestBody): Call<ReadQrCodeResponse>


    companion object {






        var retrofitService: RetrofitService? = null




        fun getInstance(): RetrofitService {

            retrofitService = ApiClient.retrofit.create(RetrofitService::class.java)
            return retrofitService!!
        }

    }

}