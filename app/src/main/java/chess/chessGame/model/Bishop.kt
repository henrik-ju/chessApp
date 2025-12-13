package chess.chessGame.model

class Bishop(team: Team, position: Position?) : Piece(team, position) {

    override val fenCh: Char = if (team == Team.WHITE) 'B' else 'b'

    override fun getLegalMoves(board: ChessGame): List<Position> {
        return getMoves(board).filter { move ->
            !board.wouldBeChecked(position!!, move, team)
        }
    }
    override fun getMoves(board: ChessGame): List<Position> {
        val directions = listOf(
            1 to 1, 1 to -1, -1 to 1, -1 to -1
        )
        return addLineMoves(board, directions)
    }
}
