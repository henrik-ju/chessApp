package chess.chessGame.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chess.chessGame.model.GameLobby
import viewModel.GameLobbyViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import chess.chessGame.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun LobbyScreen(
    vm: GameLobbyViewModel,
    navController: NavHostController,
    onGameSelected: (GameLobby) -> Unit

) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ){
        Text("Chess Game Lobby", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier
            .height(16.dp)
        )

        Button(
            onClick = {
                navController.navigate(Screen.CreateGame.route)
            },
            modifier = Modifier
                .fillMaxWidth()

        ){
            Text("Start New Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)){
            items(vm.games) { game ->
                GameLobbyItem(
                    game = game,
                    currentUser = vm.currentUser,
                    onJoin = {
                        scope.launch {
                            val joined = vm.joinGame(game.id)
                            if (joined != null) {
                                onGameSelected(joined)
                            } else {
                                println("Failed to join game ${game.id.take(4)}. It may be full or a network issue occurred.")
                            }
                        }
                    },
                    onResume = { onGameSelected(game) }
                )
            }
        }
    }
}

@Composable
fun GameLobbyItem(
    game: GameLobby,
    currentUser: String,
    onJoin: () -> Unit,
    onResume: () -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column{
                Text("Game ${game.id.take(4)}")
                Text("White ${game.whitePlayer ?: "-"}")
                Text("Black ${game.blackPlayer ?: "-"}")

            }
            when {
                game.whitePlayer == currentUser || game.blackPlayer == currentUser ->
                    Button(onClick = onResume){
                        Text("Resume")
                    }
                game.whitePlayer == null || game.blackPlayer == null ->
                    Button(onClick = onJoin){
                        Text("Join")
                    }
                else -> Text("Full Game")

            }
        }

    }

}

