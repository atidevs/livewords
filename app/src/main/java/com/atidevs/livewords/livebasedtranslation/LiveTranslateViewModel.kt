package com.atidevs.livewords.livebasedtranslation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.atidevs.livewords.livebasedtranslation.LiveTranslateFragment.Companion.HEIGHT_CROP_PERCENT
import com.atidevs.livewords.livebasedtranslation.LiveTranslateFragment.Companion.WIDTH_CROP_PERCENT
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import java.util.concurrent.Executor

class LiveTranslateViewModel : ViewModel() {

    lateinit var executor: Executor

    val imageCropPercentages = MutableLiveData<Pair<Int, Int>>()
        .apply { value = Pair(HEIGHT_CROP_PERCENT, WIDTH_CROP_PERCENT) }

    private var languageIdentifier =
        LanguageIdentification.getClient(LanguageIdentificationOptions.Builder().build())

    private var _sourceText: MutableLiveData<String> = MutableLiveData()
    var sourceText: MutableLiveData<String> = _sourceText

    fun identifyLanguage() {
        _sourceText = Transformations.switchMap(sourceText) { text ->
            val result = MutableLiveData<String>()
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    if (languageCode != "und") {
                        result.value = languageCode
                    }
                }
            result
        } as MutableLiveData<String>
    }

    override fun onCleared() {
        super.onCleared()
        languageIdentifier.close()
    }
}