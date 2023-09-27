package com.teapps.whatsthis.view.fragments.camera

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.teapps.whatsthis.R
import com.teapps.whatsthis.databinding.FragmentCameraBinding
import com.teapps.whatsthis.model.api.DetectionListener
import com.teapps.whatsthis.model.api.ObjectDetectionHandler
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), DetectionListener{
    // This property is only valid between onCreateView and onDestroyView
    private var _binding: FragmentCameraBinding? = null

    private var cameraViewModel:CameraViewModel? = null

    private lateinit var objectDetectionHandler: ObjectDetectionHandler
    private lateinit var imageBuffer: Bitmap
    private var cameraPreview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /** VIEW CALLBACKS **/
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.cameraViewModel = ViewModelProvider(this)[CameraViewModel::class.java]

        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = _binding!!.root
        return root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        //Setup the object detection handler depending on the settings
        objectDetectionHandler = ObjectDetectionHandler(
            context = requireContext(),
            objectDetectionListener = this
        )
        setUpHandler()

        //Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        _binding!!.viewFinder.post{
            //Setup the camera hardware and its use cases
            setUpCamera()
        }
    }


    /** ACTIVITY LIFECYCLE CALLBACKS **/
    override fun onResume(){
        super.onResume()
        if (!CameraPermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                .navigate(CameraFragmentDirections.actionNavCameraToNavPermissions())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = _binding!!.viewFinder.display.rotation
    }


    /** FRAGMENT FUNCTIONS **/
    /*Camera operations*/
    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                //CameraProvider
                cameraProvider = cameraProviderFuture.get()

                //Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        cameraPreview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(_binding!!.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(_binding!!.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::imageBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            imageBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }
                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            cameraPreview?.setSurfaceProvider(_binding!!.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /*Detector operations*/
    private fun setUpHandler(){
        objectDetectionHandler.threshold = cameraViewModel!!.detectionSettings!!.threshold
        objectDetectionHandler.maxResults = cameraViewModel!!.detectionSettings!!.maxResults
        objectDetectionHandler.numberOfThreads = cameraViewModel!!.detectionSettings!!.numberOfThreads
        objectDetectionHandler.currentDelegate = cameraViewModel!!.detectionSettings!!.currentDelegation
        objectDetectionHandler.currentModel = cameraViewModel!!.detectionSettings!!.currentModel
        objectDetectionHandler.setupObjectDetector()
    }

    //Detect and mark the objects
    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { imageBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectionHandler.detect(imageBuffer, imageRotation)
    }

    /** OBJECT DETECTION CALLBACKS **/
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread{
            // Pass necessary information to OverlayView for drawing on the canvas
            _binding!!.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )
            // Force a redraw
            _binding!!.overlay.invalidate()
        }
    }

    override fun onError(errorMsg: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        }
    }



    /** DEPRECATED FUNCTIONS**/
    /* [DEPRECATED]
    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // When clicked, decrease the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset detector. [DEPRECATED
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()
    }
     */
}