package com.ouh.brailledetection.model

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName
import com.squareup.okhttp.ResponseBody

data class BrailleResponse(
    @SerializedName("result")
    val brailleData: Map<String, String>,

    @SerializedName("responsebody")
    val brailleRequest: ResponseBody
)