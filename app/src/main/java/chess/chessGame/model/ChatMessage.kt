package chess.chessGame.model

data class ChatMessage(
    val senderId: String? = "",
    val timestamp: Long = 0,
    val content: String? = ""
)