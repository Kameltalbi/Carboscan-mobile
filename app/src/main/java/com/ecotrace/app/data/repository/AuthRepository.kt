package com.ecotrace.app.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val uid: String, val email: String, val displayName: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        _authState.value = if (currentUser != null) {
            AuthState.Authenticated(
                uid = currentUser.uid,
                email = currentUser.email ?: "",
                displayName = currentUser.displayName
            )
        } else {
            AuthState.Unauthenticated
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_CLIENT_ID") // À remplacer par le vrai client ID
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE), // Accès aux fichiers créés par l'app
                Scope(DriveScopes.DRIVE_APPDATA) // Accès au dossier app data
            )
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<AuthState.Authenticated> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("User is null")
            
            val authState = AuthState.Authenticated(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName
            )
            _authState.value = authState
            Result.success(authState)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        getGoogleSignInClient().signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun isUserSignedIn(): Boolean = auth.currentUser != null
}
