package chess.chessGame.model

import kotlin.math.abs

class ChessGame {
    private val board = Array(8) { arrayOfNulls<Piece>(8) }
    private var whiteKingPosition: Position = Position(7, 4)
    private var blackKingPosition = Position(0, 4)
    var isFinished: Boolean = false
        private set

    var currentTeam = Piece.Team.WHITE
        private set

    var lastDoublePushColumn: Int? = null
        private set

    var onPromotion: ((team: Piece.Team, position: Position) -> Unit)? = null

    init {
        setupStartingBoard()
    }

    fun getPieceAt(row: Int, column: Int): Piece? {
        if (row !in 0..7 || column !in 0..7)
            return null
        return board[row][column]
    }
    fun getPieceAt(position: Position): Piece? = getPieceAt(position.row, position.column)

    fun movePiece(from: Position, to: Position): Boolean {
        if(isFinished)
            return false
        val piece = getPieceAt(from) ?: return false
        if (piece.team != currentTeam)
            return false
        val legalMoves = piece.getLegalMoves(this)
        if (to !in legalMoves)
            return false

        val backupTo = getPieceAt(to)
        val fromPosition = piece.position

        var capturedPiece: Piece? = null
        if (piece is Pawn && from.column != to.column && getPieceAt(to) == null) {
            val capturedRow = from.row
            capturedPiece = board[capturedRow][to.column]
            board[capturedRow][to.column] = null
        }

        board[to.row][to.column] = piece
        board[from.row][from.column] = null
        piece.position = to

        val originalCapturedPosition = capturedPiece?.position

        if (isChecked(currentTeam)){
            board[from.row][from.column] = piece
            board[to.row][to.column] = backupTo
            piece.position = fromPosition
            if (capturedPiece != null && originalCapturedPosition != null) {
                board[originalCapturedPosition.row][originalCapturedPosition.column] = capturedPiece
                capturedPiece.position = originalCapturedPosition
            }
            return false
        }

        lastDoublePushColumn = if (piece is Pawn && abs(to.row - from.row) == 2) to.column else null


        if (piece is King) {
            piece.hasMoved = true
            if (piece.isWhite()) whiteKingPosition = to else blackKingPosition = to
        }
        if (piece is Rook)
            piece.hasMoved = true

        if (piece is Pawn && (to.row == 7 || to.row == 0)){
            onPromotion?.invoke(piece.team, to)
        }


        if(piece is King && abs(to.column - from.column) == 2){
            val rookColumn = if (to.column > from.column) 7 else 0
            val newRookColumn = if (to.column > from.column) 5 else 3
            val rook = board[from.row][rookColumn]
            board[from.row][newRookColumn] = rook
            board[from.row][rookColumn] = null
            rook?.position = Position(from.row, newRookColumn)
            rook?.let { (it as? Rook)?.hasMoved = true }

        }

        currentTeam = if (currentTeam == Piece.Team.WHITE) Piece.Team.BLACK else Piece.Team.WHITE

        if (isCheckMate(currentTeam)){
            isFinished = true
            println("Checkmate! ${if (currentTeam == Piece.Team.WHITE) "Black" else "White"} wins!")
        } else if (isChecked(currentTeam)) {
            println("${if (currentTeam == Piece.Team.WHITE) "White" else "Black"} is checked!")
        } else if (isStaleMate(currentTeam)){
            isFinished = true
            println("Stalemate!")
        }
        return true
    }

    private fun setupStartingBoard() {
        repeat(8) { i ->
            board[1][i] = Pawn(Piece.Team.BLACK, Position(1, i))
            board[6][i] = Pawn(Piece.Team.WHITE, Position(6, i))
        }

        val backRank = { team: Piece.Team, row: Int ->
            board[row][0] = Rook(team, Position(row, 0))
            board[row][1] = Knight(team, Position(row, 1))
            board[row][2] = Bishop(team, Position(row, 2))
            board[row][3] = Queen(team, Position(row, 3))
            board[row][4] = King(team, Position(row, 4))
            board[row][5] = Bishop(team, Position(row, 5))
            board[row][6] = Knight(team, Position(row, 6))
            board[row][7] = Rook(team, Position(row, 7))
        }

        backRank(Piece.Team.WHITE, 7)
        backRank(Piece.Team.BLACK, 0)
    }

    fun isChecked(team: Piece.Team): Boolean {
        val kingPosition = if (team == Piece.Team.WHITE) whiteKingPosition else blackKingPosition

        val pawnDir = if (team == Piece.Team.WHITE) -1 else 1
        listOf(-1, 1).forEach { dc ->
            val r = kingPosition.row + pawnDir
            val c = kingPosition.column + dc
            if (r in 0..7 && c in 0..7) {
                val piece = board[r][c]
                if (piece is Pawn && piece.team != team)
                    return true
            }
        }

        val knightMoves = listOf(
            2 to 1, 1 to 2, -1 to 2, -2 to 1,
            -2 to -1, -1 to -2, 1 to -2, 2 to -1
        )
        for ((dr, dc) in knightMoves) {
            val r = kingPosition.row + dr
            val c = kingPosition.column + dc
            if (r in 0..7 && c in 0..7) {
                val piece = board[r][c]
                if (piece is Knight && piece.team != team)
                    return true
            }
        }

        val directions = listOf(
            1 to 0, -1 to 0, 0 to 1, 0 to -1,
            1 to 1, 1 to -1, -1 to 1, -1 to -1
        )
        for ((dr, dc) in directions) {
            var r = kingPosition.row + dr
            var c = kingPosition.column + dc
            while (r in 0..7 && c in 0..7) {
                val piece = board[r][c]
                if (piece != null) {
                    if (piece.team != team) {
                        if ((dr == 0 || dc == 0) && (piece is Rook || piece is Queen))
                            return true
                        if ((dr != 0 && dc != 0) && (piece is Bishop || piece is Queen))
                            return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = kingPosition.row + dr
                val c = kingPosition.column + dc
                if (r in 0..7 && c in 0..7) {
                    val piece = board[r][c]
                    if (piece is King && piece.team != team) return true
                }
            }
        }
        return false
    }
    fun isCheckMate(team: Piece.Team): Boolean {
        if (!isChecked(team))
            return false

        for (row in 0..7){
            for (column in 0..7){
                val piece = board[row][column] ?: continue
                if (piece.team != team)
                    continue

                val legalMoves = piece.getLegalMoves(this)
                for(move in legalMoves){
                    if (!wouldBeChecked(piece.position!!, move, team))
                        return false
                }
            }
        }
        return true
    }
    fun wouldBeChecked(from: Position, to: Position, team: Piece.Team): Boolean {
        val piece = getPieceAt(from) ?: return false
        val targetBackup = getPieceAt(to)
        val originalKingPos = if (team == Piece.Team.WHITE) whiteKingPosition else blackKingPosition
        val originalPiecePosition = piece.position
        var capturedPawn: Piece? = null
        var capturedPawnPos: Position? = null

        if (piece is Pawn && from.column != to.column && targetBackup == null) {
            val capturedRow = from.row
            capturedPawn = board[capturedRow][to.column]
            capturedPawnPos = capturedPawn?.position
            board[capturedRow][to.column] = null
        }
        board[to.row][to.column] = piece
        board[from.row][from.column] = null
        piece.position = to

        var tempKingPos: Position? = null
        if (piece is King) {
            tempKingPos = originalKingPos
            if (team == Piece.Team.WHITE) whiteKingPosition = to else blackKingPosition = to
        }

        val inCheck = isChecked(team)

        if (piece is King) {
            if (team == Piece.Team.WHITE) whiteKingPosition = tempKingPos!! else blackKingPosition = tempKingPos!!
        }
        board[from.row][from.column] = piece
        board[to.row][to.column] = targetBackup
        piece.position = originalPiecePosition
        if (capturedPawn != null && capturedPawnPos != null) {
            board[capturedPawnPos.row][capturedPawnPos.column] = capturedPawn
            capturedPawn.position = capturedPawnPos
        }

        return inCheck
    }
    fun isStaleMate(team: Piece.Team): Boolean {
        if(isChecked(team))
            return false

        for(row in 0..7){
            for(column in 0..7){
                val piece = board[row][column] ?: continue

                if(piece.team == team) {
                    val legalMoves = piece.getLegalMoves(this)
                    for(move in legalMoves){
                        if(!wouldBeChecked(piece.position!!, move, team)){
                            return false
                        }
                    }
                }
            }
        }
        return true
    }
    fun setPieceAt(position: Position, piece: Piece?){
        board[position.row][position.column] = piece
        piece?.position = position
    }
    private fun clearBoard() {
        for (r in 0..7) for (c in 0..7) board[r][c] = null
    }
    fun toFen(): String {
        val sb = StringBuilder()

        for (row in 0..7) {
            var empty = 0
            for (col in 0..7) {
                val p = getPieceAt(row, col)
                if (p == null) empty++ else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(p.fenCh)
                }
            }
            if (empty > 0) sb.append(empty)
            if (row < 7) sb.append('/')
        }
        sb.append(if (currentTeam == Piece.Team.WHITE) " w" else " b")

        val whiteKing = getPieceAt(whiteKingPosition) as? King
        val blackKing = getPieceAt(blackKingPosition) as? King

        val whiteKingRook = getPieceAt(Position(7, 7)) as? Rook
        val whiteQueenRook = getPieceAt(Position(7, 0)) as? Rook
        val blackKingRook = getPieceAt(Position(0, 7)) as? Rook
        val blackQueenRook = getPieceAt(Position(0, 0)) as? Rook

        var castlingRights = ""
        if (whiteKing != null && !whiteKing.hasMoved) {
            if (whiteKingRook != null && !whiteKingRook.hasMoved) castlingRights += "K"
            if (whiteQueenRook != null && !whiteQueenRook.hasMoved) castlingRights += "Q"
        }
        if (blackKing != null && !blackKing.hasMoved) {
            if (blackKingRook != null && !blackKingRook.hasMoved) castlingRights += "k"
            if (blackQueenRook != null && !blackQueenRook.hasMoved) castlingRights += "q"
        }
        if (castlingRights.isEmpty()) castlingRights = "-"

        sb.append(" $castlingRights")

        val enPassantSquare = if (lastDoublePushColumn != null) {
            val rankNumber = if (currentTeam == Piece.Team.BLACK) 6 else 3
            "${'a' + lastDoublePushColumn!!}$rankNumber"
        } else {
            "-"
        }
        sb.append(" $enPassantSquare")
        sb.append(" 0")
        sb.append(" 1")
        return sb.toString()
    }

    companion object {
        fun fromFen(fen: String): ChessGame {
            val game = ChessGame()
            game.clearBoard()

            val parts = fen.split(" ")
            val rows = parts[0].split("/")
            val castlingRights = parts.getOrNull(2) ?: "-"
            val enPassantTarget = parts.getOrNull(3) ?: "-"

            for (r in 0..7) {
                var c = 0
                for (ch in rows[r]) {
                    if (ch.isDigit()) {
                        c += ch.digitToInt()
                    } else {
                        if (c >= 8) break

                        val isWhite = ch.isUpperCase()
                        val team = if (isWhite) Piece.Team.WHITE else Piece.Team.BLACK
                        val type = ch.uppercaseChar()

                        val piece: Piece? = when (type) {
                            'K' -> King(team, Position(r, c)).apply {
                                if (team == Piece.Team.WHITE) {
                                    game.whiteKingPosition = Position(r, c)

                                    hasMoved = !(castlingRights.contains('K') || castlingRights.contains('Q'))
                                } else {
                                    game.blackKingPosition = Position(r, c)

                                    hasMoved = !(castlingRights.contains('k') || castlingRights.contains('q'))
                                }
                            }
                            'Q' -> Queen(team, Position(r, c))
                            'R' -> Rook(team, Position(r, c)).apply {

                                if (team == Piece.Team.WHITE) {
                                    if (r == 7 && c == 7) hasMoved = !castlingRights.contains('K')
                                    if (r == 7 && c == 0) hasMoved = !castlingRights.contains('Q')
                                } else {
                                    if (r == 0 && c == 7) hasMoved = !castlingRights.contains('k')
                                    if (r == 0 && c == 0) hasMoved = !castlingRights.contains('q')
                                }
                            }
                            'B' -> Bishop(team, Position(r, c))
                            'N' -> Knight(team, Position(r, c))
                            'P' -> Pawn(team, Position(r, c))
                            else -> null
                        }
                        piece?.let { game.board[r][c] = it }
                        c++
                    }
                }
            }
            game.currentTeam = if (parts.getOrNull(1) == "w") Piece.Team.WHITE else Piece.Team.BLACK

            if (enPassantTarget != "-") {

                val fileChar = enPassantTarget[0]
                game.lastDoublePushColumn = fileChar - 'a'
            } else {
                game.lastDoublePushColumn = null
            }
            return game
        }
    }
}