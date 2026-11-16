package com.devdenisviana.gabaritoscanner

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.render.SheetPdfExporter
import com.devdenisviana.gabaritoscanner.ui.AppViewModel
import com.devdenisviana.gabaritoscanner.ui.camera.CameraScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamFormScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamListScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamPickerScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamResultsScreen
import com.devdenisviana.gabaritoscanner.ui.scan.ScanResultScreen
import com.devdenisviana.gabaritoscanner.ui.theme.GabaritoScannerTheme
import java.io.File

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
    data object Exams : Screen
    data object ExamForm : Screen
    data object PickExam : Screen
    data class Results(val exam: ExamEntity) : Screen
    data class Camera(val exam: ExamEntity) : Screen
    data class Photo(val file: File, val exam: ExamEntity) : Screen
}

@Composable
fun GabaritoApp(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = viewModel(),
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val exams by viewModel.exams.collectAsStateWithLifecycle()

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) {
            Screen.ExamForm, is Screen.Results -> Screen.Exams
            else -> Screen.Home
        }
    }

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            onExams = { screen = Screen.Exams },
            onScan = { screen = Screen.PickExam },
            modifier = modifier,
        )

        Screen.Exams -> ExamListScreen(
            exams = exams,
            onOpen = { exam ->
                viewModel.selectExam(exam.id)
                screen = Screen.Results(exam)
            },
            onNewExam = { screen = Screen.ExamForm },
            onDelete = viewModel::deleteExam,
            onBack = { screen = Screen.Home },
            modifier = modifier,
        )

        Screen.ExamForm -> ExamFormScreen(
            onSave = viewModel::createExam,
            onBack = { screen = Screen.Exams },
            modifier = modifier,
        )

        Screen.PickExam -> ExamPickerScreen(
            exams = exams,
            onPick = { exam -> screen = Screen.Camera(exam) },
            onNewExam = { screen = Screen.ExamForm },
            onBack = { screen = Screen.Home },
            modifier = modifier,
        )

        is Screen.Results -> {
            val results by viewModel.results.collectAsStateWithLifecycle()
            ExamResultsScreen(
                exam = current.exam,
                results = results,
                onScan = { screen = Screen.Camera(current.exam) },
                onDelete = viewModel::deleteResult,
                onBack = { screen = Screen.Exams },
                modifier = modifier,
            )
        }

        is Screen.Camera -> CameraScreen(
            onPhotoCaptured = { file -> screen = Screen.Photo(file, current.exam) },
            onClose = { screen = Screen.Home },
            modifier = modifier,
        )

        is Screen.Photo -> ScanResultScreen(
            file = current.file,
            exam = current.exam,
            onSave = { name, code, readings, grade ->
                viewModel.saveResult(current.exam, name, code, readings, grade)
            },
            onRetake = { screen = Screen.Camera(current.exam) },
            onClose = { screen = Screen.Home },
            modifier = modifier,
        )
    }
}

@Composable
fun HomeScreen(
    onExams: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Gabarito Scanner")
        Button(onClick = onExams) {
            Text(text = "Provas")
        }
        Button(onClick = onScan) {
            Text(text = "Ler gabarito (câmera)")
        }
        Button(onClick = { openSheetPdf(context, SheetProfile.DEFAULT) }) {
            Text(text = "Gerar folha de respostas (PDF)")
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