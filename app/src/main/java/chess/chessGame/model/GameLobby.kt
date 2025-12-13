package chess.chessGame.model

data class GameLobby(
    val id: String ="",
    val whitePlayer: String? = null,
    val blackPlayer: String? = null,
    val fen: String? = ""
) {
    val players: Int
        get() = listOfNotNull(whitePlayer, blackPlayer).size
}