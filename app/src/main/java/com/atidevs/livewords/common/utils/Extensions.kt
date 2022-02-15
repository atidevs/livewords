package com.atidevs.livewords.common.utils

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import com.atidevs.livewords.R

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

// Draw guiding rectangle on the surface
// This will guide the user to center to desired text to translate!
fun SurfaceHolder.drawOverlay(
    context: Context,
    heightCropPercent: Int,
    widthCropPercent: Int
) {
    val canvas = lockCanvas()
    val backgroundPaint = Paint().apply {
        alpha = 120
    }

    canvas.drawPaint(backgroundPaint)

    val rectPaint = Paint()
    rectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    rectPaint.style = Paint.Style.FILL
    rectPaint.color = Color.WHITE

    val outlinePaint = Paint()
    outlinePaint.style = Paint.Style.STROKE
    outlinePaint.color = Color.WHITE
    outlinePaint.strokeWidth = 5f

    val surfaceWidth = surfaceFrame.width()
    val surfaceHeight = surfaceFrame.height()

    val cornerRadius = 25f
    val recTop = surfaceHeight * heightCropPercent / 2 / 100f
    val rectLeft = surfaceWidth * widthCropPercent / 2 / 100f
    val rectRight = surfaceWidth * (1 - widthCropPercent / 2 / 100f)
    val rectBottom = surfaceHeight * (1 - heightCropPercent / 2 / 100f)
    val rect = RectF(rectLeft, recTop, rectRight, rectBottom)

    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rectPaint)

    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, outlinePaint)

    val textPaint = Paint()
    textPaint.color = Color.WHITE
    textPaint.textSize = 50F

    val overlayText = context.getString(R.string.center_text_label)
    val textBounds = Rect()
    textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
    val textX = (surfaceWidth - textBounds.width()) / 2f
    val textY = rectBottom + textBounds.height() + 15f

    canvas.drawText(overlayText, textX, textY, textPaint)
    unlockCanvasAndPost(canvas)
}
