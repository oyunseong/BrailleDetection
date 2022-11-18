package com.ouh.brailledetection.server

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface BrailleDao {
    @GET("uploader")
    fun requestData(): Call<String>


    @Multipart
    @POST("uploader")
    suspend fun postImage(
//        @Body text: String
        @Part image: MultipartBody.Part
    ): Call<ResponseBody>
}
