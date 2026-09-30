package com.example.config

import com.example.BuildConfig
import com.example.data.api.SafetySettingPayload
import com.example.data.image.ImageProvider
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pengelola API Key Bergilir (Rolling / Round-Robin Multiple API Keys):
 * - Mendukung input banyak API Key sekaligus (dipisahkan baris baru atau koma).
 * - Merotasi kunci secara bergantian di setiap pesan (round-robin) agar kuota/token terbagi rata ke banyak akun.
 * - Mendukung failover otomatis jika satu kunci terkena batas limit (HTTP 429 / RESOURCE_EXHAUSTED).
 */
object ApiKeyManager {
    private val keyRotationIndex = AtomicInteger(0)

    val DEFAULT_BUILTIN_KEYS = listOf(
        "AIzaSyD9Bw7CW-eZeblrDT0Kpx_AmcvxILvypMs",
        "AIzaSyAasK7Oym4rsUbqoOo-6TjdqxcozPFNXGw",
        "AIzaSyAanLBZmS2-rO_o0VcFEUBKXO9u5EMuMAA",
        "AIzaSyBaru6Vlwty6m0Kj1ZadxhcQFHBhTxeoRs",
        "AIzaSyBNhP5f0jnjRgP1WIbfSkREFFvSTMcrU7U",
        "AIzaSyBbKDYPWVilemzH0EDAWwARZx2GsLDnf7c",
        "AIzaSyAeUr7MYgQr3iwKBhtp6RfFmL-aSkMYlTM",
        "AIzaSyAL6beRSui_8_6WBS8tX9adkkfF38FBPzg",
        "AIzaSyDyvRAxWv0jQwP_L0UtpdLdN2_zafla3CU",
        "AIzaSyCi9jf12AO5qd4W_lzr8HYKoPbfjW_Ch9s",
        "AIzaSyDCK1pDdOgkHr2xkjQyj7YE-A2Xoi57AKo",
        "AIzaSyDhtrWD53QpKR4y1IygGVMWPAZETOxflSI",
        "AIzaSyDDXbGsYaANpCz9MEYe62-Dno76FhIV5vQ",
        "AIzaSyAk6DC3VBGeHpkMzF3WZWtgEjcXs95_0SA",
        "AIzaSyCFfH31LMXJVqD2_zpaaZGyfS0ZQK-6szU",
        "AIzaSyAw40rhBCZ2FFW7xfYdmrUKF9jct-Ytk_U",
        "AIzaSyDVGY6AerVr_EpmTYwkLSRSOjf3OFmyTM4",
        "AIzaSyCw_efpIRxS5KRhmBR3t4JGz081JKhAXFE",
        "AIzaSyBPkZc912PPt2LzuMzgVa9QEeUxtmk-Sns",
        "AIzaSyCRg_Q1jCgrbVmVM9Yq9W6NpQ5EL2gDWgE",
        "AIzaSyAjGL59TzyBkUPMEbg62zft91KgIwqHrr4",
        "AIzaSyB7k8v3jX-Kl8TxQXF7jj6nc7h9UurBQ9A",
        "AIzaSyAyUMHgs2taWsvZRC1j_QrdpPbgDwn6bsE"
    )

    fun parseKeys(rawKeysText: String?): List<String> {
        if (rawKeysText.isNullOrBlank()) return emptyList()
        return rawKeysText
            .lines()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.contains("MY_GEMINI_API_KEY") && !it.contains("your_api_key_here") }
            .distinct()
    }

    fun getAllAvailableKeys(rawKeysText: String?): List<String> {
        val parsed = parseKeys(rawKeysText)
        if (parsed.isNotEmpty()) return parsed
        val buildKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        if (buildKey.isNotBlank() && !buildKey.contains("MY_GEMINI_API_KEY") && !buildKey.contains("your_api_key_here")) {
            return listOf(buildKey.trim())
        }
        return DEFAULT_BUILTIN_KEYS
    }

    fun getNextApiKey(rawKeysText: String?): String {
        val keys = getAllAvailableKeys(rawKeysText)
        if (keys.isEmpty()) return ""
        val idx = Math.floorMod(keyRotationIndex.getAndIncrement(), keys.size)
        return keys[idx]
    }
}

/**
 * Konfigurasi Tunggal untuk System Instruction, Parameter Generasi & Model Gemini.
 * Wadah bersih tanpa hardcode karakter tertentu.
 */
object StoryPromptConfig {

    const val DEFAULT_MODEL = "gemini-2.5-flash"

    val AVAILABLE_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview"
    )

    const val DEFAULT_TEMPERATURE = 0.85f
    const val DEFAULT_TOP_P = 0.95f
    const val DEFAULT_TOP_K = 40
    const val DEFAULT_MAX_OUTPUT_TOKENS = 2048
    val DEFAULT_IMAGE_PROVIDER = ImageProvider.POLLINATIONS

    /**
     * Safety setting di-hardcode seluruhnya ke "NONE" (BLOCK_NONE) untuk kebebasan narasi fiksi.
     */
    val HARDCODED_SAFETY_SETTINGS = listOf(
        SafetySettingPayload(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_NONE"),
        SafetySettingPayload(category = "HARM_CATEGORY_HATE_SPEECH", threshold = "BLOCK_NONE"),
        SafetySettingPayload(category = "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold = "BLOCK_NONE"),
        SafetySettingPayload(category = "HARM_CATEGORY_DANGEROUS_CONTENT", threshold = "BLOCK_NONE"),
        SafetySettingPayload(category = "HARM_CATEGORY_CIVIC_INTEGRITY", threshold = "BLOCK_NONE")
    )

    fun getResolvedApiKey(customKey: String?): String {
        return ApiKeyManager.getNextApiKey(customKey)
    }

    /**
     * Default System Instruction untuk percakapan umum.
     */
    const val DEFAULT_CHATBOT_INSTRUCTION = "Kamu adalah narator dan pendamping cerita interaktif yang ramah, ekspresif, dan responsif."

    /**
     * Template Panduan Visual Novel Bersih & Fleksibel untuk Cerita Baru (Tanpa karakter bawaan).
     */
    const val DEFAULT_SYSTEM_INSTRUCTION = """PERANKAN KARAKTER VIRTUAL DALAM PANGGUNG CERITA BERDIALOG:

I. IDENTITAS & KONTEKS DASAR
1. Identitas User: Kamu berinteraksi dengan lawan main dalam cerita (User).
2. Bahasa: Gunakan Bahasa Indonesia yang baik, luwes, dan sesuai kepribadian karakter cerita ini.

II. ATURAN FORMAT & GAYA RESPON
1. Tindakan & Deskripsi Suasana: WAJIB diapit tanda pagar (#...#) atau tanda bintang (*...*) agar dirender menjadi teks miring. Contoh: #Melangkah perlahan mendekati jendela dan menatap ke luar#
2. Dialog: WAJIB menggunakan tanda petik ganda ("..."). Contoh: "Ada apa denganmu malam ini?"
3. Urutan: Dinamis di tiap respon (bisa dialog dulu, tindakan dulu, atau deskripsi suasana). Antara dialog dan tindakan dipisahkan baris baru (newline) agar rapi.
4. Jangan gunakan awalan label/subtitle seperti nama pembicara di setiap baris.
5. Di akhir setiap respon, kamu dapat menyertakan tag visual adegan dalam bahasa Inggris untuk ilustrasi visual novel: [SCENE_ART: Deskripsi visual adegan, karakter, pakaian, dan pencahayaan]

III. PERILAKU INTI KARAKTER
1. Berperilakulah layaknya manusia nyata yang memiliki emosi, respon fisik, dan dinamika kepribadian yang hidup.
2. Anti-Repetisi: Selalu periksa riwayat percakapan. Dilarang mengulang-ulang respon atau pilihan kata yang sama.
3. Jaga Peran: Jangan pernah keluar dari peran dan jangan pernah mengakui bahwa kamu adalah AI.
4. Dilarang Mengontrol Peran User: Kendali peran User adalah mutlak milik User.
5. DILARANG KERAS memberikan kalimat penutup khas asisten AI di akhir respon (seperti "Apakah Anda ingin melanjutkan?"). Respon harus berhenti sepenuhnya pada dialog atau tindakan karakter."""
}
