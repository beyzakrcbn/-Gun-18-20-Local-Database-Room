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
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

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
    var lastSyncTime by remember { mutableStateOf("") }
    var cachedCount by remember { mutableStateOf(0) }
    var onlineCount by remember { mutableStateOf(0) }

    // İlk yüklemede verileri al
    LaunchedEffect(Unit) {
        loadUsers(repository) { userList, loading, error, message ->
            users = userList
            isLoading = loading
            hasError = error
            errorMessage = message
        }
        updateSyncInfo(repository) { syncTime, cached, online ->
            lastSyncTime = syncTime
            cachedCount = cached
            onlineCount = online
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Offline Support App",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline Data Status Card
            StatusCard(
                lastSyncTime = lastSyncTime,
                cachedCount = cachedCount,
                onlineCount = onlineCount,
                isLoading = isLoading
            )

            // Action Buttons
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
                            updateSyncInfo(repository) { syncTime, cached, online ->
                                lastSyncTime = syncTime
                                cachedCount = cached
                                onlineCount = online
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("📁 Sync Now")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.clearCache()
                            users = emptyList()
                            hasError = false
                            cachedCount = 0
                            onlineCount = 0
                            lastSyncTime = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📁 Clear Cache")
                }
            }

            // Content Section
            if (hasError) {
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
                // User Lists
                if (users.isNotEmpty()) {
                    val cachedUsers = users.filter { it.id <= 5 } // İlk 5 cached olsun
                    val onlineUsers = users.filter { it.id > 5 }  // Geri kalanlar online

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (cachedUsers.isNotEmpty()) {
                            item {
                                SectionHeader("📁 Cached Users (${cachedUsers.size})")
                            }
                            items(cachedUsers) { user ->
                                UserListItem(user, isFromCache = true)
                            }
                        }

                        if (onlineUsers.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionHeader("📡 Online Users (${onlineUsers.size})")
                            }
                            items(onlineUsers) { user ->
                                UserListItem(user, isFromCache = false)
                            }
                        }
                    }
                } else if (!isLoading) {
                    EmptyState()
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    lastSyncTime: String,
    cachedCount: Int,
    onlineCount: Int,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📁 Offline Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )

            HorizontalDivider(color = Color(0xFFE0E0E0))

            InfoRow(
                icon = "📡",
                label = "Sync Status:",
                value = if (lastSyncTime.isNotEmpty()) "Last sync $lastSyncTime" else "Never synced"
            )

            InfoRow(
                icon = "📁",
                label = "Cached Users",
                value = "($cachedCount)"
            )

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = "Syncing...",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1976D2),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun UserListItem(user: UserEntity, isFromCache: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFromCache) Color(0xFFF3E5F5) else Color(0xFFE8F5E8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isFromCache) Color(0xFF9C27B0) else Color(0xFF4CAF50),
                        RoundedCornerShape(4.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
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

            Text(
                text = if (isFromCache) "📁 Cached" else "📡 Online",
                fontSize = 11.sp,
                color = if (isFromCache) Color(0xFF9C27B0) else Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📭",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No users found",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Text(
                text = "Tap 'Sync Now' to load data",
                fontSize = 14.sp,
                color = Color.Gray
            )
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
                        text = message,
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
    // Loading state'ini başlat
    onResult(emptyList(), true, false, "")

    try {
        // Önce mevcut cache'deki verileri al
        val cachedUsers = repository.getAllUsers().first()
        if (cachedUsers.isNotEmpty()) {
            // Cache'de veri varsa göster
            onResult(cachedUsers, true, false, "")
        }

        // Network delay simülasyonu
        kotlinx.coroutines.delay(1500)

        // API'den fresh data çekmeyi dene
        val refreshResult = repository.refreshUsers()

        if (refreshResult.isSuccess) {
            // API başarılı - güncel verileri al
            val freshUsers = repository.getAllUsers().first()
            onResult(freshUsers, false, false, "")
        } else {
            // API başarısız - error handling
            val fallbackUsers = repository.getAllUsers().first()
            if (fallbackUsers.isNotEmpty()) {
                // Cache'de veri varsa eski verileri göster
                onResult(fallbackUsers, false, false, "")
            } else {
                // Cache de boş ise error göster
                onResult(emptyList(), false, true, "İnternet bağlantısı yok ve önbellek boş")
            }
        }

    } catch (e: Exception) {
        try {
            // Exception durumunda cache'deki verileri kontrol et
            val fallbackUsers = repository.getAllUsers().first()
            if (fallbackUsers.isNotEmpty()) {
                // Cache'de veri varsa göster
                onResult(fallbackUsers, false, false, "")
            } else {
                // Cache de boş ise error göster
                onResult(emptyList(), false, true, "Bağlantı hatası: ${e.message}")
            }
        } catch (cacheError: Exception) {
            // Cache okuma da başarısız
            onResult(emptyList(), false, true, "Veri yüklenirken hata oluştu")
        }
    }
}

private suspend fun updateSyncInfo(
    repository: UserRepository,
    onResult: (String, Int, Int) -> Unit
) {
    try {
        // Son sync zamanını al (örnek implementation)
        val lastSync = getCurrentTime()

        // Cache'deki kullanıcı sayısını al
        val cachedUsers = repository.getAllUsers().first()
        val cachedCount = cachedUsers.size

        // Online kullanıcı sayısı (örnek olarak cached'den fazla olanlar)
        val onlineCount = cachedUsers.filter { it.id > 5 }.size

        onResult(lastSync, cachedCount, onlineCount)
    } catch (e: Exception) {
        onResult("", 0, 0)
    }
}

private fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}