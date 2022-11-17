package com.ouh.brailledetection.model

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName

data class BrailleResponse(
    // TODO
    @SerializedName("추론 데이터")
    val brailleData: List<String>,

    @SerializedName("Bitmap")
    val brailleImage: Bitmap
)