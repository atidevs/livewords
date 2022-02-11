package com.atidevs.livewords.common.model

import java.util.*

data class Language(
    val langCode: String
) {
    val langName: String
        get() = Locale(langCode).displayName
}