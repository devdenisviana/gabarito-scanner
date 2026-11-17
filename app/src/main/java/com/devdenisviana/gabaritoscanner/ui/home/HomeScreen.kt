package com.devdenisviana.gabaritoscanner.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devdenisviana.gabaritoscanner.ui.components.ActionCard
import com.devdenisviana.gabaritoscanner.ui.components.AppHeader
import com.devdenisviana.gabaritoscanner.ui.components.Staggered

/** Home screen: a colored header and one card per main action, appearing one after the other. */
@Composable
fun HomeScreen(
    signedIn: Boolean,
    onQuickScan: () -> Unit,
    onScan: () -> Unit,
    onExams: () -> Unit,
    onPrintSheet: () -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        }
    }
}