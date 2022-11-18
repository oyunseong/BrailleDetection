package com.ouh.brailledetection

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ouh.brailledetection.domain.Braille
import com.ouh.brailledetection.model.BrailleResponse
import com.ouh.brailledetection.server.BrailleAPI
import com.ouh.brailledetection.server.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import retrofit2.*

class CameraViewModel(
) : ViewModel() {

    private val _brailleImage = MutableLiveData<Bitmap>()
    val brailleImage: LiveData<Bitmap> get() = _brailleImage

    private val _brailleData = MutableLiveData<String>()
    val brailleData: LiveData<String> get() = _brailleData

    var retrofit: Retrofit = RetrofitClient.getInstance()
    var brailleAPI: BrailleAPI = retrofit.create(BrailleAPI::class.java)

    init {
        _brailleData.value = "추론 결과 출력"
    }

    fun setImage(bitmap: Bitmap) {
        _brailleImage.value = bitmap
    }

    fun setBrailleData(value: String) {
        _brailleData.value = value
    }

    fun sendAddRequest() {
        viewModelScope.launch {
            val bitmapRequestBody: RequestBody =
                BitmapRequestBody(brailleImage.value ?: return@launch)
            val bitmapMultipartBody: MultipartBody.Part =
                MultipartBody.Part.createFormData("image", "a1", bitmapRequestBody)
            try {
                brailleAPI.postImage(bitmapMultipartBody)
                    .enqueue(object : retrofit2.Callback<ResponseBody> {
                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            Log.d("sendAddRequest", "$response")
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.d("++onResponse", "알 수 없는 오류 $t")
                        }
                    })
            } catch (e: Exception) {
                Toast.makeText(MyApplication.instance, "서버에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }

        }
    }

    fun getResult() {
        viewModelScope.launch {
            brailleAPI.requestData().enqueue(object : retrofit2.Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("결과", "$body")
                        _brailleData.value = body.toString()
                    } else {
                        Log.d("++onResponse", "알 수 없는 오류 $response")
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Toast.makeText(MyApplication.instance, "연결 실패", Toast.LENGTH_SHORT).show()
                    Log.d("++onFailure", t.toString())
                }
            })
        }
    }

    inner class BitmapRequestBody(private val bitmap: Bitmap) : RequestBody() {
        override fun contentType(): MediaType = "image/jpeg".toMediaType()
        override fun writeTo(sink: BufferedSink) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 99, sink.outputStream())
        }
    }
}
