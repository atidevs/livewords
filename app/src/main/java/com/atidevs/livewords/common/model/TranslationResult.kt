package com.atidevs.livewords.common.model

import java.lang.Exception

sealed class TranslationResult {
    data class Success(val translation: String?) : TranslationResult()
    data class Error(val error: Exception?) : TranslationResult()
}
