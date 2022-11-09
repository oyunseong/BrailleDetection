package com.ouh.brailledetection.server

import com.ouh.brailledetection.model.Braille
import retrofit2.Call
import retrofit2.http.GET

interface BrailleAPI {
    @GET("경로")
    fun getBrailleData(): Call<Braille>
}
