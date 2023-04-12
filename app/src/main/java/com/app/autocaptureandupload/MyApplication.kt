package com.app.autocaptureandupload

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class MyApplication : Application(), CameraXConfig.Provider {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        sharedPreferences = appContext.getSharedPreferences(
            appContext.packageName,
            Context.MODE_PRIVATE
        )
        url = sharedPreferences.getString("entered_url", "")!!
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }

    companion object {
        lateinit var appContext: Context
        lateinit var url: String
    }
}