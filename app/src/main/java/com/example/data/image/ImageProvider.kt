package com.example.data.image

enum class ImageProvider(
    val id: String,
    val title: String,
    val description: String
) {
    POLLINATIONS("pollinations", "Pollinations AI", "100% Gratis, Cepat, Tanpa Kuota Gemini"),
    GEMINI_IMAGEN("gemini", "Gemini Imagen 3", "Kualitas Tinggi, Menggunakan API Key Gemini"),
    HUGGING_FACE("huggingface", "Hugging Face (FLUX.1)", "Model FLUX.1 / SDXL dengan Token HF")
}
