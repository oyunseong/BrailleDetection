package com.ouh.brailledetection.server

import com.ouh.brailledetection.model.BrailleResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface BrailleAPI {
    @GET("Desktop/imprintList.json")
    fun requestData(): Call<String>

    @Multipart
    @POST("uploader")
    fun postImage(
        @Part image: MultipartBody.Part
    ): Call<BrailleResponse>
}
