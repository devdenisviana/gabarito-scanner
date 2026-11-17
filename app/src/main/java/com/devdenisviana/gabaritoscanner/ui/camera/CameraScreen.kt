package com.devdenisviana.gabaritoscanner.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/** Single background thread used to decode, rotate and save captured photos. */
private val captureExecutor = Executors.newSingleThreadExecutor()

/**
 * Camera screen: asks for permission, shows a live preview and captures a photo.
 * The saved JPEG is already upright and is handed back through [onPhotoCaptured].
 */
@Composable
fun CameraScreen(
    onPhotoCaptured: (File) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppHeader(
            title = "Fotografar a folha",
            subtitle = "Enquadre a folha inteira",
            onBack = onClose,
        )

        if (hasPermission) {
            CameraContent(
                onPhotoCaptured = onPhotoCaptured,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = "A permissão da câmera é necessária para ler o gabarito.",
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Permitir câmera")
                }
                OutlinedButton(onClick = onClose) { Text("Voltar") }
            }
        }
    }
}

@Composable
private fun CameraContent(
    onPhotoCaptured: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(4000, 3000),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener(
                    {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                    },
                    ContextCompat.getMainExecutor(ctx),
                )
                previewView
            },
        )

        GuideFrame(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Deixe os 4 marcadores pretos dentro dos cantos",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }

        ShutterButton(
            onClick = { takePhoto(context, imageCapture, onPhotoCaptured) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        )
    }
}

/** Four corner brackets in the proportion of the answer sheet, pulsing softly. */
@Composable
private fun GuideFrame(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "guide")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "guideAlpha",
    )

    Canvas(modifier = modifier) {
        val sideMargin = 24.dp.toPx()
        val topSpace = 64.dp.toPx()
        val bottomSpace = 120.dp.toPx()
        val ratio = 1000f / 1462f

        val availableW = size.width - 2 * sideMargin
        val availableH = size.height - topSpace - bottomSpace
        val width = minOf(availableW, availableH * ratio)
        val height = width / ratio
        val left = (size.width - width) / 2
        val top = topSpace + (availableH - height) / 2

        val length = 32.dp.toPx()
        val stroke = 4.dp.toPx()
        val color = Color.White.copy(alpha = alpha)

        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + dx * length, y), stroke, StrokeCap.Round)
            drawLine(color, Offset(x, y), Offset(x, y + dy * length), stroke, StrokeCap.Round)
        }

        corner(left, top, 1f, 1f)
        corner(left + width, top, -1f, 1f)
        corner(left, top + height, 1f, -1f)
        corner(left + width, top + height, -1f, -1f)
    }
}

/** Round shutter button that shrinks a little while pressed. */
@Composable
private fun ShutterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(120),
        label = "shutter",
    )

    Box(
        modifier = modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .background(Color.White, CircleShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
    )
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (File) -> Unit,
) {
    val mainExecutor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        captureExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val rotation = image.imageInfo.rotationDegrees
                    val bitmap = image.toBitmap()
                    val upright = if (rotation == 0) {
                        bitmap
                    } else {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }

                    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                    val file = File(dir, "gabarito_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        upright.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    mainExecutor.execute { onPhotoCaptured(file) }
                } catch (e: Exception) {
                    mainExecutor.execute {
                        Toast.makeText(context, "Erro ao salvar a foto: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                mainExecutor.execute {
                    Toast.makeText(context, "Erro ao tirar a foto: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        },
    )
}