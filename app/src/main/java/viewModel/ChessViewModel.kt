package viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chess.chessGame.model.ChessGame
import chess.chessGame.model.FirebaseChess
import chess.chessGame.model.Piece
import chess.chessGame.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChessViewModel(
    val gameId: String,
    val firebaseService: FirebaseChess,
    val userTeam: Piece.Team
) : ViewModel() {

    private val _playersJoined = mutableStateOf(0)
    val playersJoined: State<Int> = _playersJoined

    private val _game = mutableStateOf(ChessGame())
    val game: State<ChessGame> = _game

    private val _selected = mutableStateOf<Position?>(null)
    val selected: State<Position?> = _selected

    private val _moveError = mutableStateOf<String?>(null)
    val moveError: State<String?> = _moveError

    private var gameListenerRegistration: Any? = null

    private val _promotionPosition = mutableStateOf<Position?>(null)
    val promotionPosition: State<Position?> = _promotionPosition
    private val _selectedPromotionPiece = mutableStateOf<Piece?>(null)
    val selectedPromotionPiece: State<Piece?> = _selectedPromotionPiece

    init {
        _game.value.onPromotion = { team, position ->
            _promotionPosition.value = position
        }


        gameListenerRegistration = firebaseService.listenToGame(gameId) { lobby ->

            _playersJoined.value = lobby.players
            println("DEBUG: Lobby Update Received for Black. Players: ${lobby.players}, FEN: ${lobby.fen}")

            lobby.fen?.let { fen ->
                if (fen.isNotEmpty() && fen != _game.value.toFen()) {
                    println("DEBUG: Black is updating board with FEN: $fen")
                    _game.value = ChessGame.fromFen(fen)
                    _selected.value = null
                }
            }
        }
    }

    fun onSquareClicked(pos: Position) {
        if (playersJoined.value < 2) {
            _moveError.value = "Waiting for an opponent..."
            return
        }

        val currentGame = _game.value
        val piece = currentGame.getPieceAt(pos)

        if (currentGame.currentTeam != userTeam) {
            _moveError.value = "It's not your turn!"
            return
        }

        when {
            _selected.value == null -> {
                if (piece?.team == currentGame.currentTeam) {
                    _selected.value = pos
                }
            }

            _selected.value == pos -> _selected.value = null

            else -> {
                val startPos = _selected.value!!

                viewModelScope.launch(Dispatchers.Default) {
                    val success = currentGame.movePiece(startPos, pos)

                    if (success) {
                        if (_game.value.onPromotion != null && _promotionPosition.value != null) {
                            withContext(Dispatchers.Main) {
                                _selected.value = null
                                _moveError.value = null
                            }
                            return@launch
                        }
                        val newFen = currentGame.toFen()
                        val transactionSuccess = firebaseService.updateGameFenTransaction(gameId, newFen)

                        withContext(Dispatchers.Main) {
                            if (transactionSuccess) {
                                _game.value = currentGame
                                _selected.value = null
                                _moveError.value = null
                            } else {
                                _moveError.value = "Move failed, remote update happened first."
                                firebaseService.getGame(gameId)?.fen?.let { remoteFen ->
                                    if (remoteFen.isNotEmpty()) {
                                        _game.value = ChessGame.fromFen(remoteFen)
                                    }
                                }
                                _selected.value = null
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _selected.value = null
                            _moveError.value = "Illegal move."
                        }
                    }
                }
            }
        }
    }

    fun promotePawn(position: Position, newPiece: Piece) {
        val position = _promotionPosition.value ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _game.value.setPieceAt(position, newPiece)
            _promotionPosition.value = null
            val newFen = _game.value.toFen()
            val transactionSuccess = firebaseService.updateGameFenTransaction(gameId, newFen)

            withContext(Dispatchers.Main) {
                if (transactionSuccess) {
                    _game.value = _game.value
                    _selected.value = null
                    _moveError.value = null
                } else {
                    _moveError.value = "Promotion failed, remote update happened first."
                }
            }
        }
    }

    fun clearError() {
        _moveError.value = null
    }
}
