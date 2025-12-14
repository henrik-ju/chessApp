package chess.chessGame.model

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

object FirebaseChess {
    private const val DATABASE_URL = "https://androidproject-b0580-default-rtdb.europe-west1.firebasedatabase.app"
    private val database: DatabaseReference = FirebaseDatabase.getInstance(DATABASE_URL).reference.child("games")

    fun createGame(game: GameLobby) {
        database.child(game.id).setValue(game)
    }
    fun stopListeningToAllGames(listener: ChildEventListener) {
        database.removeEventListener(listener)
    }
    suspend fun getGame(gameId: String): GameLobby? {
        return try {
            val snapshot = database.child(gameId).get().await()
            snapshot.getValue(GameLobby::class.java)
        } catch (e: Exception) {
            println("Error fetching game $gameId: ${e.message}")
            null
        }
    }
    suspend fun updateGameFenTransaction(gameId: String, newFen: String): Boolean {
        val gameRef = database.child(gameId)

        return suspendCancellableCoroutine { continuation ->
            gameRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    mutableData.child("fen").value = newFen
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (continuation.isCancelled) return
                    if (error != null) {
                        println("FEN transaction error: ${error.message}")
                        continuation.resume(false)
                    } else {
                        continuation.resume(committed)
                    }
                }
            })
        }
    }
    fun listenToGame(gameId: String, onUpdate: (GameLobby) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(GameLobby::class.java)?.let { onUpdate(it)}
            }
            override fun onCancelled(error: DatabaseError) {
                println("Cancelled")
            }
        }
        database.child(gameId).addValueEventListener(listener)
        return listener
    }
    fun listenToAllGames(onUpdate: (List<GameLobby>) -> Unit): ChildEventListener {
        val games = mutableMapOf<String, GameLobby>()

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(GameLobby::class.java)?.let {
                    games[snapshot.key!!] = it
                    onUpdate(games.values.toList())
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(GameLobby::class.java)?.let {
                    games[snapshot.key!!] = it
                    onUpdate(games.values.toList())
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.key?.let {
                    games.remove(it)
                    onUpdate(games.values.toList())
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                println("Lobby listener cancelled: ${error.message}")
            }
        }

        database.addChildEventListener(listener)
        return listener
    }
    suspend fun joinGame(gameId: String, username: String): GameLobby? {
        val gameRef = database.child(gameId)

        return suspendCancellableCoroutine { continuation ->
            gameRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val game = mutableData.getValue(GameLobby::class.java) ?: return Transaction.abort()

                    if (game.whitePlayer == username || game.blackPlayer == username) {
                        return Transaction.success(mutableData)
                    }

                    val updatedGame = when {
                        game.whitePlayer == null -> game.copy(whitePlayer = username)
                        game.blackPlayer == null -> game.copy(blackPlayer = username)
                        else -> return Transaction.abort()
                    }

                    mutableData.value = updatedGame
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (continuation.isCancelled) return

                    if (error != null) {
                        println("Error joining game $gameId: ${error.message}")
                        continuation.resume(null)
                    } else if (committed && snapshot != null) {
                        val game = snapshot.getValue(GameLobby::class.java)
                        continuation.resume(game)
                    } else {
                        continuation.resume(null)
                    }
                }
            })
        }
    }
    fun sendChatMessage(gameId: String, message: ChatMessage) {
        val messagesRef = database.child(gameId).child("messages")
        messagesRef.push().setValue(message)
    }
    fun listenToChat(gameId: String, onUpdate: (List<ChatMessage>) -> Unit): ValueEventListener {
        val messagesRef = database.child(gameId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { childSnapshot ->

                    childSnapshot.getValue(ChatMessage::class.java)
                }.sortedBy { it.timestamp }
                onUpdate(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Chat listener cancelled for game $gameId: ${error.message}")
            }
        }

        messagesRef.addValueEventListener(listener)
        return listener
    }
    suspend fun getOrCreateGameKey(gameId: String, newKey: String): String {
        val keyRef = database.child(gameId).child("chatKey")

        return suspendCancellableCoroutine { continuation ->
            keyRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val existingKey = mutableData.value as? String

                    if (existingKey != null) {
                        return Transaction.success(mutableData)
                    }
                    mutableData.value = newKey
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (continuation.isCancelled) return

                    if (error != null) {
                        println("Error during key transaction: ${error.message}")
                        continuation.resumeWith(Result.failure(error.toException()))
                    } else if (snapshot != null) {
                        val finalKey = snapshot.value as? String

                        if (finalKey != null) {
                            continuation.resume(finalKey)
                        } else {
                            continuation.resumeWith(Result.failure(Exception("Key was unexpectedly null after transaction.")))
                        }
                    } else {
                        continuation.resumeWith(Result.failure(Exception("Key transaction failed unexpectedly.")))
                    }
                }
            })
        }
    }

    suspend fun getOpponentInfo(gameId: String, currentUsername: String): Pair<String?, String?> {
        val game = getGame(gameId) ?: return null to null
        val opponentUsername = when (currentUsername) {
            game.whitePlayer -> game.blackPlayer
            game.blackPlayer -> game.whitePlayer
            else -> null
        }
        val opponentPhotoUrl = opponentUsername?.let { getProfilePhotoUrl(it) }
        return opponentUsername to opponentPhotoUrl
    }

    suspend fun getProfilePhotoUrl(uid: String): String? {
        val userRef = FirebaseDatabase.getInstance(DATABASE_URL).reference.child("users").child(uid)
        return try {
            val snapshot = userRef.child("photoUrl").get().await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            println("Error fetching photo URL for $uid: ${e.message}")
            null
        }
    }
}