package com.atidevs.livewords.common.model

sealed class DetectionResult(val result: String) {
    data class Text(val text: String) : DetectionResult(text)
    data class Error(val error: String) : DetectionResult(error)
}