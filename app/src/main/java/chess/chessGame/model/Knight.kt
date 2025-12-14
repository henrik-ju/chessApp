package chess.chessGame.model

class Knight(team: Team, position: Position?) : Piece(team, position) {
    override val fenCh: Char = if (team == Team.WHITE) 'N' else 'n'

    override fun getLegalMoves(board: ChessGame): List<Position> {
        return getMoves(board).filter { move ->
            !board.wouldBeChecked(position!!, move, team)
        }
    }
    override fun getMoves(board: ChessGame): List<Position> {
        val deltas = listOf(
            2 to 1, 1 to 2, -1 to 2, -2 to 1,
            -2 to -1, -1 to -2, 1 to -2, 2 to -1
        )
        return addMoves(board, deltas)
    }
}


