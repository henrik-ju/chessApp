package chess.chessGame.model

class Pawn(team: Team, position: Position?) : Piece(team, position) {
    private val dir = if (isWhite()) -1 else 1
    private val startRow = if (isWhite()) 6 else 1

    override val fenCh: Char = if (team == Team.WHITE) 'P' else 'p'

    override fun getMoves(board: ChessGame): List<Position> {
        val moves = mutableListOf<Position>()
        val current = position ?: return moves
        val (r, c) = current

        val oneStep = Position(r + dir, c)
        if (oneStep.row in 0..7 && board.getPieceAt(oneStep) == null) {
            moves += oneStep
            val twoStep = Position(r + 2 * dir, c)
            if (r == startRow && board.getPieceAt(twoStep) == null) moves += twoStep
        }


        for (dc in listOf(-1, 1)) {
            val nc = c + dc
            val nr = r + dir
            if (nc !in 0..7 || nr !in 0..7) continue
            val target = board.getPieceAt(nr, nc)
            if (isEnemy(target)) moves += Position(nr, nc)

            val enPassantRow = if (isWhite()) 3 else 4
            if (r == enPassantRow && board.lastDoublePushColumn == nc && isEnemy(board.getPieceAt(r, nc)))
                moves += Position(nr, nc)
        }

        return moves
    }

    override fun getLegalMoves(board: ChessGame): List<Position> {
        val current = position ?: return emptyList()
        return getMoves(board).filter { move ->
            !board.wouldBeChecked(current, move, team)
        }
    }
}