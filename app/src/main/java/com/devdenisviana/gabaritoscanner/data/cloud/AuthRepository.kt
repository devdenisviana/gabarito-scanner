package com.devdenisviana.gabaritoscanner.data.cloud

import android.util.Patterns
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Teacher login with e-mail and password (Firebase Authentication). */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    /** E-mail of the signed-in teacher, or null when signed out. Updates by itself. */
    val userEmail: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.email) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Id of the signed-in teacher, or null when signed out. */
    val userId: String?
        get() = auth.currentUser?.uid

    /** [onResult] receives an error message for the user, or null on success. */
    fun signIn(email: String, password: String, onResult: (String?) -> Unit) {
        checkFields(email, password)?.let { return onResult(it) }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                onResult(if (task.isSuccessful) null else messageFor(task.exception))
            }
    }

    fun signUp(email: String, password: String, onResult: (String?) -> Unit) {
        checkFields(email, password)?.let { return onResult(it) }
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                onResult(if (task.isSuccessful) null else messageFor(task.exception))
            }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun checkFields(email: String, password: String): String? = when {
        email.isBlank() || password.isEmpty() -> "Informe e-mail e senha."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Esse e-mail não parece válido."
        else -> null
    }

    private fun messageFor(error: Exception?): String = when (error) {
        // The weak-password error is a kind of invalid-credentials error, so it comes first.
        is FirebaseAuthWeakPasswordException -> "A senha precisa ter pelo menos 6 caracteres."
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException,
            -> "E-mail ou senha incorretos."
        is FirebaseAuthUserCollisionException -> "Já existe uma conta com esse e-mail."
        is FirebaseNetworkException -> "Sem conexão com a internet."
        else -> "Não foi possível concluir. Tente de novo."
    }
}