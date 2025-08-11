// MainActivity.kt
package com.example.offlinesupportapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offlinesupportapp.database.AppDatabase
import com.example.offlinesupportapp.database.entities.UserEntity
import com.example.offlinesupportapp.repository.UserRepository
import com.example.offlinesupportapp.ui.theme.OfflineSupportAppTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // Database ve Repository oluşturma
        val database = AppDatabase.getDatabase(this)
        val repository = UserRepository(database.userDao(), database.cacheDao())

        setContent {
            OfflineSupportAppTheme {
                OfflineSupportApp(repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSupportApp(repository: UserRepository) {
    val scope = rememberCoroutineScope()
    val users by repository.getAllUsers().collectAsState(initial = emptyList())
    val onlineUsers by repository.getOnlineUsers().collectAsState(initial = emptyList())
    val cachedUsers by repository.getCachedUsers().collectAsState(initial = emptyList())

    var isLoading by remember { mutableStateOf(false) }
    var lastSyncTime by remember { mutableStateOf<Long?>(null) }
    var syncStatus by remember { mutableStateOf("No sync yet") }

    // İlk açılışta son sync zamanını kontrol et
    LaunchedEffect(Unit) {
        lastSyncTime = repository.getLastSyncTime()
        updateSyncStatus(lastSyncTime) { syncStatus = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Support App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
            // Offline Data Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Offline Data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Sync Status: $syncStatus")

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    repository.refreshUsers()
                                    lastSyncTime = repository.getLastSyncTime()
                                    updateSyncStatus(lastSyncTime) { syncStatus = it }
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔄 Sync Now")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.clearCache()
                                    syncStatus = "Cache cleared"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🗑️ Clear Cache")
                        }
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Lists
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cached Users Section
                if (cachedUsers.isNotEmpty()) {
                    item {
                        UserSection(
                            title = "📦 Cached Users (${cachedUsers.size})",
                            users = cachedUsers,
                            statusColor = Color(0xFF4CAF50)
                        )
                    }
                }

                // Online Users Section
                if (onlineUsers.isNotEmpty()) {
                    item {
                        UserSection(
                            title = "🟢 Online Users (${onlineUsers.size})",
                            users = onlineUsers,
                            statusColor = Color(0xFF2196F3)
                        )
                    }
                }

                // Offline Users
                val offlineUsers = users.filter { !it.isOnline }
                if (offlineUsers.isNotEmpty()) {
                    item {
                        UserSection(
                            title = "⚫ Offline Users (${offlineUsers.size})",
                            users = offlineUsers,
                            statusColor = Color(0xFF757575)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserSection(
    title: String,
    users: List<UserEntity>,
    statusColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            users.forEach { user ->
                UserItem(user = user)
                if (user != users.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun UserItem(user: UserEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "👤 ${user.name}",
                fontWeight = FontWeight.Medium
            )
            Text(
                text = user.email,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Row {
            if (user.isCached) {
                Text(
                    text = "[📦 Cached]",
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = if (user.isOnline) "[🟢 Online]" else "[⚫ Offline]",
                fontSize = 11.sp,
                color = if (user.isOnline) Color(0xFF2196F3) else Color(0xFF757575)
            )
        }
    }
}
  //fonksiyonu son sync zamanını okunabilir zamana çevirir ve ekranda gösterir.
private fun updateSyncStatus(lastSyncTime: Long?, onUpdate: (String) -> Unit) {
    if (lastSyncTime == null) {
        onUpdate("No sync yet")
        return
    }

    val currentTime = System.currentTimeMillis()
    val diffInMinutes = (currentTime - lastSyncTime) / (1000 * 60)

    val status = when {
        diffInMinutes < 1 -> "Last sync: Just now"
        diffInMinutes < 60 -> "Last sync: ${diffInMinutes}m ago"
        else -> {
            val hours = diffInMinutes / 60
            "Last sync: ${hours}h ago"
        }
    }

    onUpdate(status)
}