package com.fjordflow.ui.screens.reader

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fjordflow.data.translation.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CameraScanner(
    onTextScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(onTextScanned = onTextScanned, onDismiss = onDismiss)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(
                    "Camera permission is required to scan text.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onTextScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isFocusing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Corner-bracket alignment guide
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f)
                .align(Alignment.Center)
        ) {
            val stroke = 4.dp.toPx()
            val arm = 36.dp.toPx()
            val color = Color.White.copy(alpha = 0.85f)
            val w = size.width
            val h = size.height
            drawLine(color, Offset(0f, 0f), Offset(arm, 0f), stroke)
            drawLine(color, Offset(0f, 0f), Offset(0f, arm), stroke)
            drawLine(color, Offset(w, 0f), Offset(w - arm, 0f), stroke)
            drawLine(color, Offset(w, 0f), Offset(w, arm), stroke)
            drawLine(color, Offset(0f, h), Offset(arm, h), stroke)
            drawLine(color, Offset(0f, h), Offset(0f, h - arm), stroke)
            drawLine(color, Offset(w, h), Offset(w - arm, h), stroke)
            drawLine(color, Offset(w, h), Offset(w, h - arm), stroke)
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isProcessing -> {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            statusMessage,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                isFocusing -> {
                    CircularProgressIndicator(color = Color.Yellow)
                    Spacer(Modifier.height(8.dp))
                    Text("Focusing...", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                else -> {
                    errorMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                "Error: $msg",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Button(
                        onClick = {
                            errorMessage = null
                            val cap = imageCapture ?: return@Button
                            val cam = camera
                            isFocusing = true

                            fun doCapture() {
                                isFocusing = false
                                isProcessing = true
                                statusMessage = "Capturing..."

                                val tempFile = File.createTempFile("scan", ".jpg", context.cacheDir)
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                                cap.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            statusMessage = "Reading page..."
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val rawBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                                    tempFile.delete()
                                                    if (rawBitmap == null) {
                                                        withContext(Dispatchers.Main) {
                                                            isProcessing = false
                                                            errorMessage = "Failed to decode captured image"
                                                        }
                                                        return@launch
                                                    }
                                                    // Resize to max 1920px — keeps text readable, avoids API timeouts
                                                    val bitmap = resizeBitmap(rawBitmap, 1920)
                                                    val text = GeminiClient.scanPage(bitmap)
                                                    Log.d("CameraScanner", "Gemini returned ${text.length} chars: ${text.take(100)}")
                                                    withContext(Dispatchers.Main) {
                                                        isProcessing = false
                                                        if (text.isNotBlank()) {
                                                            onTextScanned(text)
                                                        } else {
                                                            errorMessage = "No text detected in image"
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("CameraScanner", "Gemini scan failed", e)
                                                    tempFile.delete()
                                                    withContext(Dispatchers.Main) {
                                                        isProcessing = false
                                                        errorMessage = e.message ?: e.javaClass.simpleName
                                                    }
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CameraScanner", "Capture failed", exception)
                                            tempFile.delete()
                                            isProcessing = false
                                        }
                                    }
                                )
                            }

                            if (cam != null) {
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(previewView.width / 2f, previewView.height / 2f)
                                val action = FocusMeteringAction.Builder(point)
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cam.cameraControl.startFocusAndMetering(action)
                                    .addListener({ doCapture() }, ContextCompat.getMainExecutor(context))
                            } else {
                                doCapture()
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(
                            Icons.Default.TextSnippet,
                            contentDescription = "Scan",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Tap to scan page",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .build()

        imageCapture = capture

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
        } catch (e: Exception) {
            Log.e("CameraScanner", "Use case binding failed", e)
        }
    }
}

private fun resizeBitmap(bitmap: android.graphics.Bitmap, maxDimension: Int): android.graphics.Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val scale = minOf(maxDimension.toFloat() / w, maxDimension.toFloat() / h, 1f)
    if (scale >= 1f) return bitmap
    return android.graphics.Bitmap.createScaledBitmap(
        bitmap, (w * scale).toInt(), (h * scale).toInt(), true
    )
}
