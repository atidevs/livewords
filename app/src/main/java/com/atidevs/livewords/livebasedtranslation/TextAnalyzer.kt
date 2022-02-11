package com.atidevs.livewords.livebasedtranslation

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
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
    private val result: MutableLiveData<String>
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
        val inputImage =
            InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

        recognizeText(inputImage)
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