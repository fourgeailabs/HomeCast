import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """object BookloreClient {
    private const val TAG = "BookloreClient"
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun login"""

text = text.replace("object BookloreClient {\n    suspend fun login", replacement)

with open(file_path, "w") as f:
    f.write(text)
