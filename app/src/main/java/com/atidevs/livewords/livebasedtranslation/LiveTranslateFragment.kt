package com.atidevs.livewords.livebasedtranslation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.atidevs.livewords.databinding.FragmentLiveTranslateBinding
import com.google.common.util.concurrent.ListenableFuture

class LiveTranslateFragment() : Fragment() {

    private var _binding: FragmentLiveTranslateBinding? = null
    private val binding get() = _binding!!

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var liveTranslateViewModel: LiveTranslateViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLiveTranslateBinding.inflate(inflater, container, false)
        liveTranslateViewModel = ViewModelProvider(this)[LiveTranslateViewModel::class.java]
        initCameraPreview()
        return binding.root
    }

    private fun initCameraPreview() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                CameraSelector.LENS_FACING_BACK
            ).build()

        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}