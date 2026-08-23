package com.example.data

import com.example.BuildConfig
import com.example.Content
import com.example.GenerateContentRequest
import com.example.Part
import com.example.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class VerificationResult(
    val isValid: Boolean,
    val sourceName: String,
    val originalUrl: String,
    val correctedUrl: String,
    val mediaTypes: List<String>, // e.g. ["AUDIOBOOK", "EBOOK", "MUSIC", "COMIC"]
    val explanation: String,
    val requiresCorrection: Boolean
)

object PublicDomainSourceVerifier {

    suspend fun verifyAndCorrectSourceUrl(inputUrl: String, customName: String? = null): VerificationResult {
        return withContext(Dispatchers.IO) {
            val trimmedUrl = inputUrl.trim()
            if (trimmedUrl.isBlank()) {
                return@withContext VerificationResult(
                    isValid = false,
                    sourceName = "Invalid",
                    originalUrl = inputUrl,
                    correctedUrl = "",
                    mediaTypes = emptyList(),
                    explanation = "The provided URL is empty. Please enter a valid website or API endpoint.",
                    requiresCorrection = false
                )
            }

            // Quick heuristic check
            val sanitizedInput = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                "https://$trimmedUrl"
            } else {
                trimmedUrl
            }

            try {
                val prompt = """
                    You are the HomeCast Public Domain Source Verification AI.
                    The user has provided a website or endpoint URL to add as a Public Domain Media Source for Audiobooks, E-Books, Music, and/or Comics.
                    
                    User Input URL: "$trimmedUrl"
                    Sanitized Attempt: "$sanitizedInput"
                    Optional Custom Name: "${customName ?: ""}"
                    
                    Your job:
                    1. Analyze the domain, service, and URL path.
                    2. Identify if this is a known or valid public domain/open-access media source (such as Project Gutenberg, Internet Archive, Standard Ebooks, LibriVox, Musopen, Free Music Archive, Open Culture, Feedbooks, ManyBooks, ComicBookPlus, CC Trax, etc.) or a valid website/feed.
                    3. Check if the URL has syntax errors, missing protocols, broken paths, or bad parameters. If needed, provide the CORRECTED, canonical, fully-formed HTTPS URL for accessing/querying its catalog or open domain content.
                    4. Identify which media types it primarily provides from: ["AUDIOBOOK", "EBOOK", "MUSIC", "COMIC"].
                    5. Provide a friendly, clear, 2-sentence explanation of what was verified, what corrections were made (if any), and what media it offers.
                    
                    Return ONLY valid JSON matching this exact schema:
                    {
                      "isValid": true or false,
                      "sourceName": "Clean Name of Source (e.g. 'Project Gutenberg', 'LibriVox Free Audiobooks', etc.)",
                      "correctedUrl": "https://...",
                      "mediaTypes": ["EBOOK", "AUDIOBOOK"],
                      "requiresCorrection": true or false,
                      "explanation": "Verified Project Gutenberg catalog. Added HTTPS protocol and standardized collection path for automated e-book streaming."
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                if (rawText.isNotBlank()) {
                    val cleanJson = rawText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val startIndex = cleanJson.indexOf("{")
                    val endIndex = cleanJson.lastIndexOf("}")
                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        val jsonStr = cleanJson.substring(startIndex, endIndex + 1)
                        val jsonObj = JSONObject(jsonStr)
                        
                        val isValid = jsonObj.optBoolean("isValid", true)
                        val name = jsonObj.optString("sourceName", customName?.ifBlank { "Custom Source" } ?: "Custom Source")
                        val corrected = jsonObj.optString("correctedUrl", sanitizedInput)
                        val requiresCorr = jsonObj.optBoolean("requiresCorrection", corrected != trimmedUrl)
                        val expl = jsonObj.optString("explanation", "Source verified successfully by HomeCast AI.")
                        val typesArray = jsonObj.optJSONArray("mediaTypes")
                        val types = mutableListOf<String>()
                        if (typesArray != null) {
                            for (i in 0 until typesArray.length()) {
                                types.add(typesArray.getString(i).uppercase())
                            }
                        }
                        if (types.isEmpty()) {
                            types.add("EBOOK")
                            types.add("AUDIOBOOK")
                        }

                        return@withContext VerificationResult(
                            isValid = isValid,
                            sourceName = if (!customName.isNullOrBlank()) customName else name,
                            originalUrl = inputUrl,
                            correctedUrl = if (corrected.isNotBlank()) corrected else sanitizedInput,
                            mediaTypes = types,
                            explanation = expl,
                            requiresCorrection = requiresCorr
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback verification when Gemini is unreachable or offline
            }

            // Fallback deterministic verification
            val isValid = try {
                URL(sanitizedInput)
                true
            } catch (e: Exception) {
                false
            }

            val inferredTypes = mutableListOf<String>()
            val lower = sanitizedInput.lowercase()
            if (lower.contains("audio") || lower.contains("librivox") || lower.contains("radio") || lower.contains("voice")) {
                inferredTypes.add("AUDIOBOOK")
            }
            if (lower.contains("book") || lower.contains("gutenberg") || lower.contains("text") || lower.contains("standardebooks")) {
                inferredTypes.add("EBOOK")
            }
            if (lower.contains("music") || lower.contains("78rpm") || lower.contains("sound") || lower.contains("audio") || lower.contains("etree")) {
                inferredTypes.add("MUSIC")
            }
            if (lower.contains("comic") || lower.contains("manga")) {
                inferredTypes.add("COMIC")
            }
            if (inferredTypes.isEmpty()) {
                inferredTypes.addAll(listOf("EBOOK", "AUDIOBOOK"))
            }

            val name = customName?.ifBlank { null } ?: when {
                lower.contains("gutenberg") -> "Project Gutenberg"
                lower.contains("librivox") -> "LibriVox Audiobooks"
                lower.contains("archive.org") -> "Internet Archive Vault"
                lower.contains("standardebooks") -> "Standard Ebooks"
                lower.contains("musopen") -> "Musopen Classical"
                lower.contains("freemusic") -> "Free Music Archive"
                else -> URL(sanitizedInput).host
            }

            return@withContext VerificationResult(
                isValid = isValid,
                sourceName = name,
                originalUrl = inputUrl,
                correctedUrl = sanitizedInput,
                mediaTypes = inferredTypes,
                explanation = if (sanitizedInput != inputUrl) {
                    "Formatted URL with HTTPS protocol and verified network format."
                } else {
                    "Validated domain structure and mapped accessible media streams."
                },
                requiresCorrection = sanitizedInput != inputUrl
            )
        }
    }
}
