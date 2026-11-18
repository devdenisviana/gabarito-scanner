package com.devdenisviana.gabaritoscanner

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devdenisviana.gabaritoscanner.data.ExamEntity
import com.devdenisviana.gabaritoscanner.data.toSheetProfile
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetProfile
import com.devdenisviana.gabaritoscanner.domain.student.StudentTag
import com.devdenisviana.gabaritoscanner.render.SheetPdfExporter
import com.devdenisviana.gabaritoscanner.ui.AppViewModel
import com.devdenisviana.gabaritoscanner.ui.account.AccountScreen
import com.devdenisviana.gabaritoscanner.ui.camera.CameraScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamDraft
import com.devdenisviana.gabaritoscanner.ui.exam.ExamEditScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamFormScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamListScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamPickerScreen
import com.devdenisviana.gabaritoscanner.ui.exam.ExamResultsScreen
import com.devdenisviana.gabaritoscanner.ui.exam.KeySheetScreen
import com.devdenisviana.gabaritoscanner.ui.home.HomeScreen
import com.devdenisviana.gabaritoscanner.ui.scan.QuickGradeScreen
import com.devdenisviana.gabaritoscanner.ui.scan.ScanResultScreen
import com.devdenisviana.gabaritoscanner.ui.theme.GabaritoScannerTheme
import com.devdenisviana.gabaritoscanner.ui.theme.ThemeMode
import com.devdenisviana.gabaritoscanner.ui.theme.ThemePreference
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemePreference.load(context)) }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // The colored header is always at the top, so the status bar icons stay light.
            // The bottom bar icons follow the app theme, even when it differs from the phone's.
            DisposableEffect(darkTheme) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
                onDispose {}
            }

            GabaritoScannerTheme(darkTheme = darkTheme) {
                // Every screen handles the system bars itself (edge-to-edge).
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GabaritoApp(
                        themeMode = themeMode,
                        onThemeChange = { mode ->
                            themeMode = mode
                            ThemePreference.save(context, mode)
                        },
                    )
                }
            }
        }
    }
}

/** The screens of the app (simple state-based navigation for now). */
sealed interface Screen {
    data object Home : Screen
    data object Account : Screen
    data object Exams : Screen
    data object ExamForm : Screen
    data class ExamEdit(val exam: ExamEntity) : Screen
    data object PickExam : Screen
    data object PickExamQuick : Screen
    data class Results(val exam: ExamEntity) : Screen
    data class Camera(val exam: ExamEntity) : Screen
    data class Photo(val file: File, val exam: ExamEntity) : Screen
    data class QuickCamera(val exam: ExamEntity, val sheetNumber: Int) : Screen
    data class QuickPhoto(val file: File, val exam: ExamEntity, val sheetNumber: Int) : Screen
    data class KeyCamera(val profile: SheetProfile) : Screen
    data class KeyPhoto(val file: File, val profile: SheetProfile) : Screen
}

/**
 * How deep a screen is. Going to a deeper screen slides forward, going to a
 * shallower one slides back, and the same depth just cross-fades.
 */
private fun Screen.depth(): Int = when (this) {
    Screen.Home -> 0
    Screen.Account, Screen.Exams, Screen.PickExam, Screen.PickExamQuick -> 1
    Screen.ExamForm, is Screen.ExamEdit, is Screen.Results, is Screen.Camera, is Screen.Photo,
    is Screen.QuickCamera, is Screen.QuickPhoto -> 2
    is Screen.KeyCamera, is Screen.KeyPhoto -> 3
}

@Composable
fun GabaritoApp(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = viewModel(),
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val draft = remember { ExamDraft() }

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) {
            Screen.ExamForm, is Screen.ExamEdit, is Screen.Results -> Screen.Exams
            is Screen.KeyCamera, is Screen.KeyPhoto -> Screen.ExamForm
            else -> Screen.Home
        }
    }

    AnimatedContent(
        targetState = screen,
        modifier = modifier,
        transitionSpec = {
            val from = initialState.depth()
            val to = targetState.depth()
            when {
                to > from ->
                    (slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(200)))

                to < from ->
                    (slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(200)))

                else -> fadeIn(tween(250)) togetherWith fadeOut(tween(200))
            }
        },
        label = "screens",
    ) { current ->
        // Each screen gets its own background, so two screens never show through each other.
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (current) {
                Screen.Home -> HomeScreen(
                    signedIn = userEmail != null,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    onQuickScan = { screen = Screen.PickExamQuick },
                    onScan = { screen = Screen.PickExam },
                    onExams = { screen = Screen.Exams },
                    onPrintSheet = { openSheetPdf(context, SheetProfile.DEFAULT) },
                    onAccount = { screen = Screen.Account },
                )

                Screen.Account -> AccountScreen(
                    userEmail = userEmail,
                    unsyncedCount = unsyncedCount,
                    onSignIn = viewModel::signIn,
                    onSignUp = viewModel::signUp,
                    onSync = viewModel::sync,
                    onSignOut = viewModel::signOut,
                    onBack = { screen = Screen.Home },
                )

                Screen.Exams -> ExamListScreen(
                    exams = exams,
                    onOpen = { exam ->
                        viewModel.selectExam(exam.id)
                        screen = Screen.Results(exam)
                    },
                    onSheet = { exam, student -> openSheetPdf(context, exam.toSheetProfile(), student) },
                    onEdit = { exam -> screen = Screen.ExamEdit(exam) },
                    onNewExam = {
                        draft.clear()
                        screen = Screen.ExamForm
                    },
                    onDelete = viewModel::deleteExam,
                    onBack = { screen = Screen.Home },
                )

                Screen.ExamForm -> ExamFormScreen(
                    draft = draft,
                    onSave = viewModel::createExam,
                    onReadFromSheet = { questionCount, options ->
                        screen = Screen.KeyCamera(SheetProfile.of(questionCount, options))
                    },
                    onBack = { screen = Screen.Exams },
                )

                is Screen.ExamEdit -> ExamEditScreen(
                    exam = current.exam,
                    onSave = { name, answerKey -> viewModel.updateExam(current.exam, name, answerKey) },
                    onBack = { screen = Screen.Exams },
                )

                Screen.PickExam -> ExamPickerScreen(
                    exams = exams,
                    onPick = { exam -> screen = Screen.Camera(exam) },
                    onNewExam = {
                        draft.clear()
                        screen = Screen.ExamForm
                    },
                    onBack = { screen = Screen.Home },
                )

                Screen.PickExamQuick -> ExamPickerScreen(
                    exams = exams,
                    onPick = { exam -> screen = Screen.QuickCamera(exam, 1) },
                    onNewExam = {
                        draft.clear()
                        screen = Screen.ExamForm
                    },
                    onBack = { screen = Screen.Home },
                )

                is Screen.Results -> {
                    val results by viewModel.results.collectAsStateWithLifecycle()
                    ExamResultsScreen(
                        exam = current.exam,
                        results = results,
                        onScan = { screen = Screen.Camera(current.exam) },
                        onDelete = viewModel::deleteResult,
                        onBack = { screen = Screen.Exams },
                    )
                }

                is Screen.Camera -> CameraScreen(
                    onPhotoCaptured = { file -> screen = Screen.Photo(file, current.exam) },
                    onClose = { screen = Screen.Home },
                )

                is Screen.Photo -> ScanResultScreen(
                    file = current.file,
                    exam = current.exam,
                    onSave = { name, code, readings, grade ->
                        viewModel.saveResult(current.exam, name, code, readings, grade)
                    },
                    onRetake = { screen = Screen.Camera(current.exam) },
                    onClose = { screen = Screen.Home },
                )

                is Screen.QuickCamera -> CameraScreen(
                    onPhotoCaptured = { file ->
                        screen = Screen.QuickPhoto(file, current.exam, current.sheetNumber)
                    },
                    onClose = { screen = Screen.Home },
                )

                is Screen.QuickPhoto -> QuickGradeScreen(
                    file = current.file,
                    exam = current.exam,
                    sheetNumber = current.sheetNumber,
                    onNext = { screen = Screen.QuickCamera(current.exam, current.sheetNumber + 1) },
                    onRetake = { screen = Screen.QuickCamera(current.exam, current.sheetNumber) },
                    onClose = { screen = Screen.Home },
                )

                is Screen.KeyCamera -> CameraScreen(
                    onPhotoCaptured = { file -> screen = Screen.KeyPhoto(file, current.profile) },
                    onClose = { screen = Screen.ExamForm },
                )

                is Screen.KeyPhoto -> KeySheetScreen(
                    file = current.file,
                    profile = current.profile,
                    onUse = { answers ->
                        draft.answerKey = answers
                        screen = Screen.ExamForm
                    },
                    onRetake = { screen = Screen.KeyCamera(current.profile) },
                    onCancel = { screen = Screen.ExamForm },
                )
            }
        }
    }
}

private fun openSheetPdf(context: Context, profile: SheetProfile, student: StudentTag? = null) {
    val file = SheetPdfExporter.export(context, profile, student)
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