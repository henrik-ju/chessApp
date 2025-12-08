package chess.chessGame.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chess.chessGame.model.*
import viewModel.ChessViewModel

@Composable
fun GameScreen(
    gameId: String,
    viewModel: ChessViewModel
) {
    val game by viewModel.game
    val selected by viewModel.selected
    val moveError by viewModel.moveError
    var promotionPosition by remember { mutableStateOf<Position?>(null)}
    var promotionTeam by remember { mutableStateOf<Piece.Team?>(null) }

    LaunchedEffect(game){
        game.onPromotion = { team, pos ->
            promotionTeam = team
            promotionPosition = pos
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {


        Text(
            text = "Game ID: ${gameId.take(8)}...",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray
        )

        Text(
            text = "You are: ${if (viewModel.userTeam == Piece.Team.WHITE) "White" else "Black"}",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Turn: ${if (game.currentTeam == Piece.Team.WHITE) "White" else "Black"}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (game.currentTeam == viewModel.userTeam) Color.Green else Color.Red
        )

        Spacer(Modifier.height(16.dp))

        moveError?.let { error ->
            Text(
                text = "⚠️ $error",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(4.dp)
                    .clickable { viewModel.clearError() }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(32.dp))

        Chessboard(
            game = game,
            selected = selected,
            userTeam = viewModel.userTeam,
            onSquareClick = viewModel::onSquareClicked
        )

        promotionPosition?.let { pos ->
            PromotionDialog(
                team = promotionTeam!!,
                onSelect = { newPiece ->
                    viewModel.promotePawn(pos, newPiece)
                    promotionPosition = null
                },
                onDismiss = {
                    promotionPosition = null
                }
            )
        }
    }
}


@Composable
fun Chessboard(
    game: ChessGame,
    selected: Position?,
    userTeam: Piece.Team,
    onSquareClick: (Position) -> Unit
) {
    val lightSquare = Color(0xFFEEEDDD)
    val darkSquare = Color(0xFFFFB500)
    val selectedColor = Color.Green

    Column(
        modifier = Modifier
            .size(380.dp)
            .border(4.dp, Color.Black)
            .background(Color(0xFF121212))
    ) {
        val validMoves: Set<Position> = if (selected != null) {
            game.getValidMoves(selected)
        } else {
            emptySet()
        }

        val rowRange = if (userTeam == Piece.Team.WHITE) 0..7 else 7 downTo 0
        val columnRange = if (userTeam == Piece.Team.WHITE) 0..7 else 7 downTo 0

        for (row in rowRange) {
            Row {
                for (col in columnRange) {
                    val pos = Position(row, col)
                    val piece = game.getPieceAt(pos)
                    val isLight = (row + col) % 2 == 0
                    val isSelected = selected == pos
                    val isValidDestination = validMoves.contains(pos)

                    val squareColor = when {
                        isSelected -> selectedColor
                        isLight -> lightSquare
                        else -> darkSquare
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(squareColor)
                            .clickable { onSquareClick(pos) },
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.let {
                            Image(
                                painter = painterResource(id = PieceDrawables.getDrawable(it)),
                                contentDescription = it::class.simpleName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)

                            )
                        }
                    }
                }
            }
        }
    }
}