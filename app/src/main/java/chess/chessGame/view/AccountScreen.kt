package chess.chessGame.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import chess.chessGame.viewModel.AuthViewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    navController: NavHostController,
    vm: AuthViewModel
) {
    val scope = rememberCoroutineScope()
    val currentUsername by vm.currentDisplayUsername.collectAsState()
    val userRole by vm.userRole.collectAsState()
    val updateStatus by vm.updateStatus.collectAsState()
    val adminUsersList by vm.adminUsersList.collectAsState()
    val profilePhotoUrl by vm.currentPhotoUrl.collectAsState()

    var newUsernameInput by rememberSaveable { mutableStateOf(currentUsername ?: "") }
    var newPasswordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { scope.launch { vm.uploadProfilePicture(it) } }
    }

    LaunchedEffect(Unit) {
        vm.clearUpdateStatus()
        if (userRole == "admin") vm.fetchAdminUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Account Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        updateStatus?.let {
            Text(
                it,
                color = if (it.contains("Error") || it.contains("failed"))
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        if (userRole != "guest") {
            Text("Profile Picture", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(profilePhotoUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Upload Picture")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            UsernameChangeSection(
                currentUsername = currentUsername ?: "",
                newUsernameInput = newUsernameInput,
                onNewUsernameChange = { newUsernameInput = it },
                onUpdateUsername = { scope.launch { vm.updateUsername(newUsernameInput) } }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            PasswordChangeSection(
                newPasswordInput = newPasswordInput,
                onNewPasswordChange = { newPasswordInput = it },
                confirmPasswordInput = confirmPasswordInput,
                onConfirmPasswordChange = { confirmPasswordInput = it },
                showPassword = showPassword,
                onTogglePasswordVisibility = { showPassword = !showPassword },
                onUpdatePassword = { scope.launch { vm.updatePassword(newPasswordInput) } },
                isPasswordValid = newPasswordInput.length >= 6,
                isPasswordMatch = newPasswordInput == confirmPasswordInput
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
        }

        if (userRole == "admin") {
            AdminUserManagement(
                adminUsersList = adminUsersList.map { it.email },
                onDeleteUser = { scope.launch { vm.deleteUser(it) } },
                onChangeUserPassword = { email, pass ->
                    scope.launch { vm.changeUserPassword(email, pass) }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun AdminUserManagement(
    adminUsersList: List<String>,
    onDeleteUser: (String) -> Unit,
    onChangeUserPassword: (String, String) -> Unit
) {
    Text("Admin User Management", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(16.dp))

    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
        items(adminUsersList) { email ->
            AdminUserItem(
                email = email,
                onDeleteUser = onDeleteUser,
                onChangeUserPassword = onChangeUserPassword
            )
        }
    }
}

@Composable
fun AdminUserItem(
    email: String,
    onDeleteUser: (String) -> Unit,
    onChangeUserPassword: (String, String) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(email, modifier = Modifier.weight(1f))

        Button(onClick = { showDialog = true }) {
            Text("Change Pass")
        }

        IconButton(onClick = { onDeleteUser(email) }) {
            Icon(Icons.Default.Delete, contentDescription = null)
        }
    }

    if (showDialog) {
        ChangePasswordDialog(
            email = email,
            onDismiss = { showDialog = false },
            onConfirm = {
                onChangeUserPassword(email, it)
                showDialog = false
            }
        )
    }
}

@Composable
fun ChangePasswordDialog(
    email: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(email) },
        text = {
            Column {
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { show = !show }) {
                            Icon(
                                if (show) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                null
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pass) },
                enabled = pass.length >= 6 && pass == confirm
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UsernameChangeSection(
    currentUsername: String,
    newUsernameInput: String,
    onNewUsernameChange: (String) -> Unit,
    onUpdateUsername: () -> Unit
) {
    OutlinedTextField(
        value = newUsernameInput,
        onValueChange = onNewUsernameChange,
        label = { Text("New Username") },
        placeholder = { Text(currentUsername) },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onUpdateUsername,
        enabled = newUsernameInput.isNotBlank() && newUsernameInput != currentUsername,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Update Username")
    }
}

@Composable
fun PasswordChangeSection(
    newPasswordInput: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPasswordInput: String,
    onConfirmPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onUpdatePassword: () -> Unit,
    isPasswordValid: Boolean,
    isPasswordMatch: Boolean
) {
    OutlinedTextField(
        value = newPasswordInput,
        onValueChange = onNewPasswordChange,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    null
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = confirmPasswordInput,
        onValueChange = onConfirmPasswordChange,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onUpdatePassword,
        enabled = isPasswordValid && isPasswordMatch,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Update Password")
    }
}
