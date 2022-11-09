package com.ouh.brailledetection

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ouh.brailledetection.model.Braille

class CameraViewModel : ViewModel() {

    private val _brailleImage = MutableLiveData<Bitmap>()
    val brailleImage: LiveData<Bitmap> get() = _brailleImage

    private val _brailleData = MutableLiveData<Braille>()
    val brailleData: LiveData<Braille> get() = _brailleData

    fun setImage(bitmap: Bitmap) {
        _brailleImage.value = bitmap
    }

    fun setData(braille: Braille) {
        _brailleData.value = braille
    }

}