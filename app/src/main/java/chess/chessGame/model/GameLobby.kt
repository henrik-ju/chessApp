package chess.chessGame.model

import com.google.firebase.database.Exclude
data class GameLobby(
    val id: String ="",
    val whitePlayer: String? = null,
    val blackPlayer: String? = null,
    val fen: String? = "",
    val chatKey: String? = null,
    val messages: Map<String, Any>? = null
) {
    @get:Exclude
    val playerCount: Int
        get() = listOfNotNull(whitePlayer, blackPlayer).size
}