package com.atidevs.livewords.livebasedtranslation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.atidevs.livewords.common.utils.rotateAndCrop
import com.atidevs.livewords.common.utils.ImageUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executor

class TextAnalyzer(
    private val context: Context,
    lifecycle: Lifecycle,
    executor: Executor,
    private val result: MutableLiveData<String>,
    private val imageCropPercent: MutableLiveData<Pair<Int, Int>>
) : ImageAnalysis.Analyzer {

    private val detector =
        TextRecognition.getClient(
            TextRecognizerOptions.Builder()
                .setExecutor(executor)
                .build()
        )

    init {
        lifecycle.addObserver(detector)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image ?: return

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val imageHeight = image.height
        val imageWidth = image.width

        val actualAspectRatio = imageWidth / imageHeight

        val cropRect = Rect(0, 0, imageWidth, imageHeight)

        val currentCropPercentages = imageCropPercent.value ?: return
        if (actualAspectRatio > 3) {
            val originalHeightCropPercentage = currentCropPercentages.first
            val originalWidthCropPercentage = currentCropPercentages.second
            imageCropPercent.value =
                Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
        }

        val cropPercentages = imageCropPercent.value ?: return
        val heightCropPercent = cropPercentages.first
        val widthCropPercent = cropPercentages.second
        val (widthCrop, heightCrop) = when (rotationDegrees) {
            90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
            else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
        }

        cropRect.inset(
            (imageWidth * widthCrop / 2).toInt(),
            (imageHeight * heightCrop / 2).toInt()
        )

        val imageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(image)
        val croppedBitmap = imageToBitmap.rotateAndCrop(rotationDegrees, cropRect)

        recognizeText(InputImage.fromBitmap(croppedBitmap, 0))
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    // Detect text from camera feed
    private fun recognizeText(inputImage: InputImage): Task<Text> {
        return detector.process(inputImage)
            .addOnSuccessListener { recognizedText ->
                result.value = recognizedText.text
            }
            .addOnFailureListener { exception ->
                Log.e("TextAnalyzer", "Text recognition error", exception)
            }
    }
}