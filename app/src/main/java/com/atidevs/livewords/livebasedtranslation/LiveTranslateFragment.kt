package com.atidevs.livewords.livebasedtranslation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.atidevs.livewords.databinding.FragmentLiveTranslateBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class LiveTranslateFragment() : Fragment() {

    private var _binding: FragmentLiveTranslateBinding? = null
    private val binding get() = _binding!!

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var liveTranslateViewModel: LiveTranslateViewModel

    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var cameraExecutor: Executor

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
        init()
        return binding.root
    }

    private fun init() {
        cameraExecutor = Executors.newCachedThreadPool()
        liveTranslateViewModel.identifyLanguage()
        liveTranslateViewModel.sourceText.observe(viewLifecycleOwner) {
            binding.sourceLang.text = it
        }
    }

    private fun initCameraPreview() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                CameraSelector.LENS_FACING_BACK
            ).build()

        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        imageAnalysis =
            ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(requireContext()),
            TextAnalyzer(
                requireContext(),
                lifecycle,
                cameraExecutor,
                liveTranslateViewModel.sourceText
            )
        )

        val camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}