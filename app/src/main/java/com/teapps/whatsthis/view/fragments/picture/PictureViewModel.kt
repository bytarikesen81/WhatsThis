package com.teapps.whatsthis.view.fragments.picture

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teapps.whatsthis.model.entities.DetectionSettings

class PictureViewModel : ViewModel() {
    val detectionSettings : DetectionSettings?
    lateinit var currentImage : Bitmap

    init{
        detectionSettings = DetectionSettings.Companion.instance
    }
}