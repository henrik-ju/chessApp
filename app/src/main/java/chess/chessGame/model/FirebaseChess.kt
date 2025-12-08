package chess.chessGame.model

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

class FirebaseChess {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("games")

    fun createGame(game: GameLobby) {
        database.child(game.id).setValue(game)
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

    fun listenToGame(gameId: String, onUpdate: (GameLobby) -> Unit) {
        database.child(gameId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = snapshot.getValue(GameLobby::class.java)
                if (game != null) onUpdate(game)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenToAllGames(onUpdate: (GameLobby) -> Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                for(child in snapshot.children){
                    val game = child.getValue(GameLobby::class.java)
                    if(game != null)
                        onUpdate(game)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun joinGame(gameId: String, username: String): GameLobby? {
        val gameRef = database.child(gameId)

        return suspendCancellableCoroutine { continuation ->
            gameRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val game = mutableData.getValue(GameLobby::class.java)

                    if (game == null) return Transaction.abort()

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
}