package com.felipechaux.jarvis.presentation.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@Composable
actual fun rememberPermissionRequest(permission: AppPermission, onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onResult)
    val manifestPermission = when (permission) {
        AppPermission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
        AppPermission.CAMERA -> Manifest.permission.CAMERA
        AppPermission.NOTIFICATIONS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { latest(it) }
    return remember(manifestPermission, launcher) {
        {
            val granted = manifestPermission == null ||
                ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED
            if (granted) latest(true) else launcher.launch(manifestPermission)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
actual fun QrScannerView(onScanned: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onScanned)
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    DisposableEffect(Unit) {
        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            scanner.close()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    var done = false
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                        val media = proxy.image
                        if (media == null || done) {
                            proxy.close()
                            return@setAnalyzer
                        }
                        scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener { codes ->
                                val value = codes.firstNotNullOfOrNull { it.rawValue }
                                if (value != null && !done) {
                                    done = true
                                    latest(value)
                                }
                            }
                            .addOnCompleteListener { proxy.close() }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
    )
}
