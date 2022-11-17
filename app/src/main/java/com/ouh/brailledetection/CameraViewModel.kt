package com.ouh.brailledetection

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ouh.brailledetection.domain.Braille
import com.ouh.brailledetection.model.BrailleResponse

class CameraViewModel : ViewModel() {

    private val _brailleImage = MutableLiveData<Bitmap>()
    val brailleImage: LiveData<Bitmap> get() = _brailleImage

    private val _data = MutableLiveData<Braille>()
    val data: LiveData<Braille> get() = _data

    private val _bos = MutableLiveData<ByteArray>()
    val bos: LiveData<ByteArray> get() = _bos

    private val _brailleData = MutableLiveData<String>()
    val brailleData: LiveData<String> get() = _brailleData

    init {
        _brailleData.value = "ABCDE FF"
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

}
