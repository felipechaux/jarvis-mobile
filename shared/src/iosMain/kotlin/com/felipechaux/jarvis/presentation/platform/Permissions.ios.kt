package com.felipechaux.jarvis.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFAudio.AVAudioSession
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UIKit.UIView
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

private fun onMain(block: () -> Unit) = dispatch_async(dispatch_get_main_queue(), block)

@Composable
actual fun rememberPermissionRequest(permission: AppPermission, onResult: (granted: Boolean) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onResult)
    return remember(permission) {
        {
            when (permission) {
                AppPermission.MICROPHONE ->
                    AVAudioSession.sharedInstance().requestRecordPermission { granted -> onMain { latest(granted) } }
                AppPermission.CAMERA ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted -> onMain { latest(granted) } }
                AppPermission.NOTIFICATIONS ->
                    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                    ) { granted, _ ->
                        onMain {
                            // The APNs token arrives in the app delegate (iosApp).
                            if (granted) UIApplication.sharedApplication.registerForRemoteNotifications()
                            latest(granted)
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PreviewContainer(private val previewLayer: AVCaptureVideoPreviewLayer) :
    UIView(frame = CGRectZero.readValue()) {
    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }
}

private class QrDelegate(private val onCode: (String) -> Unit) :
    NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    private var done = false

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        if (done) return
        val code = didOutputMetadataObjects.firstNotNullOfOrNull {
            (it as? AVMetadataMachineReadableCodeObject)?.stringValue
        } ?: return
        done = true
        onCode(code)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScannerView(onScanned: (String) -> Unit, modifier: Modifier) {
    val latest by rememberUpdatedState(onScanned)
    val session = remember { AVCaptureSession() }
    val delegate = remember { QrDelegate { code -> latest(code) } }
    DisposableEffect(Unit) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
            val output = AVCaptureMetadataOutput()
            if (session.canAddOutput(output)) {
                session.addOutput(output)
                output.setMetadataObjectsDelegate(delegate, queue = dispatch_get_main_queue())
                output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
            }
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                session.startRunning()
            }
        }
        onDispose { session.stopRunning() }
    }
    UIKitView(
        factory = {
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = AVLayerVideoGravityResizeAspectFill
            PreviewContainer(layer)
        },
        modifier = modifier,
    )
}

