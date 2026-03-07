package com.cannon.mobile.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceMeshCameraView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs), FaceLandmarkerHelper.LandmarkerListener {

  private val previewView = PreviewView(context)
  private val overlayView = FaceMeshOverlayView(context)
  private var backgroundExecutor: ExecutorService? = null
  private var faceLandmarkerHelper: FaceLandmarkerHelper? = null

  private var cameraProvider: ProcessCameraProvider? = null
  private var preview: Preview? = null
  private var imageAnalyzer: ImageAnalysis? = null
  private var videoCapture: VideoCapture<Recorder>? = null
  private var activeRecording: Recording? = null
  private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

  init {
    addView(
      previewView,
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    )
    addView(
      overlayView,
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    )
  }

  fun start(lifecycleOwner: LifecycleOwner) {
    if (backgroundExecutor == null) {
        backgroundExecutor = Executors.newSingleThreadExecutor()
    }
    
    backgroundExecutor?.execute {
        faceLandmarkerHelper = FaceLandmarkerHelper(
            context = context,
            landmarkerListener = this
        )
        post {
            setUpCamera(lifecycleOwner)
        }
    }
  }

  override fun requestLayout() {
        super.requestLayout()
        post(measureAndLayout)
    }

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }

  private fun setUpCamera(lifecycleOwner: LifecycleOwner) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
      {
        cameraProvider = cameraProviderFuture.get()
        bindCameraUseCases(lifecycleOwner)
      },
      ContextCompat.getMainExecutor(context)
    )
  }

  private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
    val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

    preview = Preview.Builder().build().also {
      it.setSurfaceProvider(previewView.surfaceProvider)
    }

    imageAnalyzer = ImageAnalysis.Builder()
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
      .build()
      .also {
        it.setAnalyzer(backgroundExecutor!!) { imageProxy ->
          detectFaces(imageProxy)
        }
      }

    val recorder = Recorder.Builder()
      .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
      .build()
    videoCapture = VideoCapture.withOutput(recorder)

    try {
      cameraProvider.unbindAll()
      cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageAnalyzer,
        videoCapture
      )
    } catch (exc: Exception) {
      Log.e("FaceMeshCameraView", "Use case binding failed", exc)
    }
  }

  fun startRecording() {
      val videoCapture = this.videoCapture ?: return
      if (activeRecording != null) return

      val videoFile = java.io.File(context.cacheDir, "face_scan_${System.currentTimeMillis()}.mp4")
      val outputOptions = FileOutputOptions.Builder(videoFile).build()

      activeRecording = videoCapture.output
          .prepareRecording(context, outputOptions)
          .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
              when (recordEvent) {
                  is VideoRecordEvent.Finalize -> {
                      if (!recordEvent.hasError()) {
                          var uri = recordEvent.outputResults.outputUri.toString()
                          if (!uri.startsWith("file://")) {
                              uri = "file://" + videoFile.absolutePath
                          }
                          val event = Arguments.createMap()
                          event.putString("uri", uri)
                          (context as? ReactContext)
                              ?.getJSModule(RCTEventEmitter::class.java)
                              ?.receiveEvent(id, "onVideoRecorded", event)
                      } else {
                          Log.e("FaceMeshCameraView", "Video capture ends with error: ${recordEvent.error}")
                      }
                      activeRecording = null
                  }
              }
          }
  }

  fun stopRecording() {
      activeRecording?.stop()
      activeRecording = null
  }

  private fun detectFaces(imageProxy: ImageProxy) {
    val bitmapBuffer = Bitmap.createBitmap(
        imageProxy.width,
        imageProxy.height,
        Bitmap.Config.ARGB_8888
    )
    imageProxy.use {
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
    }

    val matrix = Matrix().apply {
        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            postScale(-1f, 1f, imageProxy.width.toFloat() / 2, imageProxy.height.toFloat() / 2)
        }
    }

    val rotatedBitmap = Bitmap.createBitmap(
        bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
    )

    faceLandmarkerHelper?.detectLiveStream(rotatedBitmap)
  }

  override fun onResults(result: FaceLandmarkerResult) {
    post { overlayView.updateResult(result) }
  }

  override fun onError(error: String) {
    Log.e("FaceMeshCameraView", "FaceLandmarkerError: $error")
    post { overlayView.updateResult(null) }
  }
}
