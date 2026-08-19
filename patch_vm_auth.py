import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """    fun saveAndConnectBooklore(
        name: String,
        hostUrl: String,
        username: String,
        password: String
    ) {
        viewModelScope.launch {
            _serverOpState.value = ServerOperationState.Loading
            try {
                // Fetch JWT from Booklore login endpoint
                val loginResult = com.example.data.network.BookloreClient.login(hostUrl, username, password)
                if (loginResult.isFailure) {
                    _serverOpState.value = ServerOperationState.Error("Login failed: ${loginResult.exceptionOrNull()?.message}")
                    return@launch
                }
                val token = loginResult.getOrNull() ?: ""
                
                val serverId = "booklore_${System.currentTimeMillis()}"
                
                val server = ServerConfig(
                    id = serverId,
                    type = "booklore",
                    name = name,
                    hostUrl = hostUrl,
                    apiKey = token, // Store the JWT token
                    username = username,
                    password = password,
                    isConnected = true,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                // Try initial sync
                val syncResult = repository.syncBooklore(server)"""

text = re.sub(r"    fun saveAndConnectBooklore\([\s\S]*?val syncResult = repository\.syncBooklore\(server\)", replacement, text)

with open(file_path, "w") as f:
    f.write(text)
