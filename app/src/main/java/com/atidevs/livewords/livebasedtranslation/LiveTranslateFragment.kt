package com.atidevs.livewords.livebasedtranslation

import android.graphics.PixelFormat
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.atidevs.livewords.R
import com.atidevs.livewords.common.Constants.AspectRatio.RATIO_16_BY_9
import com.atidevs.livewords.common.Constants.AspectRatio.RATIO_4_BY_3
import com.atidevs.livewords.common.ScopedExecutor
import com.atidevs.livewords.common.model.Language
import com.atidevs.livewords.common.model.ModelDownloadResult
import com.atidevs.livewords.common.model.TranslationResult
import com.atidevs.livewords.common.utils.drawOverlay
import com.atidevs.livewords.databinding.FragmentLiveTranslateBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class LiveTranslateFragment : Fragment() {

    private var _binding: FragmentLiveTranslateBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val liveTranslateViewModel: LiveTranslateViewModel by viewModels()

    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var camera: Camera

    private lateinit var cameraExecutor: Executor
    private lateinit var scopedExecutor: ScopedExecutor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveTranslateBinding.inflate(inflater, container, false)

        initCameraPreview()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.surfaceView.apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    liveTranslateViewModel.imageCropPercent.observe(viewLifecycleOwner) { cropPercent ->
                        holder.drawOverlay(requireContext(), cropPercent.height, cropPercent.width)
                    }
                }

                override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

                override fun surfaceDestroyed(p0: SurfaceHolder) {}
            })
        }

        initObservers()
        initLanguageSelector()
    }

    private fun initLanguageSelector() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            liveTranslateViewModel.availableLanguages
        )

        binding.targetLangSpinner.adapter = adapter
        binding.targetLangSpinner.setSelection(adapter.getPosition(Language("en")))
        binding.targetLangSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    liveTranslateViewModel.targetLang.value = adapter.getItem(position)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
    }

    private fun initObservers() {
        liveTranslateViewModel.sourceText.observe(viewLifecycleOwner) {
            binding.sourceText.text = it.result
        }
        liveTranslateViewModel.sourceLang.observe(viewLifecycleOwner) {
            binding.sourceLang.text = it.langName
        }
        liveTranslateViewModel.sourceText.observe(viewLifecycleOwner) {
            binding.sourceText.text = it.result
        }

        liveTranslateViewModel.modelDownloadResult.observe(viewLifecycleOwner) {
            when (it) {
                is ModelDownloadResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.model_downloaded_succes_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is ModelDownloadResult.Error -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.model_downloading_error_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        liveTranslateViewModel.modelDownloading.observe(viewLifecycleOwner) { downloading ->
            if (downloading) {
                binding.targetText.text = getString(R.string.model_downloading_message)
            }
        }

        liveTranslateViewModel.translationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is TranslationResult.Success -> {
                    binding.targetText.text = result.translation
                }
                is TranslationResult.Error -> {
                    binding.targetText.text = getString(R.string.translation_error_message)
                }
            }
        }

        liveTranslateViewModel.modelDownloading.observe(viewLifecycleOwner) { downloading ->
            if (downloading) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun initCameraPreview() {
        cameraExecutor = Executors.newCachedThreadPool()
        scopedExecutor = ScopedExecutor(cameraExecutor)

        liveTranslateViewModel.executor = cameraExecutor

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        val metrics = DisplayMetrics().also { binding.previewView.display.getRealMetrics(it) }

        val screenAspectRatio = getAspectRatio(metrics.widthPixels, metrics.heightPixels)

        val rotation = binding.previewView.display.rotation

        val preview: Preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(
                    scopedExecutor,
                    TextAnalyzer(
                        lifecycle,
                        cameraExecutor,
                        liveTranslateViewModel.sourceText,
                        liveTranslateViewModel.imageCropPercent
                    )
                )
            }

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(
                CameraSelector.LENS_FACING_BACK
            ).build()

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview
            )

            preview.setSurfaceProvider(binding.previewView.surfaceProvider)
        } catch (e: IllegalStateException) {
        }
    }

    private fun getAspectRatio(width: Int, height: Int): Int {
        val previewRatio = ln(max(width, height).toDouble() / min(width, height))
        if (
            abs(previewRatio - ln(RATIO_4_BY_3)) <= abs(previewRatio - ln(RATIO_16_BY_9))
        ) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        scopedExecutor.shutDown()
    }
}