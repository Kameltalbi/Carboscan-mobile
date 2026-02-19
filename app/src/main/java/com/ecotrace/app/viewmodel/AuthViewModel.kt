package com.ecotrace.app.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotrace.app.data.repository.AuthRepository
import com.ecotrace.app.data.repository.AuthState
import com.ecotrace.app.data.repository.DriveRepository
import com.ecotrace.app.data.repository.SyncRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val driveRepository: DriveRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun getGoogleSignInIntent(): Intent {
        return authRepository.getGoogleSignInClient().signInIntent
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.signInWithGoogle(account)
                .onSuccess {
                    // Initialiser le service Drive
                    driveRepository.initializeDriveService()

                    // Vérifier s'il y a des données de sauvegarde sur Drive
                    if (syncRepository.hasCloudBackup()) {
                        // Restaurer les données depuis Drive
                        syncRepository.restoreFromCloud()
                            .onSuccess {
                                _isLoading.value = false
                            }
                            .onFailure { e ->
                                _errorMessage.value = "Erreur de restauration: ${e.message}"
                                _isLoading.value = false
                            }
                    } else {
                        // Première connexion - sauvegarder les données locales sur Drive
                        syncRepository.backupToCloud()
                        _isLoading.value = false
                    }
                }
                .onFailure { e ->
                    _errorMessage.value = "Erreur de connexion: ${e.message}"
                    _isLoading.value = false
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Sauvegarder avant de se déconnecter
            syncRepository.backupToCloud()
            authRepository.signOut()
        }
    }

    fun syncData() {
        viewModelScope.launch {
            syncRepository.autoSync()
                .onFailure { e ->
                    _errorMessage.value = "Erreur de synchronisation: ${e.message}"
                }
        }
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
