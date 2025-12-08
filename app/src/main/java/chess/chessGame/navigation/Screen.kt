package chess.chessGame.navigation

sealed class Screen(val route: String) {
    object Login : Screen("Login")
    object CreateAccount : Screen("CreateAccount")
    object Lobby : Screen("Lobby")
    object Game : Screen("Game")
    object CreateGame : Screen("CreateGame")
}