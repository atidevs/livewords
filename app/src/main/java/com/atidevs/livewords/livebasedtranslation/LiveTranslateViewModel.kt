package com.atidevs.livewords.livebasedtranslation

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atidevs.livewords.common.Constants.CropPercent.HEIGHT_CROP_PERCENT
import com.atidevs.livewords.common.Constants.CropPercent.WIDTH_CROP_PERCENT
import com.atidevs.livewords.common.Constants.Throttle.SMOOTHING_DURATION
import com.atidevs.livewords.common.DeferredMutableLiveData
import com.atidevs.livewords.common.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.Executor

class LiveTranslateViewModel : ViewModel() {

    lateinit var executor: Executor

    val imageCropPercent =
        MutableLiveData(ImageCropPercent(HEIGHT_CROP_PERCENT, WIDTH_CROP_PERCENT))

    private lateinit var translator: Translator

    val sourceText: DeferredMutableLiveData<DetectionResult> =
        DeferredMutableLiveData(SMOOTHING_DURATION)

    val sourceLang: MediatorLiveData<Language> = MediatorLiveData()
    val targetLang: MutableLiveData<Language> = MutableLiveData()

    private val translating: MutableLiveData<Boolean> = MutableLiveData()

    val modelDownloading: DeferredMutableLiveData<Boolean> =
        DeferredMutableLiveData(SMOOTHING_DURATION)
    val modelDownloadResult: DeferredMutableLiveData<ModelDownloadResult> =
        DeferredMutableLiveData(SMOOTHING_DURATION)

    private var modelDownloadTask: Task<Void> = Tasks.forCanceled()

    val translationResult: MediatorLiveData<TranslationResult> = MediatorLiveData()

    val availableLanguages: List<Language> = TranslateLanguage.getAllLanguages().map {
        Language(it)
    }

    private val languageIdentifier by lazy {
        LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder()
                .setExecutor(executor)
                .build()
        )
    }

    init {
        modelDownloading.setValue(false)
        translating.value = false

        // Identify language of the detected result from camera feed
        sourceLang.addSource(sourceText) { detectedResult ->
            when (detectedResult) {
                is DetectionResult.Text -> {
                    languageIdentifier.identifyLanguage(detectedResult.text)
                        .addOnSuccessListener { identifiedLangCode ->
                            if (identifiedLangCode != "und") {
                                sourceLang.value = Language(identifiedLangCode)
                            }
                        }
                }
                is DetectionResult.Error -> {

                }
            }

        }

        // Translation task custom OnCompleteListener
        val processTranslation = OnCompleteListener<String> { task ->
            if (task.isSuccessful) {
                translationResult.value = TranslationResult.Success(task.result)
            } else {
                if (task.isCanceled) {
                    return@OnCompleteListener
                }
                translationResult.value = TranslationResult.Error(task.exception)
            }
        }

        // Attach the OnCompleteListener to react to changes on each source
        translationResult.addSource(sourceText) {
            translate().addOnCompleteListener(processTranslation)
        }
        translationResult.addSource(sourceLang) {
            translate().addOnCompleteListener(processTranslation)
        }
        translationResult.addSource(targetLang) {
            translate().addOnCompleteListener(processTranslation)
        }
    }

    // Translate text from source language to target language
    private fun translate(): Task<String> {
        val text = (sourceText.value as? DetectionResult.Text)?.text ?: return Tasks.forResult("")

        val source = sourceLang.value ?: return Tasks.forResult("")
        val target = targetLang.value ?: return Tasks.forResult("")

        val sourceLangCode =
            TranslateLanguage.fromLanguageTag(source.langCode) ?: return Tasks.forCanceled()
        val targetLangCode =
            TranslateLanguage.fromLanguageTag(target.langCode) ?: return Tasks.forCanceled()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .setExecutor(executor)
            .build()

        translator = Translation.getClient(options)

        if (modelDownloading.value != true) {
            modelDownloading.setValue(true)
        }

        modelDownloadTask = translator.downloadModelIfNeeded()
            .addOnCompleteListener {
                modelDownloadResult.setValue(ModelDownloadResult.Success)
                modelDownloading.setValue(false)
            }
            .addOnFailureListener {
                modelDownloadResult.setValue(
                    ModelDownloadResult.Error(exception = it)
                )
                modelDownloading.setValue(false)
            }

        translating.value = true
        return modelDownloadTask.onSuccessTask {
            translator.translate(text)
        }.addOnCompleteListener {
            translating.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        languageIdentifier.close()
        translator.close()
    }
}