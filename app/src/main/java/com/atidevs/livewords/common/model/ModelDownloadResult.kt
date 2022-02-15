package com.atidevs.livewords.common.model

sealed class ModelDownloadResult() {
    object Success : ModelDownloadResult()
    data class Error(val exception: Exception): ModelDownloadResult()
}