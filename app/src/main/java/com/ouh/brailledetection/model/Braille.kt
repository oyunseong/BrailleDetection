package com.ouh.brailledetection.model

import com.google.gson.annotations.SerializedName

data class Braille(
    // TODO
    @SerializedName("추론 데이터")
    val brailleData: List<String>,
)