package com.teapps.whatsthis.model.entities

import android.net.Uri
import com.teapps.whatsthis.model.api.ObjectDetectionHandler.ObjectHandlingDelegation
import com.teapps.whatsthis.model.api.ObjectDetectionHandler.ObjectHandlingModel

class DetectionSettings
/**METHODS */ /*Dynamic Methods*/ //Constructors
private constructor(
    /**FIELDS */ /*Private Fields*/
    var threshold: Float, //Getters and Setters
    var numberOfThreads: Int,
    var maxResults: Int,
    var currentDelegation: ObjectHandlingDelegation,
    var currentModel: ObjectHandlingModel
) {

    //Selected Image
    var selectedPictureUri: Uri? = null

    companion object {
        /*Static Methods*/
        /*Static Fields*/
        var instance: DetectionSettings? = null
            get() {
                if (field == null) {
                    field = DetectionSettings(
                        0.5f,
                        2,
                        3,
                        ObjectHandlingDelegation.DELEGATE_CPU,
                        ObjectHandlingModel.MODEL_EFFICIENTDETV0
                    )
                }
                return field
            }
            private set
    }
}