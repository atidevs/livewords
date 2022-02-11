package com.atidevs.livewords.livebasedtranslation

import android.graphics.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.atidevs.livewords.R
import com.atidevs.livewords.common.ScopedExecutor
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

    companion object {
        const val RATIO_4_BY_3 = 4.0 / 3.0
        const val RATIO_16_BY_9 = 16.0 / 9.0
        const val WIDTH_CROP_PERCENT = 8
        const val HEIGHT_CROP_PERCENT = 74
    }

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
                    liveTranslateViewModel.imageCropPercentages.observe(viewLifecycleOwner) {
                        drawOverlay(holder, it.first, it.second)
                    }
                }

                override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

                override fun surfaceDestroyed(p0: SurfaceHolder) {}
            })
        }

        init()
    }

    private fun init() {
        liveTranslateViewModel.identifyLanguage()
        liveTranslateViewModel.sourceText.observe(viewLifecycleOwner) {
            binding.sourceText.text = it
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
                        requireContext(),
                        lifecycle,
                        cameraExecutor,
                        liveTranslateViewModel.sourceText
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

    private fun drawOverlay(
        holder: SurfaceHolder,
        heightCropPercent: Int,
        widthCropPercent: Int
    ) {
        val canvas = holder.lockCanvas()
        val backgroundPaint = Paint().apply {
            alpha = 120
        }

        canvas.drawPaint(backgroundPaint)

        val rectPaint = Paint()
        rectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = Color.WHITE

        val outlinePaint = Paint()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.color = Color.WHITE
        outlinePaint.strokeWidth = 5f

        val surfaceWidth = holder.surfaceFrame.width()
        val surfaceHeight = holder.surfaceFrame.height()

        val cornerRadius = 25f
        val recTop = surfaceHeight * heightCropPercent / 2 / 100f
        val rectLeft = surfaceWidth * widthCropPercent / 2 / 100f
        val rectRight = surfaceWidth * (1 - widthCropPercent / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - heightCropPercent / 2 / 100f)
        val rect = RectF(rectLeft, recTop, rectRight, rectBottom)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rectPaint)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, outlinePaint)

        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 50F

        val overlayText = getString(R.string.center_text_label)
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textX = (surfaceWidth - textBounds.width()) / 2f
        val textY = rectBottom + textBounds.height() + 15f

        canvas.drawText(overlayText, textX, textY, textPaint)
        holder.unlockCanvasAndPost(canvas)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        scopedExecutor.shutDown()
    }
}