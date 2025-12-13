package viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import chess.chessGame.model.FirebaseChess
import chess.chessGame.model.GameLobby
import chess.chessGame.model.Piece

class ChessViewModelFactory(
    val gameId: String,
    val firebaseService: FirebaseChess,
    val userTeam: Piece.Team,
    val currentUserId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChessViewModel(gameId, firebaseService, userTeam, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
