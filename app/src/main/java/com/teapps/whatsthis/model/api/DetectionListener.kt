package com.teapps.whatsthis.model.api

import org.tensorflow.lite.task.vision.detector.Detection

interface DetectionListener {
    fun onError(errorMsg: String)
    fun onResults(results: MutableList<Detection>?, inferenceTime: Long, imageHeight: Int, imageWidth: Int)
}