package chess.chessGame.viewModel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chess.chessGame.model.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

data class AdminUser(
    val email: String,
    val uid: String,
    val username: String? = null,
    val role: String = "ordinary"
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository()
    private val preference = application.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val adminUID = "UW9Famx2uTUesUfdH66Wr3E12Zy2"

    private val _userRole = MutableStateFlow("guest")
    val userRole: StateFlow<String> = _userRole
    private val _loggedIn = MutableStateFlow(userRepository.getCurrentUserEmail() != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn
    private val _loginError = MutableStateFlow("")
    val loginError: StateFlow<String> = _loginError
    private val _currentUserEmail = MutableStateFlow(userRepository.getCurrentUserEmail())
    val currentUserEmail: StateFlow<String?> = _currentUserEmail
    private val initialUid = userRepository.getCurrentUserId() ?: "Guest"
    private val _currentUserUid: MutableStateFlow<String?> = MutableStateFlow(initialUid)
    val currentUserUid: StateFlow<String?> = _currentUserUid
    private val _currentDisplayUsername: MutableStateFlow<String?> =
        MutableStateFlow(preference.getString("display_username", null))
    val currentDisplayUsername: StateFlow<String?> = _currentDisplayUsername
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready
    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus

    private val _adminUsersList = MutableStateFlow<List<AdminUser>>(emptyList())
    val adminUsersList: StateFlow<List<AdminUser>> = _adminUsersList

    private val _currentPhotoUrl = MutableStateFlow<String?>(null)
    val currentPhotoUrl: StateFlow<String?> = _currentPhotoUrl

    init {
        if (_loggedIn.value) {
            observeUserProfile()
            fetchUserProfile()
        } else {
            _currentUserUid.value = "Guest"
            _userRole.value = "guest"
            _currentDisplayUsername.value = "Guest"
            _ready.value = true
        }
    }

    private fun observeUserProfile() {
        val userId = userRepository.getCurrentUserId()
        if (userId != null) {
            viewModelScope.launch {
                userRepository.getProfilePhotoUrl(userId)
                    .collectLatest { url ->
                        _currentPhotoUrl.value = url
                    }
            }
        } else {
            _currentPhotoUrl.value = null
        }
    }

    fun clearUpdateStatus() {
        _updateStatus.value = null
    }

    suspend fun updateUsername(newUsername: String): Boolean {
        val userId = userRepository.getCurrentUserId()
        if (userId == null || userId == "Guest") {
            _updateStatus.value = "Error: Must be logged in to update username."
            return false
        }

        _updateStatus.value = "Updating.."
        return try {
            userRepository.updateUsername(userId, newUsername)

            _currentDisplayUsername.value = newUsername
            preference.edit {
                putString("display_username", newUsername)
            }

            _updateStatus.value = "Username updated successfully!"
            true
        } catch (e: Exception) {
            _updateStatus.value = "Error updating username: ${e.message}"
            false
        }
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            if (userId != null) {

                if (userId == adminUID) {
                    _userRole.value = "admin"
                    _currentDisplayUsername.value = "Admin User"
                } else {
                    val username = userRepository.fetchUsername(userId)

                    _userRole.value = "ordinary"

                    _currentDisplayUsername.value = username
                    if (username != null) {
                        preference.edit {
                            putString("display_username", username)
                        }
                    }
                }
                observeUserProfile()
            } else {
                _userRole.value = "guest"
            }
            _ready.value = true
        }
    }

    fun loginAsGuest() {
        userRepository.logout()
        preference.edit { clear() }

        _loggedIn.value = true
        _currentUserEmail.value = null
        _currentUserUid.value = "Guest"
        _currentDisplayUsername.value = "Guest"
        _userRole.value = "guest"
        _ready.value = true
        _currentPhotoUrl.value = null
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val success = userRepository.login(email, password)
                if (success) {
                    preference.edit { putString("email", email) }
                    _currentUserEmail.value = userRepository.getCurrentUserEmail()
                    _loggedIn.value = true
                    _loginError.value = ""
                    _currentUserUid.value = userRepository.getCurrentUserId()
                    fetchUserProfile()
                } else {
                    _loginError.value = "Incorrect email or password"
                }
            } catch (e: Exception) {
                _loginError.value = "Login failed: Incorrect email or password."
                println("Login exception: ${e.message}")
            }
        }
    }

    fun createAccount(email: String, password: String, username: String) {
        viewModelScope.launch {
            val created = userRepository.createAccount(email, password, username)
            if(created){
                val defaultRole = "ordinary"

                preference.edit {
                    putString("email", email)
                    putString("display_username", username)
                }

                _currentDisplayUsername.value = username
                _userRole.value = defaultRole
                _currentUserEmail.value = userRepository.getCurrentUserEmail()
                _loggedIn.value = true
                _loginError.value = ""
                _currentUserUid.value = userRepository.getCurrentUserId()
                observeUserProfile()

                _ready.value = true
            } else {
                _loginError.value = "Account creation failed (email might be in use or password too weak)"
                _ready.value = true
            }
        }
    }

    fun logout() {
        userRepository.logout()
        preference.edit { clear() }
        _currentUserEmail.value = null
        _currentDisplayUsername.value = null
        _loggedIn.value = false
        _userRole.value = "guest"
        _ready.value = true
        _currentUserUid.value = "Guest"
        _currentPhotoUrl.value = null
    }

    suspend fun updatePassword(newPassword: String): Boolean {
        if (!loggedIn.value) {
            _updateStatus.value = "Error: Must be logged in to change password."
            return false
        }
        _updateStatus.value = "Updating password..."
        return try {
            userRepository.updatePassword(newPassword)
            _updateStatus.value = "Password updated successfully! Please log in again to confirm."
            true
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("requires recent login") == true ->
                    "Password update failed: Please log out and log in again, then try to change your password immediately."
                e.message?.contains("weak password") == true ->
                    "Password update failed: Password is too weak (min 6 characters)."
                else -> "Error updating password."
            }
            _updateStatus.value = errorMessage
            false
        }
    }

    fun fetchAdminUsers() {
        if (userRole.value != "admin") return

        viewModelScope.launch {
            _updateStatus.value = "Fetching users..."
            try {
                val usersMap = userRepository.fetchAllUsersForAdmin()
                val adminUsers = usersMap.map { (uid, userData) ->
                    val role = if (uid == adminUID) "admin" else "ordinary"
                    AdminUser(
                        email = userData["email"] ?: "N/A",
                        uid = uid,
                        username = userData["username"],
                        role = role
                    )
                }
                _adminUsersList.value = adminUsers
                _updateStatus.value = "Users fetched successfully."
            } catch (e: Exception) {
                _updateStatus.value = "Error fetching users: ${e.message}"
                _adminUsersList.value = emptyList()
            }
        }
    }

    suspend fun deleteUser(email: String): Boolean {
        if (userRole.value != "admin") return false
        _updateStatus.value = "Deleting user $email..."

        return try {
            val uid = userRepository.getUidByEmail(email)
            if (uid == null) {
                _updateStatus.value = "Error: Could not find UID for $email."
                return false
            }

            userRepository.deleteUser(uid)
            fetchAdminUsers()
            _updateStatus.value = "User $email deleted successfully."
            true
        } catch (e: Exception) {
            _updateStatus.value = "Error deleting user $email: ${e.message}"
            false
        }
    }

    suspend fun changeUserPassword(email: String, newPassword: String): Boolean {
        if (userRole.value != "admin") return false
        _updateStatus.value = "Changing password for $email..."

        return try {
            val uid = userRepository.getUidByEmail(email)
            if (uid == null) {
                _updateStatus.value = "Error: Could not find UID for $email."
                return false
            }

            userRepository.adminUpdatePassword(uid, newPassword)
            _updateStatus.value = "Password for $email changed successfully."
            true
        } catch (e: Exception) {
            _updateStatus.value = "Error changing password for $email: ${e.message}"
            false
        }
    }

    suspend fun uploadProfilePicture(uri: Uri) {
        val userId = userRepository.getCurrentUserId()
        if (userId.isNullOrEmpty() || userId == "Guest") {
            _updateStatus.value = "Error: Must be logged in to upload picture."
            return
        }
        val storageRef = Firebase.storage.reference.child("profile_images/$userId.jpg")
        try {
            _updateStatus.value = "Uploading image..."
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            userRepository.updateProfilePictureUrl(userId, downloadUrl)
            _currentPhotoUrl.value = downloadUrl
            _updateStatus.value = "Profile picture updated successfully!"
        } catch (e: Exception) {
            _updateStatus.value = "Error uploading picture: ${e.localizedMessage}"
        }
    }

    fun setError(message: String) {
        _loginError.value = message
    }
}
