package viewModel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chess.chessGame.model.FirebaseChess
import chess.chessGame.model.GameLobby
import kotlinx.coroutines.launch
import java.util.UUID

class GameLobbyViewModel(
    val currentUser: String
) : ViewModel() {

    private val firebaseService = FirebaseChess()
    val games = mutableStateListOf<GameLobby>()

    init {
        firebaseService.listenToAllGames { lobby ->
            val index = games.indexOfFirst { it.id == lobby.id }
            if (index >= 0) {

                games[index] = lobby
            } else {

                games += lobby
            }
        }
    }

    fun startNewGame(): GameLobby {
        val newGame = GameLobby(
            id = UUID.randomUUID().toString(),
            whitePlayer = currentUser,
            blackPlayer = null,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" // Standard starting FEN
        )
        viewModelScope.launch {
            firebaseService.createGame(newGame)
        }
        return newGame
    }


    suspend fun joinGame(gameId: String): GameLobby? {
        return firebaseService.joinGame(gameId, currentUser)
    }
}