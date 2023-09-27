package com.teapps.whatsthis.view.fragments.camera

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teapps.whatsthis.model.entities.DetectionSettings

class CameraViewModel : ViewModel() {
    val detectionSettings : DetectionSettings?
    lateinit var currentImage : Bitmap

    init{
        detectionSettings = DetectionSettings.Companion.instance
    }
}