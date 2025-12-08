package chess.chessGame.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import viewModel.AuthViewModel
import viewModel.ChessViewModel
import viewModel.ChessViewModelFactory
import viewModel.GameLobbyViewModel

@Composable
fun ChessNavHost(
    navController: NavHostController,
    authVm: AuthViewModel,
    gameLobbyVm: GameLobbyViewModel
) {
    val currentUserEmail by authVm.currentUserEmail.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ){
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

            if (isLoggedIn) {
                androidx.compose.runtime.LaunchedEffect(key1 = Unit) {
                    navController.navigate(Screen.Lobby.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Lobby.route) {
            LobbyScreen(
                vm = gameLobbyVm,
                navController = navController,
                onGameSelected = { game: GameLobby ->
                    navController.navigate("${Screen.Game.route}/${game.id}")
                }
            )
        }

        composable(Screen.CreateGame.route) {
            CreateGameScreen(
                vm = gameLobbyVm,
                navController = navController
            )
        }

        composable(
            route = "${Screen.Game.route}/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId")!!
            val firebaseService = FirebaseChess()

            val gameLobby = gameLobbyVm.games.find { it.id == gameId }

            val userTeam = when (currentUserEmail) {
                gameLobby?.whitePlayer -> Piece.Team.WHITE
                gameLobby?.blackPlayer -> Piece.Team.BLACK
                else -> {
                    Piece.Team.WHITE
                }
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