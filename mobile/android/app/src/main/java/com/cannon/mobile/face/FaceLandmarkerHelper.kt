package com.cannon.mobile.face

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FaceLandmarkerHelper(
    val context: Context,
    val landmarkerListener: LandmarkerListener? = null
) {
    private var faceLandmarker: FaceLandmarker? = null

    init {
        setupFaceLandmarker()
    }

    fun clearFaceLandmarker() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    private fun setupFaceLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")

        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setNumFaces(1)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)

            val options = optionsBuilder.build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            landmarkerListener?.onError("Face Landmarker failed to initialize: ${e.message}")
            Log.e("FaceLandmarkerHelper", "Face Landmarker failed to load model: ${e.message}")
        } catch (e: RuntimeException) {
            landmarkerListener?.onError("Face Landmarker failed to initialize: ${e.message}")
            Log.e("FaceLandmarkerHelper", "Face Landmarker failed to load model: ${e.message}")
        }
    }

    fun detectLiveStream(bitmap: Bitmap) {
        if (faceLandmarker == null) return

        val frameTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(bitmap).build()

        faceLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(
        result: FaceLandmarkerResult,
        input: MPImage
    ) {
        landmarkerListener?.onResults(result)
    }

    private fun returnLivestreamError(error: RuntimeException) {
        landmarkerListener?.onError(error.message ?: "An unknown error has occurred")
    }

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(result: FaceLandmarkerResult)
    }
}