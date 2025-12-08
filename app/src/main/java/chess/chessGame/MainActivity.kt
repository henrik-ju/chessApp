package chess.chessGame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import chess.chessGame.navigation.ChessNavHost
import chess.chessGame.ui.theme.AndroidProjectTheme
import com.google.firebase.FirebaseApp
import viewModel.AuthViewModel
import viewModel.GameLobbyViewModel
import viewModel.GameLobbyViewModelFactory

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)

        setContent {
            AndroidProjectTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 50.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val currentUserEmail by authViewModel.currentUserEmail.collectAsState(initial = null)

                    val userIdForLobby = currentUserEmail ?: "Guest"

                    val gameLobbyViewModel: GameLobbyViewModel = viewModel(
                        factory = GameLobbyViewModelFactory(userIdForLobby)
                    )

                    ChessNavHost(
                        navController = navController,
                        authVm = authViewModel,
                        gameLobbyVm = gameLobbyViewModel
                    )
                }
            }
        }
    }
}