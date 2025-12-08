package chess.chessGame.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository{

    private val db = FirebaseDatabase.getInstance().reference.child("users")
    private val auth = FirebaseAuth.getInstance()

    suspend fun createAccount(email: String, password: String, username: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid

            if (userId != null) {
                db.child(userId).setValue(mapOf("username" to username)).await()
            }
            true
        } catch (e: Exception) {
            println("Firebase account creation failed: ${e.message}")
            false
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            println("Firebase login failed: ${e.message}")
            false
        }
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun fetchUsername(userId: String): String? {
        return try {
            val snapshot = db.child(userId).child("username").get().await()
            snapshot.value as? String
        } catch (e: Exception) {
            println("Error fetching username: ${e.message}")
            null
        }
    }

    fun logout() {
        auth.signOut()
    }
}