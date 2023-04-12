package com.app.autocaptureandupload.api


import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.app.autocaptureandupload.MyApplication
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder


import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



object ApiClient {





    private val gson: Gson by lazy {
        GsonBuilder().setLenient().create()
    }


     private val httpClient: OkHttpClient? by lazy {


         val sharedPreferences: SharedPreferences = MyApplication.appContext.getSharedPreferences(MyApplication.appContext.packageName, Context.MODE_PRIVATE)

         OkHttpClient.Builder()

            .addInterceptor(ChuckerInterceptor(MyApplication.appContext))
            .addInterceptor {

                val original: Request = it.request()
                val requestBuilder = original.newBuilder()

//                requestBuilder.header("Cookie", "session=.eJwlzjsOwjAMANC7ZGaIf4nTy1R2YgvWlk6Iu1OJ6a3vU_Y84nyW7X1c8Sj7a5WtJPhAcqV0Xj3NpDLbojpNmAOUq4dHTbohNp4BQ7Gxdx5KNgxYPbmjje4BVjt5IkxUEIzKA010jd6guXQT1AlpBCpYeVq5I9cZx39D5fsD2vYu6Q.Y947Bg.RqryG5KJSZHj3pwjFdHtBn1hnlQ; Path=/; HttpOnly;")
//                requestBuilder.header("Content-Type", "text/plain")

                if(sharedPreferences.contains("access_token") && !TextUtils.isEmpty(sharedPreferences.getString("access_token","")))
                {
                    requestBuilder.header(
                        "Authorization",

                        "Bearer "+sharedPreferences.getString("access_token","")
                    )
                }



                val request = requestBuilder.build()
                it.proceed(request)
            }
            .build()
    }


    private val httpClientForFirebase: OkHttpClient? by lazy {


        val sharedPreferences: SharedPreferences = MyApplication.appContext.getSharedPreferences(MyApplication.appContext.packageName, Context.MODE_PRIVATE)

        OkHttpClient.Builder()

            .addInterceptor(ChuckerInterceptor(MyApplication.appContext))
            .addInterceptor {

                val original: Request = it.request()
                val requestBuilder = original.newBuilder()

//                requestBuilder.header("Cookie", "session=.eJwlzjsOwjAMANC7ZGaIf4nTy1R2YgvWlk6Iu1OJ6a3vU_Y84nyW7X1c8Sj7a5WtJPhAcqV0Xj3NpDLbojpNmAOUq4dHTbohNp4BQ7Gxdx5KNgxYPbmjje4BVjt5IkxUEIzKA010jd6guXQT1AlpBCpYeVq5I9cZx39D5fsD2vYu6Q.Y947Bg.RqryG5KJSZHj3pwjFdHtBn1hnlQ; Path=/; HttpOnly;")
                requestBuilder.header("Content-Type", "text/plain")

//                if(sharedPreferences.contains("access_token") && !TextUtils.isEmpty(sharedPreferences.getString("access_token","")))
//                {
//                    requestBuilder.header(
//                        "Authorization",
//
//                        "Bearer "+sharedPreferences.getString("access_token","")
//                    )
//                }



                val request = requestBuilder.build()
                it.proceed(request)
            }
            .build()
    }


    private val httpClientForMove: OkHttpClient? by lazy {


        val sharedPreferences: SharedPreferences = MyApplication.appContext.getSharedPreferences(MyApplication.appContext.packageName, Context.MODE_PRIVATE)

        OkHttpClient.Builder()

            .addInterceptor(ChuckerInterceptor(MyApplication.appContext))
            .addInterceptor {

                val original: Request = it.request()
                val requestBuilder = original.newBuilder()

//                requestBuilder.header("Cookie", "session=.eJwlzjsOwjAMANC7ZGaIf4nTy1R2YgvWlk6Iu1OJ6a3vU_Y84nyW7X1c8Sj7a5WtJPhAcqV0Xj3NpDLbojpNmAOUq4dHTbohNp4BQ7Gxdx5KNgxYPbmjje4BVjt5IkxUEIzKA010jd6guXQT1AlpBCpYeVq5I9cZx39D5fsD2vYu6Q.Y947Bg.RqryG5KJSZHj3pwjFdHtBn1hnlQ; Path=/; HttpOnly;")
//                requestBuilder.header("Content-Type", "text/plain")

//                if(sharedPreferences.contains("access_token") && !TextUtils.isEmpty(sharedPreferences.getString("access_token","")))
//                {
//                    requestBuilder.header(
//                        "Authorization",
//
//                        "Bearer "+sharedPreferences.getString("access_token","")
//                    )
//                }



                val request = requestBuilder.build()
                it.proceed(request)
            }
            .build()
    }

    val retrofit: Retrofit by lazy {


        val  sharedPreferences  :SharedPreferences = MyApplication.appContext.getSharedPreferences(MyApplication.appContext.packageName, Context.MODE_PRIVATE)


        Retrofit.Builder()

//                https://api-dev.momenttrack.com//
//            .baseUrl("https://api-dev.momenttrack.com//")
            .baseUrl("https://jared-bucket-us.s3.us-west-1.amazonaws.com/")
//            .baseUrl("")

            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val retrofitFirebase: Retrofit by lazy {


        Retrofit.Builder()

//                https://api-dev.momenttrack.com//
//            .baseUrl("https://yellow-mercury.firebaseio.com/")
            .baseUrl("https://green-mercury-207900.firebaseio.com/")
//            .baseUrl("https://mt-sandbox.firebaseio.com/")

            .client(httpClientForFirebase)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val retrofitForMoveItem: Retrofit by lazy {


        Retrofit.Builder()

//                https://api-dev.momenttrack.com//
            .baseUrl("https://api.3hd.us/")

            .client(httpClientForMove)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }


}