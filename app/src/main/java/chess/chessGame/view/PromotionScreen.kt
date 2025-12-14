package chess.chessGame.view

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
    val pieces = listOf(
        Queen(team, Position(0, 0)),
        Rook(team, Position(0, 0)),
        Bishop(team, Position(0, 0)),
        Knight(team, Position(0, 0))
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White
        ){
            Column(modifier = Modifier.padding(16.dp)){
                Text("Choose a piece", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                pieces.forEach { piece ->
                    Button(
                        onClick = {
                            onSelect(piece)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ){
                        Image(
                            painter = painterResource(id = PieceDrawables.getDrawable(piece)),
                            contentDescription = piece.fenCh.toString(),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }
    }
}