package com.ouh.brailledetection.model

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName

data class BrailleResponse(
    @SerializedName("result")
    val brailleData: Map<String, String>,
)