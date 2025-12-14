package chess.chessGame.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import chess.chessGame.model.Piece
import kotlinx.coroutines.launch
import chess.chessGame.viewModel.GameLobbyViewModel

@Composable
fun CreateGameScreen(
    vm: GameLobbyViewModel,
    navController: NavHostController
) {
    val scope = rememberCoroutineScope()

    var selectedTeam by rememberSaveable { mutableStateOf(Piece.Team.WHITE) }
    var creationError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("New Game", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        Text("Choose Your Color", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            ColorSelectionButton(
                team = Piece.Team.WHITE,
                isSelected = selectedTeam == Piece.Team.WHITE,
                onSelect = { selectedTeam = Piece.Team.WHITE }
            )
            Spacer(modifier = Modifier.width(16.dp))
            ColorSelectionButton(
                team = Piece.Team.BLACK,
                isSelected = selectedTeam == Piece.Team.BLACK,
                onSelect = { selectedTeam = Piece.Team.BLACK }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        creationError?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    creationError = null
                    val newGameId = vm.startNewGame(selectedTeam)

                    if (newGameId == null) {
                        creationError = "Failed to create game."
                    }

                }
            }
        ) {
                Text("Create Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                navController.popBackStack()
            }
        ) {
            Text("Back")
        }
    }
}
@Composable
fun ColorSelectionButton(
    team: Piece.Team,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = if (team == Piece.Team.WHITE) Color.White else Color.Black
    val contentColor = if (team == Piece.Team.WHITE) Color.Black else Color.White
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)

    OutlinedButton(
        onClick = onSelect,
        modifier = Modifier
            .size(width = 120.dp, height = 60.dp)
            .alpha(if (isSelected) 1f else 0.6f),
        border = BorderStroke(
            width = if (isSelected) 4.dp else 1.dp,
            color = borderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(8.dp) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = team.name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}