package chess.chessGame.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import chess.chessGame.navigation.Screen
import viewModel.GameLobbyViewModel

@Composable
fun CreateGameScreen(
    vm: GameLobbyViewModel,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("New Game", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val newGame = vm.startNewGame()
                navController.navigate("${Screen.Game.route}/${newGame.id}"){
                    popUpTo(Screen.Lobby.route) { inclusive = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Create Game")
        }

    }


}