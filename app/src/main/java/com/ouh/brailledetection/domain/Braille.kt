package com.ouh.brailledetection.domain

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Braille(
    val image: Uri?
) : Parcelable