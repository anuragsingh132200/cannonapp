package com.cannon.mobile.face

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FaceMeshOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var result: FaceLandmarkerResult? = null
    private val meshPaint = Paint().apply {
        color = 0x6600E5FF.toInt() // 40% opacity Cyan
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val pointPaint = Paint().apply {
        color = 0x6600E5FF.toInt() // 40% opacity Cyan
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun updateResult(newResult: FaceLandmarkerResult?) {
        result = newResult
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = result ?: return
        if (r.faceLandmarks().isEmpty()) return
        val landmarks = r.faceLandmarks()[0]
        val w = width.toFloat()
        val h = height.toFloat()

        for (conn in FaceLandmarker.FACE_LANDMARKS_TESSELATION) {
            val p1 = landmarks[conn.start()]
            val p2 = landmarks[conn.end()]
            canvas.drawLine(p1.x() * w, p1.y() * h, p2.x() * w, p2.y() * h, meshPaint)
        }
        
        for (p in landmarks) {
            canvas.drawCircle(p.x() * w, p.y() * h, 1.5f, pointPaint)
        }
    }
}