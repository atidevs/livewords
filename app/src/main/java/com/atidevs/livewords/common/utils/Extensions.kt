package com.atidevs.livewords.common.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect

fun Bitmap.rotateAndCrop(
    rotationDegrees: Int,
    cropRect: Rect
): Bitmap {
    val matrix = Matrix()
    matrix.preRotate(rotationDegrees.toFloat())
    return Bitmap.createBitmap(
        this,
        cropRect.left,
        cropRect.top,
        cropRect.width(),
        cropRect.height(),
        matrix,
        true
    )
}
