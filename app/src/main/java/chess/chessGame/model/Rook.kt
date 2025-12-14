package chess.chessGame.model

class Rook(team: Team, position: Position?) : Piece(team, position) {
    var hasMoved = false
    override val fenCh: Char = if (team == Team.WHITE) 'R' else 'r'

    override fun getLegalMoves(board: ChessGame): List<Position> {
        return getMoves(board).filter { move ->
            !board.wouldBeChecked(position!!, move, team)
        }
    }
    override fun getMoves(board: ChessGame): List<Position> {
        val directions = listOf(
            1 to 0, -1 to 0, 0 to 1, 0 to -1
        )
        return addLineMoves(board, directions)
    }
}
