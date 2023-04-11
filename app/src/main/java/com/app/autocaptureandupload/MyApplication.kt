package com.app.autocaptureandupload

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class MyApplication : Application() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

          sharedPreferences   = appContext.getSharedPreferences(MyApplication.appContext.packageName, Context.MODE_PRIVATE)

        url= sharedPreferences.getString("entered_url","")!!
    }

    companion object {

        lateinit  var appContext: Context
        lateinit  var url: String


    }
}