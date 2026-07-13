package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.netauth.NetAuthManager
import com.example.netauth.UdpDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGuestLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var serverUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = "Поиск сервера NetAuth..."
        val url = UdpDiscovery.discoverServer(context)
        if (url != null) {
            serverUrl = url
            NetAuthManager.setServerUrl(url)
            errorMessage = null
        } else {
            errorMessage = "Сервер NetAuth не найден в локальной сети. Играйте в режиме гостя."
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "NetAuth", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (serverUrl == null && !isLoading) {
            var manualIp by remember { mutableStateOf("") }
            
            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                label = { Text("IP-адрес сервера (например, 192.168.1.100)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (manualIp.isNotBlank()) {
                            var ip = manualIp.trim()
                            if (!ip.startsWith("http://") && !ip.startsWith("https://")) {
                                if (!ip.contains(":")) {
                                    ip = "http://$ip:8080/"
                                } else {
                                    ip = "http://$ip/"
                                }
                            }
                            serverUrl = ip
                            NetAuthManager.setServerUrl(ip)
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Подключиться")
                }
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = "Поиск сервера NetAuth..."
                            val url = UdpDiscovery.discoverServer(context)
                            if (url != null) {
                                serverUrl = url
                                NetAuthManager.setServerUrl(url)
                                errorMessage = null
                            } else {
                                errorMessage = "Сервер NetAuth не найден в локальной сети. Играйте в режиме гостя."
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Повторить поиск")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (serverUrl != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Подключено к серверу:", style = MaterialTheme.typography.labelSmall)
                        Text(serverUrl ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(
                        onClick = {
                            serverUrl = null
                            errorMessage = null
                        }
                    ) {
                        Text("Сменить")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Заполните все поля"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val hash = hashPassword(password)
                        val result = NetAuthManager.login(email, hash)
                        if (result.isSuccess) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "Неверный логин или пароль"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Войти через NetAuth")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет аккаунта? Зарегистрируйтесь в приложении NetAuth",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = {
                NetAuthManager.loginAsGuest()
                onGuestLogin()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Играть как Гость")
        }
    }
}

fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
