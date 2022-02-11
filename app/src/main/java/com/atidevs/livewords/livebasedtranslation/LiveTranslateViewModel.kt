package com.atidevs.livewords.livebasedtranslation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions

class LiveTranslateViewModel : ViewModel() {

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