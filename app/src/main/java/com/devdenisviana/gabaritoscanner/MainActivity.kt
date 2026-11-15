package com.devdenisviana.gabaritoscanner

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.render.SheetPdfExporter
import com.devdenisviana.gabaritoscanner.ui.camera.CameraScreen
import com.devdenisviana.gabaritoscanner.ui.theme.GabaritoScannerTheme
import java.io.File
import com.devdenisviana.gabaritoscanner.ui.scan.ScanResultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GabaritoScannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GabaritoApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** The screens of the app (simple state-based navigation for now). */
sealed interface Screen {
    data object Home : Screen
    data object Camera : Screen
    data class Photo(val file: File) : Screen
}

@Composable
fun GabaritoApp(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            onScan = { screen = Screen.Camera },
            modifier = modifier,
        )

        Screen.Camera -> CameraScreen(
            onPhotoCaptured = { file -> screen = Screen.Photo(file) },
            onClose = { screen = Screen.Home },
            modifier = modifier,
        )

        is Screen.Photo -> ScanResultScreen(
            file = current.file,
            onRetake = { screen = Screen.Camera },
            onClose = { screen = Screen.Home },
            modifier = modifier,
        )
    }
}

@Composable
fun HomeScreen(onScan: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Gabarito Scanner")
        Button(onClick = { openSheetPdf(context, SheetProfile.DEFAULT) }) {
            Text(text = "Gerar folha de respostas (PDF)")
        }
        Button(onClick = onScan) {
            Text(text = "Ler gabarito (câmera)")
        }
    }
}

private fun openSheetPdf(context: Context, profile: SheetProfile) {
    val file = SheetPdfExporter.export(context, profile)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum app de PDF encontrado", Toast.LENGTH_LONG).show()
    }
}