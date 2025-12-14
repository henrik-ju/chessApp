package viewModel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chess.chessGame.model.FirebaseChess
import chess.chessGame.model.GameLobby
import chess.chessGame.model.Piece
import com.google.firebase.database.ChildEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class GameLobbyViewModel(
    private var _currentUser: String,
    val firebaseService: FirebaseChess,
    private val onNavigateToGame: (gameId: String, assignedColor: String) -> Unit
) : ViewModel() {

    val games = mutableStateListOf<GameLobby>()
    private var listenerRegistration: Any? = null

    private var listeningStarted = false

    init {
        if (_currentUser != "Guest" && _currentUser != "initializing"){
            startListening()
        }
    }

    fun onUserLoggedIn(newUid: String) {
        if (_currentUser != newUid) {
            stopListening()
            _currentUser = newUid
            startListening()
        }
    }

    fun startListening() {
        println("startListening called, currentUser = $_currentUser, listeningStarted = $listeningStarted")
        if(!listeningStarted){
            listeningStarted = true
            listenerRegistration = firebaseService.listenToAllGames { list ->
                println("Lobby Update Received: ${list.size} games. First game ID: ${list.firstOrNull()?.id?.take(4)}")
                viewModelScope.launch(Dispatchers.Main){
                    games.clear()
                    games.addAll(list)
                }
            }
        }
    }

    fun stopListening() {
        if (listeningStarted) {
            (listenerRegistration as? ChildEventListener)?.let { listener ->
                firebaseService.stopListeningToAllGames(listener)
            }
            listeningStarted = false
        }
    }

    suspend fun startNewGame(playerTeam: Piece.Team): String? {

        val whitePlayerId: String? = if (playerTeam == Piece.Team.WHITE) _currentUser else null
        val blackPlayerId: String? = if (playerTeam == Piece.Team.BLACK) _currentUser else null

        val newGame = GameLobby(
            id = UUID.randomUUID().toString(),
            whitePlayer = whitePlayerId,
            blackPlayer = blackPlayerId,
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )

        return try {
            firebaseService.createGame(newGame)

            withContext(Dispatchers.Main) {
                onNavigateToGame(newGame.id, playerTeam.name)
            }

            newGame.id

        } catch (e: Exception) {
            println("Error creating new game: ${e.message}")
            null
        }
    }

    suspend fun joinGame(gameId: String): GameLobby? {
        val updatedGame = firebaseService.joinGame(gameId, _currentUser)

        if(updatedGame != null) {
            val assignedColor: String = when (_currentUser) {
                updatedGame.whitePlayer -> "WHITE"
                updatedGame.blackPlayer -> "BLACK"
                else -> {
                    println("Joined game, but NOT assigned color")
                    return updatedGame
                }
            }

            withContext(Dispatchers.Main) {
                onNavigateToGame(gameId, assignedColor)
            }
        }
        return updatedGame
    }

    val currentUser: String
        get() = _currentUser
}