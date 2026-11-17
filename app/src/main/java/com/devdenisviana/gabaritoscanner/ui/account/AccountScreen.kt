package com.devdenisviana.gabaritoscanner.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Teacher account: sign in, create an account, sync with the cloud or sign out.
 * [onSignIn] / [onSignUp] report back with an error message, or null on success.
 */
@Composable
fun AccountScreen(
    userEmail: String?,
    unsyncedCount: Int,
    onSignIn: (email: String, password: String, onResult: (String?) -> Unit) -> Unit,
    onSignUp: (email: String, password: String, onResult: (String?) -> Unit) -> Unit,
    onSync: (onResult: (String) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var creating by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Conta", style = MaterialTheme.typography.headlineSmall)

        if (userEmail != null) {
            Text("Conectado como $userEmail")
            Text(
                if (unsyncedCount == 0) {
                    "Nenhum resultado aguardando envio."
                } else {
                    "$unsyncedCount resultado(s) aguardando envio."
                },
            )
            Button(
                enabled = !syncing,
                onClick = {
                    syncing = true
                    syncMessage = null
                    onSync { message ->
                        syncing = false
                        syncMessage = message
                    }
                },
            ) { Text(if (syncing) "Enviando..." else "Sincronizar agora") }

            syncMessage?.let { Text(it) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSignOut) { Text("Sair") }
                OutlinedButton(onClick = onBack) { Text("Voltar") }
            }
        } else {
            Text(
                if (creating) {
                    "Crie sua conta para guardar os resultados online."
                } else {
                    "Entre para guardar os resultados online. Sem conta, o app continua funcionando só neste celular."
                },
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                },
                label = { Text("E-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = { Text(if (creating) "Senha (mínimo 6 caracteres)" else "Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (creating) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = null
                    },
                    label = { Text("Repita a senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !busy,
                    onClick = {
                        if (creating && password != confirmPassword) {
                            error = "As senhas não são iguais. Digite de novo."
                        } else {
                            busy = true
                            val action = if (creating) onSignUp else onSignIn
                            action(email, password) { message ->
                                busy = false
                                error = message
                                if (message == null) {
                                    password = ""
                                    confirmPassword = ""
                                    creating = false
                                }
                            }
                        }
                    },
                ) { Text(if (creating) "Criar conta" else "Entrar") }
                OutlinedButton(onClick = onBack) { Text("Voltar") }
            }

            TextButton(onClick = {
                creating = !creating
                error = null
                confirmPassword = ""
            }) {
                Text(if (creating) "Já tenho conta. Entrar" else "Não tenho conta. Criar uma")
            }
        }
    }
}