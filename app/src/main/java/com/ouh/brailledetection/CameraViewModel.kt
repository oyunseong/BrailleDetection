package com.ouh.brailledetection

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private val _braille = MutableLiveData<Bitmap>()
    val braille: LiveData<Bitmap> get() = _braille

    fun setImage(bitmap: Bitmap) {
        _braille.value = bitmap
    }

}