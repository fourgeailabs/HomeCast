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
                // For Booklore OPDS/personal server, save config and sync immediately
                val serverId = "booklore_${System.currentTimeMillis()}"
                
                // Let's assume for Booklore OPDS, token/apiKey is derived or just basic auth username:password.
                // We'll store basic auth in the apiKey field if that's what BookloreClient uses.
                // Actually BookloreClient uses Bearer token, but let's pass a basic auth token or simply fetch it directly.
                val authString = okhttp3.Credentials.basic(username, password)
                
                val server = ServerConfig(
                    id = serverId,
                    type = "booklore",
                    name = name,
                    hostUrl = hostUrl,
                    apiKey = authString, // BookloreClient will send this
                    username = username,
                    password = password,
                    isConnected = true,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                // Try initial sync
                val syncResult = repository.syncBooklore(server)
                if (syncResult.isSuccess) {
                    repository.addOrUpdateServer(server)
                    _serverOpState.value = ServerOperationState.Success(
                        "Connected to Booklore server '${server.name}' successfully! Synced ${syncResult.getOrNull()} books."
                    )
                } else {
                    _serverOpState.value = ServerOperationState.Error("Failed to connect or sync: ${syncResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _serverOpState.value = ServerOperationState.Error("Error: ${e.message}")
            }
        }
    }"""

text = re.sub(r"    fun saveAndConnectBooklore\([\s\S]*?ServerOperationState\.Error\(\"Error: \$\{e\.message\}\"\)\n            \}\n        \}\n    \}", replacement, text)

with open(file_path, "w") as f:
    f.write(text)
