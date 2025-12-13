package viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import chess.chessGame.model.FirebaseChess

class GameLobbyViewModelFactory(
    val currentUser: String,
    val firebaseService: FirebaseChess,
    private val onNavigateToGame: (gameId: String, assignedColor: String) -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameLobbyViewModel::class.java)) {
            return GameLobbyViewModel(
                currentUser,
                firebaseService = firebaseService,
                onNavigateToGame = onNavigateToGame
                ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}