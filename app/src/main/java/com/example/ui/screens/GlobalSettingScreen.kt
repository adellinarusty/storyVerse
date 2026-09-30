package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.ApiKeyManager
import com.example.config.StoryPromptConfig
import com.example.data.image.ImageProvider
import com.example.viewmodel.StoryUiState
import com.example.viewmodel.StoryViewModel
import java.util.Locale

@Composable
fun GlobalSettingScreen(
    viewModel: StoryViewModel,
    uiState: StoryUiState,
    modifier: Modifier = Modifier
) {
    var apiKeyText by remember(uiState.customApiKey) {
        mutableStateOf(uiState.customApiKey)
    }
    var selectedModel by remember(uiState.selectedModel) {
        mutableStateOf(uiState.selectedModel)
    }
    var temperature by remember(uiState.temperature) {
        mutableFloatStateOf(uiState.temperature)
    }
    var topP by remember(uiState.topP) {
        mutableFloatStateOf(uiState.topP)
    }
    var topK by remember(uiState.topK) {
        mutableIntStateOf(uiState.topK)
    }
    var maxOutputTokens by remember(uiState.maxOutputTokens) {
        mutableIntStateOf(uiState.maxOutputTokens)
    }
    var selectedImageProvider by remember(uiState.imageProvider) {
        mutableStateOf(uiState.imageProvider)
    }
    var hfTokenText by remember(uiState.hfToken) {
        mutableStateOf(uiState.hfToken)
    }
    var showSavedNotice by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0717))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Layar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x337A4BCF),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Global Setting",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF4EEFF)
                    )
                    Text(
                        text = "Pengaturan API Key, Engine Gambar & Model AI",
                        fontSize = 11.sp,
                        color = Color(0xFFB388FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========================================================
            // SEKSI 1: API KEY GEMINI (ROLLING MULTI-KEY SUPPORT)
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x3328154D)),
                border = BorderStroke(1.dp, Color(0x44B388FF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini API Key (Bergilir)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFEDE7F6)
                            )
                        }

                        val parsedKeys = ApiKeyManager.parseKeys(apiKeyText)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (parsedKeys.isNotEmpty()) Color(0x3300E676) else Color(0x33FFB300)
                        ) {
                            Text(
                                text = if (parsedKeys.isNotEmpty()) "${parsedKeys.size} Kunci Siap" else "Menggunakan Default",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (parsedKeys.isNotEmpty()) Color(0xFF69F0AE) else Color(0xFFFFD54F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Masukkan satu atau banyak API Key Gemini (pisahkan baris baru atau koma). Aplikasi akan merotasi kunci secara bergantian di tiap pesan agar kuota token tidak cepat habis.",
                        fontSize = 11.sp,
                        color = Color(0xFFC7B9E5),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("global_api_key_input"),
                        placeholder = {
                            Text(
                                "AIzaSyA...\nAIzaSyB...\nAIzaSyC...",
                                fontSize = 12.sp,
                                color = Color(0x66C7B9E5)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFB388FF),
                            unfocusedBorderColor = Color(0x66B388FF),
                            focusedContainerColor = Color(0x22130728),
                            unfocusedContainerColor = Color(0x22130728),
                            focusedTextColor = Color(0xFFEDE7F6),
                            unfocusedTextColor = Color(0xFFEDE7F6)
                        )
                    )

                    if (apiKeyText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { apiKeyText = "" },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80))
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kosongkan Kunci", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========================================================
            // SEKSI 2: ENGINE PEMBUAT GAMBAR (IMAGE GENERATION)
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x3328154D)),
                border = BorderStroke(1.dp, Color(0x44B388FF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Engine Pembuat Gambar (Scene Art)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFEDE7F6)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pilih penyedia AI untuk membangkitkan ilustrasi adegan cerita secara instan saat membaca.",
                        fontSize = 11.sp,
                        color = Color(0xFFC7B9E5)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opsi Pollinations
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedImageProvider == ImageProvider.POLLINATIONS) Color(0x337A4BCF) else Color(0x11FFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (selectedImageProvider == ImageProvider.POLLINATIONS) Color(0xFFB388FF) else Color(0x22B388FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedImageProvider == ImageProvider.POLLINATIONS,
                                onClick = { selectedImageProvider = ImageProvider.POLLINATIONS },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD54F))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pollinations AI (Rekomendasi - Cepat & Gratis)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEDE7F6)
                                )
                                Text(
                                    text = "Tanpa perlu token tambahan. Gambar berkualitas tinggi dengan resolusi tajam.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC7B9E5)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opsi Hugging Face FLUX
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedImageProvider == ImageProvider.HUGGING_FACE) Color(0x337A4BCF) else Color(0x11FFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (selectedImageProvider == ImageProvider.HUGGING_FACE) Color(0xFFB388FF) else Color(0x22B388FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedImageProvider == ImageProvider.HUGGING_FACE,
                                    onClick = { selectedImageProvider = ImageProvider.HUGGING_FACE },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD54F))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Hugging Face (FLUX.1-schnell)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEDE7F6)
                                    )
                                    Text(
                                        text = "Memerlukan Hugging Face User Access Token (hf_...)",
                                        fontSize = 11.sp,
                                        color = Color(0xFFC7B9E5)
                                    )
                                }
                            }

                            if (selectedImageProvider == ImageProvider.HUGGING_FACE) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = hfTokenText,
                                    onValueChange = { hfTokenText = it },
                                    placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxxxx", fontSize = 12.sp) },
                                    label = { Text("Hugging Face Token") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFB388FF),
                                        unfocusedBorderColor = Color(0x66B388FF)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========================================================
            // SEKSI 3: MODEL AI & PARAMETER KREATIVITAS GLOBAL
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x3328154D)),
                border = BorderStroke(1.dp, Color(0x44B388FF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Model Gemini & Kreativitas Default",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFEDE7F6)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Pilih Model Default:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEDE7F6)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StoryPromptConfig.AVAILABLE_MODELS.forEach { model ->
                            val isSelected = (selectedModel == model)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedModel = model },
                                label = {
                                    Text(
                                        text = model.removePrefix("gemini-"),
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7A4BCF),
                                    selectedLabelColor = Color(0xFFFFD54F)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Temperature Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Suhu Generasi (Temperature):",
                            fontSize = 12.sp,
                            color = Color(0xFFEDE7F6)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", temperature),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFB388FF)
                        )
                    )

                    // Top-P Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Top-P Sampling:",
                            fontSize = 12.sp,
                            color = Color(0xFFEDE7F6)
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f", topP),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                    Slider(
                        value = topP,
                        onValueChange = { topP = it },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFB388FF)
                        )
                    )

                    // Max Tokens Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Batas Token Output (Max Tokens):",
                            fontSize = 12.sp,
                            color = Color(0xFFEDE7F6)
                        )
                        Text(
                            text = "$maxOutputTokens",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                    Slider(
                        value = maxOutputTokens.toFloat(),
                        onValueChange = { maxOutputTokens = it.toInt() },
                        valueRange = 512f..4096f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFB388FF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========================================================
            // SEKSI 4: SAFETY SETTINGS (INFORMASI HARDCODED BLOCK_NONE)
            // ========================================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x2200E676),
                border = BorderStroke(1.dp, Color(0x4400E676)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF69F0AE),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Safety Settings: BLOCK_NONE Aktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE8F5E9)
                        )
                        Text(
                            text = "Seluruh filter keamanan bawaan diatur ke bebas blokir (BLOCK_NONE) untuk fleksibilitas narasi fiksi, drama, dan romansa tanpa sensor palsu.",
                            fontSize = 11.sp,
                            color = Color(0xFFC8E6C9),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ========================================================
            // TOMBOL SIMPAN PENGATURAN GLOBAL
            // ========================================================
            Button(
                onClick = {
                    viewModel.saveGlobalSettings(
                        apiKey = apiKeyText,
                        model = selectedModel,
                        temperature = temperature,
                        topP = topP,
                        topK = topK,
                        maxTokens = maxOutputTokens,
                        imageProvider = selectedImageProvider,
                        hfToken = hfTokenText
                    )
                    showSavedNotice = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7A4BCF),
                    contentColor = Color(0xFFFFD54F)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_global_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan Pengaturan Global",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (showSavedNotice) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "✓ Pengaturan global berhasil disimpan untuk semua cerita!",
                    fontSize = 12.sp,
                    color = Color(0xFF69F0AE),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
