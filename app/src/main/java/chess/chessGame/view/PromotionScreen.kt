package chess.chessGame.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chess.chessGame.model.Bishop
import chess.chessGame.model.Knight
import chess.chessGame.model.Piece
import chess.chessGame.model.Position
import chess.chessGame.model.Queen
import chess.chessGame.model.Rook


@Composable
fun PromotionDialog(
    team: Piece.Team,
    onSelect: (Piece) -> Unit,
    onDismiss: () -> Unit
){
    val pieces = listOf("Queen", "Rook", "Bishop", "Knight")
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White
        ){
            Column(modifier = Modifier.padding(16.dp)){
                Text("Choose a piece", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                pieces.forEach { pieceName ->
                    Button(
                        onClick = {
                            val position = Position(0, 0)
                            val piece = when(pieceName){
                                "Queen" -> Queen(team, position)
                                "Rook" -> Rook(team, position)
                                "Bishop" -> Bishop(team, position)
                                "Knight" -> Knight(team, position)
                                else -> null
                            }
                            piece?.let { onSelect(it) }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ){
                        Text(pieceName)
                    }
                }
            }
        }
    }
}