package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<ContentPayload>,
    @Json(name = "systemInstruction") val systemInstruction: ContentPayload? = null,
    @Json(name = "generationConfig") val generationConfig: GenerationConfigPayload? = null,
    @Json(name = "safetySettings") val safetySettings: List<SafetySettingPayload>? = null
)

@JsonClass(generateAdapter = true)
data class SafetySettingPayload(
    @Json(name = "category") val category: String,
    @Json(name = "threshold") val threshold: String
)

@JsonClass(generateAdapter = true)
data class ContentPayload(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<PartPayload>
)

@JsonClass(generateAdapter = true)
data class PartPayload(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfigPayload(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<CandidatePayload>? = null,
    @Json(name = "promptFeedback") val promptFeedback: PromptFeedbackPayload? = null
)

@JsonClass(generateAdapter = true)
data class CandidatePayload(
    @Json(name = "content") val content: ContentPayload? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class PromptFeedbackPayload(
    @Json(name = "blockReason") val blockReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ListModelsResponse(
    @Json(name = "models") val models: List<GeminiModelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelInfo(
    @Json(name = "name") val name: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null
)

