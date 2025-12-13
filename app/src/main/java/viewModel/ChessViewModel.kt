package viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chess.chessGame.model.ChatMessage
import chess.chessGame.model.ChessGame
import chess.chessGame.model.Crypting
import chess.chessGame.model.FirebaseChess
import chess.chessGame.model.Piece
import chess.chessGame.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

class ChessViewModel(
    val gameId: String,
    val firebaseService: FirebaseChess,
    val userTeam: Piece.Team,
    val currentUserId: String
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

    private var gameSessionSecretKey: SecretKey? = null

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _decryptedChatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val decryptedChatMessages: StateFlow<List<Pair<String, String>>> = _decryptedChatMessages

    private var chatListenerRegistration: Any? = null

    init {
        _game.value.onPromotion = { team, position ->
            _promotionPosition.value = position
        }

        viewModelScope.launch {
            try {

                val newSerializedKey = Crypting.generateNewSecretKey()
                val finalSerializedKey =
                    firebaseService.getOrCreateGameKey(gameId, newSerializedKey)
                gameSessionSecretKey = Crypting.getSecretKey(finalSerializedKey)


                chatListenerRegistration = firebaseService.listenToChat(gameId) { newMessages ->
                    processEncryptedMessages(newMessages)
                }

            } catch (e: Exception) {
                println("ERROR: Failed to initialize symmetric key and chat listener: ${e.message}")
            }
        }

        gameListenerRegistration = firebaseService.listenToGame(gameId) { lobby ->

            _playersJoined.value = lobby.playerCount
            println("DEBUG: Lobby Update Received for Black. Players: ${lobby.playerCount}, FEN: ${lobby.fen}")

            lobby.fen?.let { fen ->
                if (fen.isNotEmpty() && fen != _game.value.toFen()) {
                    println("DEBUG: Black is updating board with FEN: $fen")
                    val newGame = ChessGame.fromFen(fen)
                    newGame.onPromotion = { team, position ->
                        _promotionPosition.value = position
                    }
                    _game.value = newGame
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
                        if (_promotionPosition.value != null) {
                            withContext(Dispatchers.Main) {
                                _selected.value = null
                                _moveError.value = null
                            }

                            return@launch
                        }
                        val newFen = currentGame.toFen()
                        val transactionSuccess =
                            firebaseService.updateGameFenTransaction(gameId, newFen)

                        withContext(Dispatchers.Main) {
                            if (transactionSuccess) {
                                _game.value = currentGame
                                _selected.value = null
                                _moveError.value = null
                            } else {
                                _moveError.value = "Move failed, remote update happened first."
                                firebaseService.getGame(gameId)?.fen?.let { remoteFen ->
                                    if (remoteFen.isNotEmpty()) {
                                        val remoteGame = ChessGame.fromFen(remoteFen)
                                        remoteGame.onPromotion = { team, position -> _promotionPosition.value = position }
                                        _game.value = remoteGame
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

    fun promotePawn(newPiece: Piece) {
        val promotionSquare = _promotionPosition.value ?: return
        val gameInstance = _game.value

        viewModelScope.launch(Dispatchers.Default) {


            gameInstance.setPieceAt(promotionSquare, newPiece)

            withContext(Dispatchers.Main) {
                _game.value = gameInstance
                _promotionPosition.value = null
            }

            val newFen = gameInstance.toFen()
            val transactionSuccess = firebaseService.updateGameFenTransaction(gameId, newFen)

            withContext(Dispatchers.Main) {
                if (transactionSuccess) {
                    _selected.value = null
                    _moveError.value = null
                } else {
                    _moveError.value = "Promotion failed, remote update happened first."
                    firebaseService.getGame(gameId)?.fen?.let { remoteFen ->
                        if (remoteFen.isNotEmpty()) {
                            val remoteGame = ChessGame.fromFen(remoteFen)
                            remoteGame.onPromotion = { team, position -> _promotionPosition.value = position }
                            _game.value = remoteGame
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _moveError.value = null
    }

    fun processEncryptedMessages(encryptedList: List<ChatMessage>) {
        val secretKey = gameSessionSecretKey

        if (secretKey == null) {
            println("DEBUG_KEY: CANNOT DECRYPT. SecretKey is NULL when messages arrived.")
            return
        }

        val decryptedList = encryptedList.mapNotNull { msg ->

            val encryptedContent = msg.content ?: return@mapNotNull null

            try {
                val decryptedText = Crypting.decrypt(encryptedContent, secretKey)

                val senderLabel = if (msg.senderId == currentUserId) "You" else "Opponent"
                senderLabel to decryptedText
            } catch (e: Exception) {
                println("ERROR: Failed to decrypt message: ${e.message}")
                val senderLabel = if (msg.senderId == currentUserId) "You" else "Opponent"
                senderLabel to "[Decryption Failed - Check Keys]"
            }
        }
        (_chatMessages as MutableStateFlow).value = encryptedList
        _decryptedChatMessages.value = decryptedList
    }

    fun sendChatMessage(plaintext: String) {
        val secretKey = gameSessionSecretKey
        if (secretKey == null){
            println("DEBUG_KEY: CANNOT SEND. SecretKey is NULL.")
            return
        }

        if(plaintext.isBlank())
            return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ciphertext = Crypting.encrypt(plaintext, secretKey)

                val message = ChatMessage(
                    senderId = currentUserId,
                    timestamp = System.currentTimeMillis(),
                    content = ciphertext
                )
                firebaseService.sendChatMessage(gameId, message)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _moveError.value = "Failed to send encrypted message."
                }
            }
        }
    }
}