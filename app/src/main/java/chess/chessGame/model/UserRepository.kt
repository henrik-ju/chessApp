package chess.chessGame.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    companion object {
        private const val DATABASE_URL =
            "https://androidproject-b0580-default-rtdb.europe-west1.firebasedatabase.app"
    }
    private val db = FirebaseDatabase.getInstance(DATABASE_URL).reference.child("users")
    private val auth = FirebaseAuth.getInstance()
    suspend fun createAccount(email: String, password: String, username: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid
            if (userId != null) {
                db.child(userId)
                    .setValue(mapOf("username" to username, "email" to email))
                    .await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    suspend fun fetchUsername(userId: String): String? {
        return try {
            val snapshot = db.child(userId).child("username").get().await()
            snapshot.value as? String
        } catch (e: Exception) {
            null
        }
    }
    suspend fun updateUsername(userId: String, newUsername: String): Boolean {
        return try {
            db.child(userId)
                .updateChildren(mapOf("username" to newUsername))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun updatePassword(newPassword: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.updatePassword(newPassword).await()
            true
        } catch (e: Exception) {
            throw e
        }
    }
    fun logout() {
        auth.signOut()
    }
    suspend fun fetchAllUsersForAdmin(): Map<String, Map<String, String>> {
        return try {
            val snapshot = db.get().await()
            (snapshot.value as? Map<String, Map<String, Any>>)
                ?.mapValues { (_, userData) ->
                    userData.mapValues { (_, value) -> value.toString() }
                } ?: emptyMap()
        } catch (e: Exception) {
            throw e
        }
    }
    suspend fun getUidByEmail(email: String): String? {
        return try {
            val snapshot = db.orderByChild("email").equalTo(email).get().await()
            snapshot.children.firstOrNull()?.key
        } catch (e: Exception) {
            null
        }
    }
    suspend fun deleteUser(uid: String): Boolean {
        return try {
            db.child(uid).removeValue().await()
            true
        } catch (e: Exception) {
            throw e
        }
    }
    suspend fun adminUpdatePassword(uid: String, newPassword: String): Boolean {
        throw UnsupportedOperationException()
    }
    fun getProfilePhotoUrl(userId: String): Flow<String?> {
        val flow = MutableStateFlow<String?>(null)
        val ref = db.child(userId).child("profilePhotoUrl")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                flow.value = snapshot.getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                flow.value = null
            }
        })
        return flow
    }

    suspend fun updateProfilePictureUrl(userId: String, url: String) {
        db.child(userId).child("profilePhotoUrl").setValue(url).await()
    }
}
