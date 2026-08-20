import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

# Fix categorizeBooksWithAI
text = text.replace("val books = _allEBooks.value", "val books = allEBooks.value")
text = text.replace("val response = geminiApiService.generateContent(request)", "val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)")
text = text.replace("libraryDao.insertEBooks(updatedBooks)", "database.libraryDao().insertEBooks(updatedBooks)")

# Fix fetchGeminiCategoryItems
text = text.replace("val b = _allBooks.value.", "val b = allBooks.value.")
text = text.replace("val e = _allEBooks.value.", "val e = allEBooks.value.")
text = text.replace("val m = _allMusic.value.", "val m = allMusic.value.")

with open(file_path, "w") as f:
    f.write(text)
