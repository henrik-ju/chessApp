package chess.chessGame.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import chess.chessGame.model.FirebaseChess
import chess.chessGame.model.GameLobby
import chess.chessGame.model.Piece
import chess.chessGame.view.CreateAccountScreen
import chess.chessGame.view.CreateGameScreen
import chess.chessGame.view.GameScreen
import chess.chessGame.view.LobbyScreen
import chess.chessGame.view.LoginScreen
import kotlinx.coroutines.launch
import viewModel.AuthViewModel
import viewModel.ChessViewModel
import viewModel.ChessViewModelFactory
import viewModel.GameLobbyViewModel

@Composable
fun ChessNavHost(
    navController: NavHostController,
    authVm: AuthViewModel,
    gameLobbyVm: GameLobbyViewModel? = null
) {
    val currentUserEmail by authVm.currentUserEmail.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                vm = authVm,
                onLoginSuccess = { navController.navigate(Screen.Lobby.route) },
                onCreateAccount = { navController.navigate(Screen.CreateAccount.route) }
            )
        }

        composable(Screen.CreateAccount.route) {
            val isLoggedIn by authVm.loggedIn.collectAsState()
            CreateAccountScreen(
                vm = authVm,
            )

            LaunchedEffect(isLoggedIn){
                if(isLoggedIn){
                    navController.navigate(Screen.Lobby.route){
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Lobby.route) {
            if(gameLobbyVm == null){
                Text("Please login")
            } else {
                val scope = rememberCoroutineScope()
                LobbyScreen(
                    vm = gameLobbyVm,
                    navController = navController,
                    onGameSelected = { game: GameLobby ->
                        scope.launch {
                            gameLobbyVm.joinGame(game.id)
                        }
                    }
                )
            }
        }

        composable(Screen.CreateGame.route) {
            if(gameLobbyVm == null) {
                Text("Please login")
            } else {
                CreateGameScreen(
                    vm = gameLobbyVm,
                    navController = navController
                )
            }

        }

        composable(
            route = "${Screen.Game.route}/{gameId}/{playerColor}",
            arguments = listOf(navArgument("gameId") { type = NavType.StringType },
                navArgument("playerColor") { type = NavType.StringType }
                )
        ) { backStackEntry ->

            val gameId = backStackEntry.arguments?.getString("gameId")!!
            val playerColorString = backStackEntry.arguments?.getString("playerColor")!!

            val userTeam = try {
                Piece.Team.valueOf(playerColorString)
            } catch (e: IllegalArgumentException) {
                Piece.Team.WHITE
            }


            val firebaseService = FirebaseChess



            if (currentUserEmail == null) {
                Text("Loading user..")
                return@composable
            }


            val viewModel: ChessViewModel = viewModel(
                factory = ChessViewModelFactory(
                    gameId = gameId,
                    firebaseService = firebaseService,
                    userTeam = userTeam
                )
            )

            GameScreen(
                gameId = gameId,
                viewModel = viewModel
            )
        }
    }
}


