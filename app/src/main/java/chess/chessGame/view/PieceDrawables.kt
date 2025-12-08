package chess.chessGame.view

import androidx.annotation.DrawableRes
import chess.chessGame.model.*
import chess.chessGame.R

object PieceDrawables {
    @DrawableRes
    fun getDrawable(piece: Piece): Int {
        return when(piece) {
            is Pawn -> if (piece.isWhite()) R.drawable.white_pawn else R.drawable.black_pawn
            is Rook -> if (piece.isWhite()) R.drawable.white_rook else R.drawable.black_rook
            is Knight -> if (piece.isWhite()) R.drawable.white_knight else R.drawable.black_knight
            is Bishop -> if (piece.isWhite()) R.drawable.white_bishop else R.drawable.black_bishop
            is Queen -> if (piece.isWhite()) R.drawable.white_queen else R.drawable.black_queen
            is King -> if (piece.isWhite()) R.drawable.white_king else R.drawable.black_king
            else -> 0
        }
    }
}