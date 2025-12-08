package chess.chessGame.model

abstract class Piece(
    val team: Team,
    var position: Position?
) {
    enum class Team {
        WHITE,
        BLACK
    }

    abstract fun getMoves(board: ChessGame): List<Position>

    open fun getLegalMoves(board: ChessGame): List<Position> {
        val currentPos = position ?: return emptyList()
        return getMoves(board).filter { move ->
            !board.wouldBeChecked(currentPos, move, team)
        }
    }

    fun isWhite() = team == Team.WHITE

    fun isEnemy(other: Piece?) = other != null && other.team != team

    protected fun addLineMoves(board: ChessGame, directions: List<Pair<Int, Int>>) : List<Position> {
        val moves = mutableListOf<Position>()
        val currentPosition = position ?: return emptyList()
        val (row, column) = currentPosition

        for ((dr, dc) in directions){
            var r = row + dr
            var c = column + dc

            while (r in 0..7 && c in 0..7){
                val piece = board.getPieceAt(r, c)
                if (piece == null) {
                    moves += Position(r, c)
                } else {
                    if (isEnemy(piece)) moves += Position(r, c)
                    break
                }
                r += dr
                c += dc
            }
        }
        return moves
    }

    protected fun addMoves(board: ChessGame, deltas: List<Pair<Int, Int>>): List<Position> {
        val currentPosition = position ?: return emptyList()
        val (row, column) = currentPosition

        return deltas.mapNotNull { (dr, dc) ->
            val r = row + dr
            val c = column + dc

            if (r in 0..7 && c in 0..7) {
                val target = board.getPieceAt(r, c)
                if(target == null || isEnemy(target)) Position(r, c) else null
            } else null
        }
    }
}