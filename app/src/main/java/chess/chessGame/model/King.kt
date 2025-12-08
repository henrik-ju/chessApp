package chess.chessGame.model

class King(team: Team, position: Position?) : Piece(team, position) {
    var hasMoved = false

    override fun getLegalMoves(board: ChessGame): List<Position> {
        return getMoves(board).filter { move ->
            !board.wouldBeChecked(position!!, move, team)
        }
    }

    override fun getMoves(board: ChessGame): List<Position> {
        val deltas = listOf(
            1 to 0, -1 to 0, 0 to 1, 0 to -1,
            1 to 1, 1 to -1, -1 to 1, -1 to -1
        )
        val moves = addMoves(board, deltas).toMutableList()

        if (!hasMoved) {
            val row = position!!.row
            val kingsideRook = board.getPieceAt(row, 7)
            if (kingsideRook is Rook && !kingsideRook.hasMoved) {
                if ((5..6).all { board.getPieceAt(row, it) == null } &&
                    (5..6).all { !board.wouldBeChecked(position!!, Position(row, it), team) }
                ) {
                    moves += Position(row, 6)
                }
            }

            val queensideRook = board.getPieceAt(row, 0)
            if (queensideRook is Rook && !queensideRook.hasMoved) {
                if ((1..3).all { board.getPieceAt(row, it) == null } &&
                    (2..3).all { !board.wouldBeChecked(position!!, Position(row, it), team) }
                ) {
                    moves += Position(row, 2)
                }
            }
        }

        return moves
    }
}





