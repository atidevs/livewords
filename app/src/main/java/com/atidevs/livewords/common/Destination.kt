package com.atidevs.livewords.common

sealed class Destination {
    object LiveTranslationScreen : Destination()
    object TextTranslationScreen : Destination()
}
