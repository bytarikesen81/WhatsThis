package com.teapps.whatsthis.view.fragments.settings

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teapps.whatsthis.model.api.ObjectDetectionHandler
import com.teapps.whatsthis.model.entities.DetectionSettings

class SettingsViewModel : ViewModel() {
    val detectionSettings : DetectionSettings?
    lateinit var currentImage : Bitmap

    init{
        detectionSettings = DetectionSettings.Companion.instance
    }

    fun applySettings(
        threshold:Float,
        numberOfThreads: Int,
        maxResults: Int,
        currentDelegation: ObjectDetectionHandler.ObjectHandlingDelegation,
        currentModel: ObjectDetectionHandler.ObjectHandlingModel) {
        detectionSettings!!.numberOfThreads = numberOfThreads
        detectionSettings!!.maxResults = maxResults
        detectionSettings!!.currentDelegation = currentDelegation
        detectionSettings!!.currentModel = currentModel
    }
}