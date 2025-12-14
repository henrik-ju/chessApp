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
import chess.chessGame.viewModel.GameLobbyViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import chess.chessGame.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun LobbyScreen(
    vm: GameLobbyViewModel,
    navController: NavHostController,
    onGameSelected: (GameLobby) -> Unit,
    onLogout: () -> Unit,
    userRole: String,
    onAccountSettingsClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ){
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chess Game Lobby", style = MaterialTheme.typography.headlineSmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (userRole != "guest") {
                    IconButton(
                        onClick = onAccountSettingsClick,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Account Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(
                    onClick = onLogout
                ) {
                    Text("Logout")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (userRole == "guest") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create an account to start or join a game.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate(Screen.Login.route) }) {
                    Text("Log In / Sign Up")
                }
            }
        } else {
            Button(
                onClick = {
                    navController.navigate(Screen.CreateGame.route)
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Start New Game")
            }
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
                    onResume = { onGameSelected(game) },
                    userRole = userRole
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
    onResume: () -> Unit,
    userRole: String
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
            Column(modifier = Modifier.weight(1f)) {
                Text("Game ${game.id.take(4)}")
                Text("White ${game.whitePlayer ?: "-"}")
                Text("Black ${game.blackPlayer ?: "-"}")
            }
            when {
                game.whitePlayer == currentUser || game.blackPlayer == currentUser ->
                    Button(onClick = onResume){
                        Text("Resume")
                    }
                userRole != "guest" && (game.whitePlayer == null || game.blackPlayer == null) ->
                    Button(onClick = onJoin){
                        Text("Join")
                    }
                userRole == "guest" && (game.whitePlayer == null || game.blackPlayer == null) ->
                    Text("Available")
                else -> Text("Full Game")
            }
        }
    }
}