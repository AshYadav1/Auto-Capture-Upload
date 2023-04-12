package com.app.autocaptureandupload.models
import com.google.gson.annotations.SerializedName



data class ReadQrCodeResponse (


  @SerializedName("errors"  ) var errors  : String?  = null,
  @SerializedName("message" ) var message : String?  = null,
  @SerializedName("status"  ) var status  : String?  = null,
  @SerializedName("success" ) var success : Boolean? = null

)