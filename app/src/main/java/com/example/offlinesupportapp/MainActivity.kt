package com.example.offlinesupportapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlinesupportapp.database.AppDatabase
import com.example.offlinesupportapp.database.entities.UserEntity
import com.example.offlinesupportapp.repository.UserRepository
import com.example.offlinesupportapp.ui.theme.OfflineSupportAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = UserRepository(database.userDao(), database.cacheDao())

        setContent {
            OfflineSupportAppTheme {
                UserListApp(repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListApp(repository: UserRepository) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // İlk yüklemede verileri al
    LaunchedEffect(Unit) {
        loadUsers(repository) { userList, loading, error, message ->
            users = userList
            isLoading = loading
            hasError = error
            errorMessage = message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User List App - API Integration",
                        fontSize = 16.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 Users",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "[${users.size}]",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⏳ Loading users...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Buttons
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    loadUsers(repository) { userList, loading, error, message ->
                                        users = userList
                                        isLoading = loading
                                        hasError = error
                                        errorMessage = message
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔄 Refresh")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    // Clear cache action
                                    repository.clearCache()
                                    // Show cleared message
                                    users = emptyList()
                                    hasError = false
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚙️ Settings")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Section
            if (hasError) {
                // Error State
                ErrorSection(
                    message = errorMessage,
                    onRetry = {
                        scope.launch {
                            loadUsers(repository) { userList, loading, error, message ->
                                users = userList
                                isLoading = loading
                                hasError = error
                                errorMessage = message
                            }
                        }
                    }
                )
            } else {
                // User List
                if (users.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(users) { user ->
                            UserListItem(user)
                        }
                    }
                } else if (!isLoading) {
                    // Empty State
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📭 No users found",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: UserEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Icon
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        Color(0xFF2196F3),
                        RoundedCornerShape(4.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "👤 ${user.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "📧 ${user.email}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ErrorSection(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error Handling:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚫 Network Error",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "❌",
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "İnternet bağlantısı yok",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text(
                            text = "[RETRY]",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadUsers(
    repository: UserRepository,
    onResult: (List<UserEntity>, Boolean, Boolean, String) -> Unit
) {
    onResult(emptyList(), true, false, "")

    try {
        // Simulate delay like real network
        kotlinx.coroutines.delay(1500)

        val result = repository.refreshUsers()
        if (result.isSuccess) {
            // Get fresh data from repository
            repository.getAllUsers().collect { userList ->
                onResult(userList, false, false, "")
                return@collect
            }
        } else {
            // Simulate network error sometimes
            if (kotlin.random.Random.nextBoolean()) {
                onResult(emptyList(), false, true, "İnternet bağlantısı yok")
            } else {
                onResult(emptyList(), false, false, "")
            }
        }
    } catch (e: Exception) {
        onResult(emptyList(), false, true, "Network error occurred")
    }
}