package chess.chessGame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import chess.chessGame.model.FirebaseChess
import chess.chessGame.navigation.ChessNavHost
import chess.chessGame.ui.theme.AndroidProjectTheme
import com.google.firebase.FirebaseApp
import chess.chessGame.viewModel.AuthViewModel
import chess.chessGame.viewModel.GameLobbyViewModel
import chess.chessGame.viewModel.GameLobbyViewModelFactory

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val firebaseChessService = FirebaseChess

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        splashScreen.setKeepOnScreenCondition {
            !authViewModel.ready.value
        }


        setContent {
            val isReady by authViewModel.ready.collectAsState()
            AndroidProjectTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 50.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUserUid by authViewModel.currentUserUid.collectAsState(initial = null)

                    val navigateToGame: (gameId: String, assignedColor: String) -> Unit =
                        { gameId, assignedColor ->
                            navController.navigate("GameScreen/$gameId/$assignedColor")
                        }

                    val factory = GameLobbyViewModelFactory(
                        currentUser = currentUserUid ?: "initializing",
                        firebaseService = firebaseChessService,
                        onNavigateToGame = navigateToGame
                    )

                    val gameLobbyViewModel: GameLobbyViewModel = viewModel(factory = factory)


                    LaunchedEffect(currentUserUid) {
                        val uid = currentUserUid
                        if (!uid.isNullOrEmpty() && uid != "Guest") {
                            gameLobbyViewModel.onUserLoggedIn(uid)
                        } else {
                            gameLobbyViewModel.stopListening()
                        }
                    }


                    ChessNavHost(
                        navController = navController,
                        authVm = authViewModel,
                        gameLobbyVm = gameLobbyViewModel,
                        isReady = isReady
                    )
                }
            }
        }
    }
}