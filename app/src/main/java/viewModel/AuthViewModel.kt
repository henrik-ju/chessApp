package viewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chess.chessGame.model.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()
    private val preference = application.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _loggedIn = MutableStateFlow(userRepository.getCurrentUserEmail() != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn

    private val _loginError = MutableStateFlow("")
    val loginError: StateFlow<String> = _loginError

    private val _currentUserEmail = MutableStateFlow(userRepository.getCurrentUserEmail())
    val currentUserEmail: StateFlow<String?> = _currentUserEmail

    private val _currentDisplayUsername = MutableStateFlow(preference.getString("display_username", null))
    val currentDisplayUsername: StateFlow<String?> = _currentDisplayUsername

    init {
        if (_loggedIn.value) {
            fetchAndSetUsername()
        }
    }

    private fun fetchAndSetUsername() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            if (userId != null) {
                val username = userRepository.fetchUsername(userId)
                _currentDisplayUsername.value = username
                if (username != null) {
                    preference.edit().putString("display_username", username).apply()
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val success = userRepository.login(email, password)
                if (success) {
                    preference.edit().putString("email", email).apply()
                    _currentUserEmail.value = userRepository.getCurrentUserEmail()
                    _loggedIn.value = true
                    _loginError.value = ""
                    fetchAndSetUsername()
                } else {
                    _loginError.value = "Incorrect email or password"
                }
            } catch (e: Exception) {
                _loginError.value = "Login failed: Incorrect email or password."
                println("Login exception: ${e.message}")
            }
        }
    }

    fun createAccount(email: String, password: String, username: String) { // ADDED username parameter
        viewModelScope.launch {
            val created = userRepository.createAccount(email, password, username)
            if(created){
                preference.edit().putString("email", email).apply()
                preference.edit().putString("display_username", username).apply()
                _currentDisplayUsername.value = username

                _currentUserEmail.value = userRepository.getCurrentUserEmail()
                _loggedIn.value = true
                _loginError.value = ""
            } else {
                _loginError.value = "Account creation failed (email might be in use or password too weak)"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun logout() {
        userRepository.logout()
        preference.edit().clear().apply()
        _currentUserEmail.value = null
        _currentDisplayUsername.value = null
        _loggedIn.value = false
    }

    fun setError(message: String) {
        _loginError.value = message
    }
}