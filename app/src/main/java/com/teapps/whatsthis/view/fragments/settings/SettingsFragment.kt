package com.teapps.whatsthis.view.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.teapps.whatsthis.databinding.FragmentSettingsBinding
import com.teapps.whatsthis.model.api.ObjectDetectionHandler

class SettingsFragment : Fragment() {
    /**VIEW**/
    private var _binding: FragmentSettingsBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    /**VIEWMODEL**/
    private var settingsViewModel : SettingsViewModel? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        initializeUIComponents()
        return root
    }

    private fun initializeUIComponents(){
        //Load current settings values
        binding.settingsTxtThreshold.text = settingsViewModel!!.detectionSettings!!.threshold.toString()
        binding.settingsTxtMaxResults.text = settingsViewModel!!.detectionSettings!!.maxResults.toString()
        binding.settingsTxtNumOfThreads.text = settingsViewModel!!.detectionSettings!!.numberOfThreads.toString()

        binding.settingsSpinnerDelegation.setSelection((settingsViewModel!!.detectionSettings!!.currentDelegation.hardwareNumber.toInt()))
        binding.settingsSpinnerMLModel.setSelection((settingsViewModel!!.detectionSettings!!.currentModel.modelNumber.toInt()))

        //Adjust button functionalities
        binding.settingsBtnThresholdDown.setOnClickListener {
            var threshold = (binding.settingsTxtThreshold.text.toString()).toFloat()
            //threshold = ((threshold * 10.0f).roundToInt() / 10.0f)
            if(threshold > 0.1f){
                threshold -= 0.1f
                binding.settingsTxtThreshold.text = String.format("%.1f", threshold)
            }
        }
        binding.settingsBtnThresholdUp.setOnClickListener {
            var threshold = (binding.settingsTxtThreshold.text.toString()).toFloat()
            //threshold = ((threshold * 10.0f).roundToInt() / 10.0f)
            if(threshold < 0.9f){
                threshold += 0.1f
                binding.settingsTxtThreshold.text = String.format("%.1f", threshold)
            }
        }
        binding.settingsBtnMaxResDown.setOnClickListener {
            var maxres = (binding.settingsTxtMaxResults.text.toString()).toInt()
            if(maxres > 1){
                maxres--
                binding.settingsTxtMaxResults.text = maxres.toString()
            }
        }
        binding.settingsBtnMaxResUp.setOnClickListener {
            var maxres = (binding.settingsTxtMaxResults.text.toString()).toInt()
            if(maxres < 4){
                maxres++
                binding.settingsTxtMaxResults.text = maxres.toString()
            }
        }
        binding.settingsBtnNumOfThreadsDown.setOnClickListener {
            var numofthreads = (binding.settingsTxtNumOfThreads.text.toString()).toInt()
            if(numofthreads > 1){
                numofthreads--
                binding.settingsTxtNumOfThreads.text = numofthreads.toString()
            }
        }
        binding.settingsBtnNumOfThreadsUp.setOnClickListener {
            var numofthreads = (binding.settingsTxtNumOfThreads.text.toString()).toInt()
            if(numofthreads < 4){
                numofthreads++
                binding.settingsTxtNumOfThreads.text = numofthreads.toString()
            }
        }

        //Apply settings
        binding.settingsBtnApply.setOnClickListener {
            var delegation = ObjectDetectionHandler.ObjectHandlingDelegation.DELEGATE_GPU
            when(binding.settingsSpinnerDelegation.selectedItemPosition){
                0 -> delegation = ObjectDetectionHandler.ObjectHandlingDelegation.DELEGATE_CPU
                1 -> delegation = ObjectDetectionHandler.ObjectHandlingDelegation.DELEGATE_GPU
                2 -> delegation = ObjectDetectionHandler.ObjectHandlingDelegation.DELEGATE_NNAPI
            }

            var model = ObjectDetectionHandler.ObjectHandlingModel.MODEL_MOBILENETV1
            when(binding.settingsSpinnerMLModel.selectedItemPosition){
                0 -> model = ObjectDetectionHandler.ObjectHandlingModel.MODEL_MOBILENETV1
                1 -> model = ObjectDetectionHandler.ObjectHandlingModel.MODEL_EFFICIENTDETV0
                2 -> model = ObjectDetectionHandler.ObjectHandlingModel.MODEL_EFFICIENTDETV1
                3 -> model = ObjectDetectionHandler.ObjectHandlingModel.MODEL_EFFICIENTDETV2
            }

            settingsViewModel!!.applySettings(
                binding.settingsTxtThreshold.text.toString().toFloat(),
                binding.settingsTxtNumOfThreads.text.toString().toInt(),
                binding.settingsTxtMaxResults.text.toString().toInt(),
                delegation,
                model
            )
            Toast.makeText(context, "Settings applied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}