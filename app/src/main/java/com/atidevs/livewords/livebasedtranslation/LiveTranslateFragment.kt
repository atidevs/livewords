package com.atidevs.livewords.livebasedtranslation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class LiveTranslateFragment() : Fragment() {

    private var _binding: FragmentLiveTranslateBinding? = null
    private val binding get() = _binding!!

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var liveTranslateViewModel: LiveTranslateViewModel? = null

    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var detector: TextRecognizer

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
        initTextRecognition()
        initCameraPreview()
        return binding.root
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

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
            val image = imageProxy.image
            if (image != null) {
                val inputImage =
                    InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                recognizeText(inputImage)
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
                    .addOnFailureListener {
                        imageProxy.close()
                    }
            }
        }

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

    private fun initTextRecognition() {
        detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        lifecycle.addObserver(detector)
    }

    private fun recognizeText(image: InputImage): Task<Text> {
        return detector.process(image)
            .addOnSuccessListener {
                binding.sourceText.text = it.text
            }
            .addOnFailureListener { exception ->
                Log.e("LiveTranslationFragment", "Text recognition error", exception)
                // Handle error message!
            }
    }
}