package chess.chessGame.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chess.chessGame.model.*
import kotlinx.coroutines.launch
import chess.chessGame.viewModel.ChessViewModel
import coil.compose.rememberAsyncImagePainter

@Composable
fun GameScreen(
    viewModel: ChessViewModel
) {
    val game by viewModel.game
    val selected by viewModel.selected
    val moveError by viewModel.moveError
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "You are: ${if (viewModel.userTeam == Piece.Team.WHITE) "White" else "Black"}",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Turn: ${if (game.currentTeam == Piece.Team.WHITE) "White" else "Black"}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (game.currentTeam == viewModel.userTeam) Color.Green else Color.Red
        )
        Spacer(Modifier.height(16.dp))
        moveError?.let { error ->
            Text(
                text = "⚠️ $error",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(4.dp)
                    .clickable { viewModel.clearError() }
            )
            Spacer(Modifier.height(8.dp))
        }

        OpponentInformation(
            username = viewModel.opponentUsername.collectAsState().value,
            photoUrl = viewModel.opponentPhotoUrl.collectAsState().value
        )

        Spacer(Modifier.height(32.dp))
        Chessboard(
            game = game,
            selected = selected,
            userTeam = viewModel.userTeam,
            onSquareClick = viewModel::onSquareClicked
        )
        Spacer(Modifier.height(24.dp))
        ChatBox(viewModel = viewModel)
        Spacer(Modifier.height(16.dp))

        viewModel.promotionPosition.value?.let { pos ->
            PromotionDialog(
                team = game.currentTeam.otherTeam(),
                onSelect = { newPiece ->
                    viewModel.promotePawn(newPiece)
                },
                onDismiss = {
                    viewModel.promotePawn(Queen(game.currentTeam.otherTeam(), pos))
                }
            )
        }
    }
}

@Composable
fun Chessboard(
    game: ChessGame,
    selected: Position?,
    userTeam: Piece.Team,
    onSquareClick: (Position) -> Unit
) {
    val lightSquare = Color(0xFFEEEDDD)
    val darkSquare = Color(0xFFFFB500)
    val selectedColor = Color.Green

    Column(
        modifier = Modifier
            .size(380.dp)
            .border(4.dp, Color.Black)
            .background(Color(0xFF121212))
            .fillMaxWidth()
    ) {

        val rowRange = if (userTeam == Piece.Team.WHITE) 0..7 else 7 downTo 0
        val columnRange = if (userTeam == Piece.Team.WHITE) 0..7 else 7 downTo 0

        for (row in rowRange) {
            Row {
                for (col in columnRange) {
                    val pos = Position(row, col)
                    val piece = game.getPieceAt(pos)
                    val isLight = (row + col) % 2 == 0
                    val isSelected = selected == pos

                    val squareColor = when {
                        isSelected -> selectedColor
                        isLight -> lightSquare
                        else -> darkSquare
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(squareColor)
                            .clickable { onSquareClick(pos) },
                        contentAlignment = Alignment.Center
                    ) {
                        piece?.let {
                            Image(
                                painter = painterResource(id = PieceDrawables.getDrawable(it)),
                                contentDescription = it::class.simpleName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ChatBox(viewModel: ChessViewModel) {
    val messages by viewModel.decryptedChatMessages.collectAsState(initial = emptyList())
    var messageInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 75.dp, max = 300.dp)
            .padding(top = 8.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .fillMaxWidth(),
            reverseLayout = true
        ) {
            items(messages.reversed()) { (sender, text) ->
                val isMe = sender == "You"
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color.Blue else Color.Red
                            )
                        ) {
                            append(sender)
                            append(": ")
                        }
                        append(text)
                    },
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        scope.launch {
                            viewModel.sendChatMessage(messageInput)
                            messageInput = ""
                        }
                    }
                },
                enabled = messageInput.isNotBlank(),
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun OpponentInformation(
    username: String?,
    photoUrl: String?
) {
    if (username == null && photoUrl == null)
        return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        if (photoUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(photoUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (username != null) {
            Text(
                text = username,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
