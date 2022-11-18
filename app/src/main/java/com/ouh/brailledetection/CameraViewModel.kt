package com.ouh.brailledetection

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ouh.brailledetection.domain.Braille
import com.ouh.brailledetection.model.BrailleResponse
import com.ouh.brailledetection.server.BrailleDao
import com.ouh.brailledetection.server.BrailleRepository
import com.ouh.brailledetection.server.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import retrofit2.awaitResponse

class CameraViewModel(
) : ViewModel() {

    private val _brailleImage = MutableLiveData<Bitmap>()
    val brailleImage: LiveData<Bitmap> get() = _brailleImage

    private val _data = MutableLiveData<Braille>()
    val data: LiveData<Braille> get() = _data

    private val _bos = MutableLiveData<ByteArray>()
    val bos: LiveData<ByteArray> get() = _bos

    private val _brailleData = MutableLiveData<String>()
    val brailleData: LiveData<String> get() = _brailleData
    lateinit var repository: BrailleRepository
    lateinit var dao: BrailleDao

    init {
        repository = BrailleRepository(dao)
//        repository = BrailleRepository(dao)
        _brailleData.value = "추론 결과 출력"
    }

    fun setImage(bitmap: Bitmap) {
        _brailleImage.value = bitmap
    }

    fun setImageUri(data: Braille) {
        _data.value = data
    }

    fun setBos(data: ByteArray) {
        _bos.value = data
    }

    fun setData(braille: BrailleResponse) {
        _brailleData.value = braille.toString()
    }

    fun sendAddRequest() {
        viewModelScope.launch {
            val bitmapRequestBody: RequestBody =
                BitmapRequestBody(brailleImage.value ?: return@launch)
            val bitmapMultipartBody: MultipartBody.Part =
                MultipartBody.Part.createFormData("image", "a1", bitmapRequestBody)
            val response = repository.postData(bitmapMultipartBody).awaitResponse()
            Log.d("++response", "$response")
        }
    }

    inner class BitmapRequestBody(private val bitmap: Bitmap) : RequestBody() {
        override fun contentType(): MediaType = "image/jpeg".toMediaType()
        override fun writeTo(sink: BufferedSink) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 99, sink.outputStream())
        }
    }

    private fun String?.toPlainRequestBody() =
        requireNotNull(this).toRequestBody("text/plain".toMediaTypeOrNull())
}
