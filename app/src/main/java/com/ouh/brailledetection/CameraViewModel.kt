package com.ouh.brailledetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ouh.brailledetection.model.BrailleResponse
import com.ouh.brailledetection.server.BrailleAPI
import com.ouh.brailledetection.server.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit

class CameraViewModel(
) : ViewModel() {

    private val _brailleImage = MutableLiveData<Bitmap>()
    val brailleImage: LiveData<Bitmap> get() = _brailleImage

    private val _brailleData = MutableLiveData<String>()
    val brailleData: LiveData<String> get() = _brailleData

    var retrofit: Retrofit = RetrofitClient.getInstance()
    var brailleAPI: BrailleAPI = retrofit.create(BrailleAPI::class.java)

    private val _responseUrl = MutableLiveData<String>()
    val responseUrl: LiveData<String> get() = _responseUrl

    init {
        _brailleData.value = "추론 결과 출력"
    }

    fun setImage(bitmap: Bitmap) {
        _brailleImage.value = bitmap
    }

    // 서버에 있는 이미지 가져오기
    fun getImageFromServer(url: String) {
        viewModelScope.launch {
            brailleAPI.requestData(url)
                .enqueue(object : retrofit2.Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            val bitmap = BitmapFactory.decodeStream(response.body()?.byteStream())
                            _brailleImage.value = bitmap
                            Log.d("++getImageFromServer", "$body")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d("++onFailure", t.message.toString())
                    }
                })
        }
    }

    fun sendAddRequest() {
        viewModelScope.launch {
            val fileName = System.currentTimeMillis()
            val bitmapRequestBody: RequestBody =
                BitmapRequestBody(brailleImage.value ?: return@launch)
            val bitmapMultipartBody: MultipartBody.Part =
                MultipartBody.Part.createFormData("image", "$fileName.jpeg", bitmapRequestBody)
            try {
                brailleAPI.postImage(bitmapMultipartBody)
                    .enqueue(object : retrofit2.Callback<BrailleResponse> {
                        override fun onResponse(
                            call: Call<BrailleResponse>,
                            response: Response<BrailleResponse>
                        ) {
                            if (response.isSuccessful) {
                                val body = response.body()
                                _responseUrl.value = "$fileName.jpeg"
                                _brailleData.value =
                                    body?.brailleData?.get("inferenced-chractors").toString()
                                Log.d("++sendAddRequest", "${response.body()}")
                            }
                        }

                        override fun onFailure(call: Call<BrailleResponse>, t: Throwable) {
                            Log.d("++onResponse", "알 수 없는 오류 $t")
                        }
                    })
            } catch (e: Exception) {
                Log.d("++sendAddRequest", "에러 : $e")
                Toast.makeText(MyApplication.instance, "서버에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }

        }
    }

    inner class BitmapRequestBody(private var bitmap: Bitmap) : RequestBody() {
        override fun contentType(): MediaType = "image/jpeg".toMediaType()
        override fun writeTo(sink: BufferedSink) {
            bitmap = Bitmap.createScaledBitmap(bitmap, 1280, 1280, true)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, sink.outputStream())
        }
    }
}
