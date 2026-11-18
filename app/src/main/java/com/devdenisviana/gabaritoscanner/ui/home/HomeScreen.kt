package com.devdenisviana.gabaritoscanner.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.ui.components.ActionCard
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import com.devdenisviana.gabaritoscanner.ui.components.Staggered
import com.devdenisviana.gabaritoscanner.ui.theme.ThemeMode

/** Home screen: a colored header and one card per main action, appearing one after the other. */
@Composable
fun HomeScreen(
    signedIn: Boolean,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onQuickScan: () -> Unit,
    onScan: () -> Unit,
    onExams: () -> Unit,
    onPrintSheet: () -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppHeader(
            title = "Gabarito Scanner",
            icon = Icons.Filled.Checklist,
            subtitle = "Corrija provas pela câmera do celular",
            large = true,
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Staggered(index = 0, modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    icon = Icons.Filled.PhotoCamera,
                    title = "Só corrigir",
                    subtitle = "Fotografe a folha e veja a nota na hora, sem cadastrar aluno",
                    onClick = onQuickScan,
                )
            }
            Staggered(index = 1, modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    icon = Icons.Filled.Person,
                    title = "Corrigir e salvar",
                    subtitle = "Identifica o aluno pelo QR code e guarda o resultado",
                    onClick = onScan,
                )
            }
            Staggered(index = 2, modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    icon = Icons.Filled.Checklist,
                    title = "Gabaritos",
                    subtitle = "Cadastre gabaritos, gere folhas e veja resultados",
                    onClick = onExams,
                )
            }
            Staggered(index = 3, modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    icon = Icons.Filled.PictureAsPdf,
                    title = "Folha de respostas (PDF)",
                    subtitle = "Folha padrão de 20 questões, pronta para imprimir",
                    onClick = onPrintSheet,
                )
            }
            Staggered(index = 4, modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    icon = Icons.Filled.AccountCircle,
                    title = if (signedIn) "Conta e sincronização" else "Entrar / criar conta",
                    subtitle = if (signedIn) {
                        "Conectado. Envie os resultados para a nuvem"
                    } else {
                        "Guarde os resultados online"
                    },
                    onClick = onAccount,
                )
            }
            Staggered(index = 5, modifier = Modifier.fillMaxWidth()) {
                ActionCard(
                    icon = Icons.Filled.Palette,
                    title = "Aparência",
                    subtitle = "Tema: ${themeMode.short}",
                    onClick = { showThemeDialog = true },
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Aparência") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = themeMode == mode,
                                    onClick = {
                                        onThemeChange(mode)
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = null)
                            Text(text = mode.label, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Fechar") }
            },
        )
    }
}