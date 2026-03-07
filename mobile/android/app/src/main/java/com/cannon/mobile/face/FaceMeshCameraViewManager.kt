package com.cannon.mobile.face

import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext

class FaceMeshCameraViewManager : SimpleViewManager<FaceMeshCameraView>() {

    override fun getName() = "FaceMeshCameraView"

    override fun createViewInstance(reactContext: ThemedReactContext): FaceMeshCameraView {
        val view = FaceMeshCameraView(reactContext)

        val activity = reactContext.currentActivity
        if (activity is LifecycleOwner) {
            view.start(activity)
        }

        return view
    }

    override fun getCommandsMap(): Map<String, Int> {
        return MapBuilder.of(
            "startRecording", COMMAND_START_RECORDING,
            "stopRecording", COMMAND_STOP_RECORDING
        )
    }

    override fun receiveCommand(root: FaceMeshCameraView, commandId: Int, args: ReadableArray?) {
        super.receiveCommand(root, commandId, args)
        when (commandId) {
            COMMAND_START_RECORDING -> root.startRecording()
            COMMAND_STOP_RECORDING -> root.stopRecording()
        }
    }

    override fun receiveCommand(root: FaceMeshCameraView, commandId: String, args: ReadableArray?) {
        super.receiveCommand(root, commandId, args)
        when (commandId) {
            "startRecording" -> root.startRecording()
            "stopRecording" -> root.stopRecording()
        }
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any>? {
        return MapBuilder.of(
            "onVideoRecorded",
            MapBuilder.of("registrationName", "onVideoRecorded")
        )
    }

    companion object {
        const val COMMAND_START_RECORDING = 1
        const val COMMAND_STOP_RECORDING = 2
    }
}