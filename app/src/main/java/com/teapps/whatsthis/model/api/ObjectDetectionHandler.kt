package com.teapps.whatsthis.model.api

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.lang.IllegalStateException

class ObjectDetectionHandler(
    var threshold: Float = 0.5f,
    var numberOfThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: ObjectHandlingDelegation = ObjectHandlingDelegation.DELEGATE_CPU,
    var currentModel: ObjectHandlingModel = ObjectHandlingModel.MODEL_EFFICIENTDETV0,
    val context: Context,
    val objectDetectionListener: DetectionListener?
)
{
    private var objectDetector: ObjectDetector? = null

    init{
        setupObjectDetector()
    }

    fun clearObjectDetector(){
        this.objectDetector = null
    }

    fun setupObjectDetector(){
        /**BUILD DETECTOR OPTIONS**/
        //Create the options builder and set threshold and max results options
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(this.threshold)
            .setMaxResults(this.maxResults)

        //Create the base options builder and set number of threads option
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(this.numberOfThreads)

        //Select a specified hardware for running the model(Default: CPU)
        when(this.currentDelegate){
            ObjectHandlingDelegation.DELEGATE_CPU ->{
                //Use CPU
            }
            ObjectHandlingDelegation.DELEGATE_GPU ->{
                if(CompatibilityList().isDelegateSupportedOnThisDevice) baseOptionsBuilder.useGpu()
                else objectDetectionListener?.onError("GPU support not found on this device")
            }

            ObjectHandlingDelegation.DELEGATE_NNAPI ->{
                baseOptionsBuilder.useNnapi()
            }
        }

        //Set the model name depending on the model used for the detection
        val modelName = when(this.currentModel){
                            ObjectHandlingModel.MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
                            ObjectHandlingModel.MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
                            ObjectHandlingModel.MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
                            ObjectHandlingModel.MODEL_EFFICIENTDETV2 ->  "efficientdet-lite2.tflite"
                        }

        //Configure the base options
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        /**GENERATE DETECTOR WITH OPTIONS**/
        /*
          Try generating object detector for the handler used in the specific context
          with the selected model and options
        */
        try{
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        }catch(e: IllegalStateException){
            objectDetectionListener?.onError("Object detector failed to initialize: " + e.message)
            Log.e("DETECTION_FAILED", "Failed to load model with error:"+e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int){
        //Control if the object detector is null or not. If it's null then setup it
        if(this.objectDetector == null) setupObjectDetector()

        // Get inference time is the difference between the system time at the start and finish of the process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image to be captured
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        //Preprocess the image and convert it into a TensorImage for detection
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        //Detect objects in the image by using the object detector structured with the setup options
        val objectResults = objectDetector?.detect(tensorImage)

        //Calculate the elapsed time for the detection process
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        //Get and process the detection results
        objectDetectionListener?.onResults(objectResults, inferenceTime, tensorImage.height, tensorImage.width)
    }

    enum class ObjectHandlingDelegation(val hardwareNumber: Short){
        DELEGATE_CPU(0x0000),
        DELEGATE_GPU(0x0001),
        DELEGATE_NNAPI(0x0002)
    }

    enum class ObjectHandlingModel(val modelNumber:Short){
        MODEL_MOBILENETV1(0),
        MODEL_EFFICIENTDETV0(1),
        MODEL_EFFICIENTDETV1(2),
        MODEL_EFFICIENTDETV2(3)
    }
}