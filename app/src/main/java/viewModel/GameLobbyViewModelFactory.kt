package viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GameLobbyViewModelFactory(
    val currentUser: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameLobbyViewModel::class.java)) {
            return GameLobbyViewModel(currentUser) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}