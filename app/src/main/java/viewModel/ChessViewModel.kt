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

    private val _game = mutableStateOf(ChessGame())
    val game: State<ChessGame> = _game

    val _selected = mutableStateOf<Position?>(null)
    val selected: State<Position?> = _selected

    private val _moveError = mutableStateOf<String?>(null)
    val moveError: State<String?> = _moveError

    init {
        firebaseService.listenToGame(gameId) { lobby ->
            lobby.fen?.let { fen ->

                if (fen.isNotEmpty() && fen != _game.value.toFen()) {
                    _game.value = ChessGame.fromFen(fen)
                    _selected.value = null
                }
            }
        }
    }

    fun onSquareClicked(pos: Position) {
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
                        val newFen = currentGame.toFen()
                        val transactionSuccess = firebaseService.updateGameFenTransaction(gameId, newFen)

                        withContext(Dispatchers.Main) {
                            if (transactionSuccess) {

                                _game.value = currentGame
                                _selected.value = null
                                _moveError.value = null
                            } else {
                                _moveError.value = "Move failed: Someone moved first. Reverting..."
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
                            _moveError.value = "Illegal Move."
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (!success && piece?.team == userTeam) {
                            _selected.value = pos
                        }
                    }
                }
            }
        }
    }

    fun promotePawn(position: Position, newPiece: Piece){
        _game.value.setPieceAt(position, newPiece)
    }


    fun clearError() {
        _moveError.value = null
    }
}