package com.app.autocaptureandupload

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MainActivity : AppCompatActivity() {
    private var isPermitted: Boolean = false
    private var mEtTime: EditText? = null
    private var mEtUrl: EditText? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
       mEtTime= findViewById(R.id.et_interval)
        mEtUrl=findViewById(R.id.et_url)
        mEtUrl?.setSelection(mEtUrl?.text?.length?.minus(1) ?: 0)

         findViewById<Button>(R.id.btn_next).setOnClickListener{
              if(isValidData())
              {
                  val sharedPreferences: SharedPreferences = this.getSharedPreferences(packageName,
                      Context.MODE_PRIVATE)
                  val editor: SharedPreferences.Editor =  sharedPreferences.edit()

                  editor.putInt("time_interval",mEtTime?.text.toString().toInt())
                  editor.putString("entered_url",mEtUrl?.text.toString())
                  editor.apply()
                  if (ContextCompat.checkSelfPermission(
                          this,
                          Manifest.permission.CAMERA
                      ) != PackageManager.PERMISSION_GRANTED
                  ) {
                      checkPermissionForCameraUse()
                  }
                  else {
                      val intent = Intent(this@MainActivity, UploadImageActivity::class.java)
                      startActivity(intent)
                  }
              }
         }

    }
    private fun checkPermissionForCameraUse() {

        requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)


    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode === 101) {
            for (i in grantResults.indices) {
                val permission = permissions[i]
                isPermitted = grantResults[i] === PackageManager.PERMISSION_GRANTED
                if (grantResults[i] === PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                    val showRationale = shouldShowRequestPermissionRationale(permission)
                    if (!showRationale) {
                        val snack = mEtTime.let { it1 ->
                            Snackbar.make(
                                it1!!,
                                "Please allow camera permission for use the device ",
                                Snackbar.LENGTH_LONG
                            )
                        }
                        snack?.show()
                        //execute when 'never Ask Again' tick and permission dialog not show
                    } else {
                        val snack = mEtTime.let { it1 ->
                            Snackbar.make(
                                it1!!,
                                "Please allow camera permission for use the device ",
                                Snackbar.LENGTH_LONG
                            )
                        }
                        snack?.show()
//                        if (openDialogOnce) {
//                            alertView()
//                        }
                    }
                }
            }
            if (isPermitted) {
                findViewById<Button>(R.id.btn_next)?.performClick()
            }
        }

    }



    private fun isValidData(): Boolean {
         if(!TextUtils.isEmpty(mEtTime?.text.toString()) && !TextUtils.isEmpty(mEtUrl?.text.toString()))
         {

              if(mEtTime?.text.toString().toInt()==0)
                  {
                      Toast.makeText(this,"PLease enter valid time interval ",Toast.LENGTH_LONG).show()
                return false
                  }
              else   if(! URLUtil.isValidUrl(mEtUrl?.text.toString()))
              {
                  Toast.makeText(this,"PLease enter valid URL ",Toast.LENGTH_LONG).show()
                  return false
              }
              return true
         }
         else{
              Toast.makeText(this,"PLease enter time interval and url",Toast.LENGTH_LONG).show()
             return false
         }

    }

}