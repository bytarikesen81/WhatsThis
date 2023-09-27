package com.teapps.whatsthis.view.fragments.picture

import android.R.attr
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.teapps.whatsthis.databinding.FragmentPictureBinding
import com.teapps.whatsthis.model.api.DetectionListener
import com.teapps.whatsthis.model.api.ObjectDetectionHandler
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PictureFragment : Fragment(), DetectionListener {
    companion object {
        val IMAGE_REQUEST_CODE = 0x64
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private var _binding: FragmentPictureBinding? = null
    private val binding get() = _binding!!

    private var pictureViewModel:PictureViewModel ?= null


    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var objectDetectionHandler: ObjectDetectionHandler

    /** VIEW CALLBACKS **/
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        pictureViewModel = ViewModelProvider(this)[PictureViewModel::class.java]

        _binding = FragmentPictureBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initializeUIComponents()
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
    }

    /** ACTIVITY LIFECYCLE CALLBACKS **/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK){
            binding.pictureImgView.setImageURI(data?.data)
            pictureViewModel!!.detectionSettings!!.selectedPictureUri = data?.data

            pictureViewModel!!.currentImage = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, data?.data)
            objectDetectionHandler.detect(pictureViewModel!!.currentImage, 0)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = _binding!!.pictureImgView.display.rotation
    }



    /** FRAGMENT FUNCTIONS **/
    /*Initialization*/
    //General view initialization
    private fun initializeUIComponents(){
        if(pictureViewModel!!.detectionSettings!!.selectedPictureUri != null) binding.pictureImgView.setImageURI(pictureViewModel!!.detectionSettings!!.selectedPictureUri)
        binding.pictureBtnUpload.setOnClickListener {
            pickImageGallery()
        }
    }

    /*Image picking*/
    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    /*Detector Operations*/
    private fun setUpHandler(){
        objectDetectionHandler.threshold = pictureViewModel!!.detectionSettings!!.threshold
        objectDetectionHandler.maxResults = pictureViewModel!!.detectionSettings!!.maxResults
        objectDetectionHandler.numberOfThreads = pictureViewModel!!.detectionSettings!!.numberOfThreads
        objectDetectionHandler.currentDelegate = pictureViewModel!!.detectionSettings!!.currentDelegation
        objectDetectionHandler.currentModel = pictureViewModel!!.detectionSettings!!.currentModel
        objectDetectionHandler.setupObjectDetector()
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
            _binding!!.overlayImg.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Force a redraw
            _binding!!.overlayImg.invalidate()
        }
    }

    override fun onError(errorMsg: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        }
    }
}